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

    /// Scalda solo la pagina attorno a `page`, non tutta la collezione.
    ///
    /// `warmAll` accodava una richiesta per ogni timbro. La sessione tiene 4
    /// connessioni per host, quindi le immagini della pagina che l'utente sta
    /// *guardando* finivano in fila dietro decine di prefetch di pagine mai
    /// aperte: e' esattamente il caricamento in ritardo che si vede a schermo.
    /// Una pagina avanti e una indietro bastano, il libro si sfoglia una
    /// pagina per volta.
    static func warmAround(_ stamps: [StampUserModel], page: Int, perPage: Int) {
        guard !stamps.isEmpty, perPage > 0 else { return }
        let from = max(0, (page - 1) * perPage)
        let to = min(stamps.count, (page + 2) * perPage)
        guard from < to else { return }
        warm(Array(stamps[from..<to]))
    }

    private static func warm(_ stamps: [StampUserModel]) {
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
        return Files.url(collection: "stamp", recordId: r.id, filename: r.image, thumb: PassportLayout.gridThumb)
    }
}
