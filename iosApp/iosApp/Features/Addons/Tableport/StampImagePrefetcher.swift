import Foundation
import Shared

/// Warms `URLCache.shared` with stamp PNGs so the next page's images are
/// already on disk by the time the user flips to it.
///
/// Strategy: fire one URLSession data task per image, low priority,
/// rate-limited so we don't saturate the network. Each request reuses
/// `URLCache.shared` (configured at app boot in `CachedImageCacheConfig`).
enum StampImagePrefetcher {
    private static let queue = DispatchQueue(label: "mensa.stamp.prefetch", qos: .utility)
    private static var inflight: Set<String> = []
    private static let session: URLSession = {
        let cfg = URLSessionConfiguration.default
        cfg.requestCachePolicy = .returnCacheDataElseLoad
        cfg.urlCache = URLCache.shared
        cfg.httpMaximumConnectionsPerHost = 4
        return URLSession(configuration: cfg)
    }()

    /// Warm every stamp image we know about. Cheap because already-cached
    /// requests short-circuit at the URLCache layer.
    static func warmAll(_ stamps: [StampUserModel]) {
        queue.async {
            for s in stamps {
                guard let url = url(for: s) else { continue }
                let key = url.absoluteString
                if inflight.contains(key) { continue }
                inflight.insert(key)
                var req = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad)
                req.timeoutInterval = 20
                let task = session.dataTask(with: req) { _, _, _ in
                    queue.async { inflight.remove(key) }
                }
                task.priority = URLSessionTask.lowPriority
                task.resume()
            }
        }
    }

    private static func url(for stamp: StampUserModel) -> URL? {
        guard let r = stamp.stampRecord, !r.image.isEmpty else { return nil }
        return Files.url(collection: "stamp", recordId: r.id, filename: r.image, thumb: "600x400")
    }
}
