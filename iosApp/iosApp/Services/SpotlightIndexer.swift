import Foundation
import CoreSpotlight
import UniformTypeIdentifiers
import UIKit
import ImageIO
import Shared

/// Spotlight glue for **documents** + the deep-link reverse-lookup used by
/// `iosAppApp.onContinueUserActivity`.
///
/// **Members are no longer indexed here.** The end-to-end member sync
/// (server fetch + hash diff + image download + batched index) lives in
/// KMP — see `SpotlightSyncEngine` + `SpotlightSink`. iOS implements the
/// sink in `SpotlightSinkImpl.swift` (resize + CSSearchableItem + index).
///
/// Identifier convention preserved across both paths:
///   - `member:<id>`   → MemberDetailView(memberId:)
///   - `document:<id>` → DocumentDetailView(documentId:)
enum SpotlightIndexer {

    static let memberDomain = "it.mensa.app.spotlight.members"
    static let documentDomain = "it.mensa.app.spotlight.documents"

    static func memberIdentifier(_ id: String) -> String { "member:\(id)" }
    static func documentIdentifier(_ id: String) -> String { "document:\(id)" }

    static func target(forIdentifier identifier: String) -> NotificationTarget? {
        if let id = identifier.stripPrefix("member:") { return .member(id) }
        if let id = identifier.stripPrefix("document:") { return .singleDocument(id) }
        return nil
    }

    // MARK: - Tuning (documents only)

    private static let docIndexBatchSize = 200
    private static let docIndexBatchConcurrency = 6
    private static let docIconPixelSize: CGFloat = 96

    // MARK: - Documents

    /// Indicizza tutti i documenti con thumbnail = icona categoria.
    /// Le icone vengono pre-renderizzate UPFRONT (sequenzialmente) e poi
    /// la mappa viene letta read-only durante i batch paralleli, eliminando
    /// race condition / rendering duplicati.
    static func indexDocuments(
        _ documents: [DocumentModel],
        expiration: Date,
        onProgress: @escaping @MainActor (Int, Int) -> Void
    ) async {
        let total = documents.count
        guard total > 0 else {
            await MainActor.run { onProgress(0, 0) }
            return
        }

        // Pre-render icone categoria (sequenziale, prima dei batch paralleli).
        let categories = Set(documents.map { $0.category })
        var iconCache: [String: Data] = [:]
        iconCache.reserveCapacity(categories.count + 1)
        for cat in categories {
            if let data = renderCategoryIcon(category: cat) {
                iconCache[cat] = data
            }
        }
        let defaultIcon = renderCategoryIcon(category: "")
        let frozenCache = iconCache
        let defaultData = defaultIcon

        // Costruisci tutti gli item (cheap, in memoria).
        let items: [CSSearchableItem] = documents.map { d in
            let attr = CSSearchableItemAttributeSet(contentType: UTType.content)
            attr.title = d.name
            let desc = d.description_ ?? ""
            attr.contentDescription = desc.isEmpty ? d.category : desc
            attr.keywords = [d.name, d.category, desc].filter { !$0.isEmpty }
            if let data = frozenCache[d.category] ?? defaultData {
                attr.thumbnailData = data
            }
            let item = CSSearchableItem(
                uniqueIdentifier: documentIdentifier(d.id),
                domainIdentifier: documentDomain,
                attributeSet: attr
            )
            item.expirationDate = expiration
            return item
        }

        let throttler = ProgressThrottler(total: total, callback: onProgress)
        await throttler.start()
        let batches = items.chunked(by: docIndexBatchSize)
        let index = CSSearchableIndex.default()

        await withTaskGroup(of: Int.self) { group in
            var iter = batches.makeIterator()
            var active = 0

            func spawnNext() -> Bool {
                guard let batch = iter.next() else { return false }
                group.addTask {
                    await indexBatchAsync(batch: batch, index: index)
                    return batch.count
                }
                return true
            }

            for _ in 0..<docIndexBatchConcurrency {
                if spawnNext() { active += 1 } else { break }
            }

            while active > 0 {
                if let done = await group.next() {
                    active -= 1
                    await throttler.add(done)
                    if spawnNext() { active += 1 }
                }
            }
        }

        await throttler.flush()
    }

    // MARK: - Index batch

    private static func indexBatchAsync(batch: [CSSearchableItem], index: CSSearchableIndex) async {
        guard !batch.isEmpty else { return }
        await withCheckedContinuation { (cont: CheckedContinuation<Void, Never>) in
            index.indexSearchableItems(batch) { _ in
                cont.resume()
            }
        }
    }

    // MARK: - Category icons

    /// Mappa categorie italiane → (SF Symbol, tint). Renderizzata a 96×96
    /// PNG (~1KB) e cached pre-batch.
    private static let categoryStyle: [String: (symbol: String, tint: UIColor)] = [
        "statuto":       ("building.columns.fill", .systemIndigo),
        "regolamento":   ("text.book.closed.fill", .systemBlue),
        "verbale":       ("doc.text.fill",         .systemTeal),
        "circolare":     ("megaphone.fill",        .systemOrange),
        "modulo":        ("square.and.pencil",     .systemGreen),
        "manuale":       ("book.fill",             .systemPurple),
        "presentazione": ("rectangle.on.rectangle", .systemPink),
        "bilancio":      ("chart.pie.fill",        .systemYellow),
        "contratto":     ("signature",             .systemBrown),
        "privacy":       ("lock.shield.fill",      .systemRed)
    ]

    private static func renderCategoryIcon(category: String) -> Data? {
        let key = category.lowercased()
        let style = categoryStyle[key] ?? ("doc.fill", .systemGray)

        let size = CGSize(width: docIconPixelSize, height: docIconPixelSize)
        let renderer = UIGraphicsImageRenderer(size: size, format: {
            let f = UIGraphicsImageRendererFormat()
            f.scale = 1
            f.opaque = true
            return f
        }())
        let img = renderer.image { ctx in
            let rect = CGRect(origin: .zero, size: size)
            style.tint.setFill()
            ctx.fill(rect)

            let cfg = UIImage.SymbolConfiguration(pointSize: docIconPixelSize * 0.55, weight: .semibold)
            guard let sym = UIImage(systemName: style.symbol, withConfiguration: cfg)?
                .withTintColor(.white, renderingMode: .alwaysOriginal) else { return }

            let symSize = sym.size
            let target = CGSize(
                width: docIconPixelSize * 0.55,
                height: docIconPixelSize * 0.55 * (symSize.height / max(symSize.width, 1))
            )
            let origin = CGPoint(
                x: (size.width - target.width) / 2,
                y: (size.height - target.height) / 2
            )
            sym.draw(in: CGRect(origin: origin, size: target))
        }
        return img.pngData()
    }
}

// MARK: - Progress throttler

/// Throttla i callback di progresso a ~5/sec per non far render-loop SwiftUI.
/// Accumula i delta e li flusha quando passa l'intervallo o a fine batch.
///
/// I callback vengono invocati con `await MainActor.run` (singolo hop, NON
/// fire-and-forget) così quando `flush()` ritorna il chiamante ha la garanzia
/// che l'ultima emissione è già stata processata dal main actor — evita la
/// race per cui un `phase = .idle` settato subito dopo veniva sovrascritto
/// dal callback in coda.
private actor ProgressThrottler {
    private let total: Int
    private let callback: @MainActor (Int, Int) -> Void
    private let minInterval: TimeInterval = 0.2
    private var done: Int = 0
    private var lastEmit: Date = .distantPast

    init(total: Int, callback: @escaping @MainActor (Int, Int) -> Void) {
        self.total = total
        self.callback = callback
    }

    func start() async {
        let t = total
        let cb = callback
        await MainActor.run { cb(0, t) }
    }

    func add(_ delta: Int) async {
        done += delta
        let now = Date()
        guard now.timeIntervalSince(lastEmit) >= minInterval || done >= total else { return }
        lastEmit = now
        let snapshot = done
        let t = total
        let cb = callback
        await MainActor.run { cb(snapshot, t) }
    }

    func flush() async {
        let snapshot = done
        let t = total
        let cb = callback
        await MainActor.run { cb(snapshot, t) }
    }
}

// MARK: - Tiny helpers

private extension String {
    func stripPrefix(_ prefix: String) -> String? {
        guard hasPrefix(prefix) else { return nil }
        return String(dropFirst(prefix.count))
    }
}

private extension Array {
    func chunked(by size: Int) -> [[Element]] {
        guard size > 0 else { return [self] }
        var out: [[Element]] = []
        out.reserveCapacity((count + size - 1) / size)
        var i = 0
        while i < count {
            out.append(Array(self[i..<Swift.min(i + size, count)]))
            i += size
        }
        return out
    }
}
