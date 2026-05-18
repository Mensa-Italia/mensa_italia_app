import Foundation
import CoreSpotlight
import CoreServices
import UniformTypeIdentifiers
import ImageIO
import Shared

/// Thin iOS-side implementation of the KMP `SpotlightSink` interface.
///
/// All diff/download/hash logic lives in KMP (`SpotlightSyncEngine`). This
/// class only:
///   1. translates a [SpotlightMemberBlock] into a [CSSearchableItem];
///   2. when the block carries fresh `imageBytes`, resizes/encodes the
///      thumbnail and caches the result on disk;
///   3. when the block has `reuseImage = true`, looks up the cached resized
///      thumbnail and re-attaches it (cheap re-index without re-download);
///   4. pushes batches to `CSSearchableIndex.default()` and handles bulk
///      deletes / clears.
///
/// When we ship an Android port the equivalent class will live in androidApp.
final class SpotlightSinkImpl: NSObject, SpotlightSink {

    static let shared = SpotlightSinkImpl()

    // Match the namespaces SpotlightIndexer has always used; docs phase
    // still lives in Swift and writes to documentDomain.
    private let memberDomain = "it.mensa.app.spotlight.members"
    private let documentDomain = "it.mensa.app.spotlight.documents"
    private let thumbnailPixelSize: Int = 540
    private let thumbnailJPEGQuality: CGFloat = 0.7

    // MARK: - SpotlightSink

    func indexMembers(batch: [SpotlightMemberBlock]) async throws {
        guard !batch.isEmpty else { return }
        let items: [CSSearchableItem] = batch.compactMap { build($0) }
        guard !items.isEmpty else { return }
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            CSSearchableIndex.default().indexSearchableItems(items) { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }
    }

    func deleteMembers(ids: [String]) async throws {
        guard !ids.isEmpty else { return }
        let qualified = ids.map { "member:\($0)" }
        // Drop the thumbnail cache entries too so disk doesn't grow forever.
        for id in ids { SpotlightThumbnailCache.delete(id: id) }
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            CSSearchableIndex.default().deleteSearchableItems(withIdentifiers: qualified) { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }
    }

    func clearAll() async throws {
        // Match the previous SpotlightIndexer.clearAll behaviour: wipe both
        // domains + the on-disk thumbnail cache.
        try await withCheckedThrowingContinuation { (cont: CheckedContinuation<Void, Error>) in
            CSSearchableIndex.default().deleteSearchableItems(
                withDomainIdentifiers: [memberDomain, documentDomain]
            ) { error in
                if let error { cont.resume(throwing: error) } else { cont.resume() }
            }
        }
        SpotlightThumbnailCache.clearAll()
    }

    // MARK: - Block → CSSearchableItem

    private func build(_ block: SpotlightMemberBlock) -> CSSearchableItem? {
        let attr = CSSearchableItemAttributeSet(contentType: UTType.contact)
        attr.title = block.name

        let subtitle: String
        if !block.city.isEmpty, !block.state.isEmpty {
            subtitle = "\(block.city) · \(block.state)"
        } else if !block.city.isEmpty {
            subtitle = block.city
        } else {
            subtitle = block.state
        }
        attr.contentDescription = subtitle
        attr.keywords = block.nameKeywords
        if !block.emails.isEmpty { attr.emailAddresses = block.emails }
        if !block.phones.isEmpty { attr.phoneNumbers = block.phones }

        // Thumbnail policy:
        //  - imageBytes != nil  → fresh download from KMP. Resize + cache + attach.
        //  - imageBytes == nil + reuseImage → cache lookup. If miss (cache evicted
        //    by iOS under disk pressure), index without thumb; next full refresh
        //    will pick it up via the image-hash diff (we DON'T persist the hash
        //    here so an evicted thumb naturally re-downloads).
        //  - imageBytes == nil + !reuseImage → no image (empty/legacy avatar).
        if let bytes = block.imageBytes {
            let data = bytes.toSwiftData()
            if let resized = resizeToThumbnailData(source: data) {
                SpotlightThumbnailCache.write(id: block.id, data: resized)
                attr.thumbnailData = resized
            }
        } else if block.reuseImage, let cached = SpotlightThumbnailCache.read(id: block.id) {
            attr.thumbnailData = cached
        }

        let item = CSSearchableItem(
            uniqueIdentifier: "member:\(block.id)",
            domainIdentifier: memberDomain,
            attributeSet: attr
        )
        item.expirationDate = Date(
            timeIntervalSince1970: TimeInterval(block.expirationEpochSeconds)
        )
        return item
    }

    // MARK: - Image resize (copied from SpotlightIndexer)

    /// Decodifica + downscale via `CGImageSourceCreateThumbnailAtIndex` con
    /// `kCGImageSourceShouldCacheImmediately: false` — evita di allocare il
    /// bitmap full-size prima del resize (critico per non triggerare jetsam
    /// su JPEG con dimensioni gonfie nei metadata).
    private func resizeToThumbnailData(source: Data) -> Data? {
        let cfData = source as CFData
        guard let imageSource = CGImageSourceCreateWithData(cfData, [
            kCGImageSourceShouldCache: false
        ] as CFDictionary) else { return nil }

        let opts: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceShouldCacheImmediately: false,
            kCGImageSourceThumbnailMaxPixelSize: thumbnailPixelSize,
        ]
        guard let thumb = CGImageSourceCreateThumbnailAtIndex(imageSource, 0, opts as CFDictionary) else {
            return nil
        }

        let outData = NSMutableData()
        guard let dest = CGImageDestinationCreateWithData(
            outData, UTType.jpeg.identifier as CFString, 1, nil
        ) else { return nil }
        let jpegOpts: [CFString: Any] = [kCGImageDestinationLossyCompressionQuality: thumbnailJPEGQuality]
        CGImageDestinationAddImage(dest, thumb, jpegOpts as CFDictionary)
        guard CGImageDestinationFinalize(dest) else { return nil }
        return outData as Data
    }
}

// MARK: - KotlinByteArray bridge

private extension KotlinByteArray {
    /// O(n) copy: KotlinByteArray storage isn't directly addressable from
    /// Swift, so we read element-by-element. For 30KB thumbnails this is a
    /// fraction of a millisecond — negligible vs the JPEG decode/resize.
    func toSwiftData() -> Data {
        let n = Int(size)
        guard n > 0 else { return Data() }
        var data = Data(count: n)
        data.withUnsafeMutableBytes { (ptr: UnsafeMutableRawBufferPointer) in
            for i in 0..<n {
                ptr[i] = UInt8(bitPattern: get(index: Int32(i)))
            }
        }
        return data
    }
}
