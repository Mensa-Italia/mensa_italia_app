import SwiftUI
import Foundation
import UIKit

/// Cache di sessione dedicata per le immagini.
///
/// Storia: prima usavamo `URLCache.shared = customCache` + `URLSession.shared`.
/// Il problema è che `URLSession.shared` può aggrapparsi alla `URLCache` che
/// trovava al primo accesso — sostituirla globalmente DOPO non garantisce che
/// `URLSession.shared.data(for:)` la consulti, e in pratica le immagini
/// venivano ri-scaricate ad ogni cold launch (memory cache vuota + miss su
/// URLCache).
///
/// Adesso usiamo una `URLSession` privata con `URLCache` legata esplicitamente,
/// directory `<App>/Library/Caches/MensaImages/`. Tutto il path I/O è quindi
/// vincolato a una cache che riconosciamo, persiste tra cold launch, e che
/// possiamo anche misurare via `URLCache.currentDiskUsage` se serve.
enum CachedImageCacheConfig {
    /// Backwards-compat: oggi è un no-op, la cache è inizializzata pigramente
    /// al primo accesso a `imageURLSession`. Lasciato come hook esplicito nel
    /// boot dell'app per scoraggiare un futuro `URLCache.shared = ...` che
    /// rompa di nuovo questa configurazione.
    static func configureShared() {
        _ = imageURLSession
    }
}

private let imageURLCache: URLCache = {
    let cachesDir = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
    let dir = cachesDir.appendingPathComponent("MensaImages", isDirectory: true)
    try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
    return URLCache(
        memoryCapacity: 100_000_000,   // 100 MB RAM
        diskCapacity: 500_000_000,     // 500 MB disco
        directory: dir
    )
}()

private let imageURLSession: URLSession = {
    let config = URLSessionConfiguration.default
    // La cache su disco la gestiamo MANUALMENTE su `imageURLCache` con una
    // chiave canonica (`mensa-img:///<filename>?thumb=<size>`) — vedi
    // `canonicalCacheKey(for:)`. Lasciare anche la `URLCache` automatica della
    // sessione produrrebbe doppia scrittura su disco e ridurrebbe il TTR del
    // hit perché la lookup automatica della sessione è keyata sull'URL reale.
    config.urlCache = nil
    config.requestCachePolicy = .reloadIgnoringLocalCacheData
    config.waitsForConnectivity = false
    return URLSession(configuration: config)
}()

/// Chiave di cache "canonica" per gli URL file PocketBase.
///
/// PocketBase produce filename auto-hashati (`<base>_<8char>.<ext>`), quindi
/// globalmente unici a prescindere dal `recordId` del path. Collassando l'URL
/// reale `/api/files/<collection>/<recordId>/<filename>?thumb=<size>` su un
/// URI sintetico `mensa-img:///<filename>?thumb=<size>` (o `thumb=0` se assente)
/// otteniamo:
///   - più path UI che puntano allo stesso file (es. lista soci con
///     `member.id`, OrgChart con `userId`, dettaglio con un altro ancora)
///     condividono UNA sola entry su disco/RAM.
///   - cache hit anche quando il thumb richiesto è "sbagliato" e finisce con
///     un fallback all'originale — finché il filename combacia.
/// Per URL fuori da `/api/files/...` (es. asset CDN esterni) torniamo
/// all'URL completo come chiave per evitare collisioni cross-origin.
private func canonicalCacheKey(for url: URL) -> URL {
    guard url.path.hasPrefix("/api/files/") else { return url }
    let filename = url.lastPathComponent
    let thumb = URLComponents(url: url, resolvingAgainstBaseURL: false)?
        .queryItems?
        .first(where: { $0.name == "thumb" })?
        .value ?? "0"
    let escaped = filename.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? filename
    return URL(string: "mensa-img:///\(escaped)?thumb=\(thumb)") ?? url
}

/// In-memory cache of fully decoded UIImage instances. Keyed by absolute URL.
final class MensaImageMemoryCache: @unchecked Sendable {
    static let shared = MensaImageMemoryCache()
    private let cache: NSCache<NSURL, UIImage> = {
        let c = NSCache<NSURL, UIImage>()
        c.countLimit = 500
        c.totalCostLimit = 50_000_000
        return c
    }()

    func image(for url: URL) -> UIImage? { cache.object(forKey: url as NSURL) }
    func store(_ image: UIImage, for url: URL) {
        let cost = Int(image.size.width * image.size.height * image.scale * image.scale * 4)
        cache.setObject(image, forKey: url as NSURL, cost: cost)
    }
}

/// Drop-in replacement for `AsyncImage` that:
/// - Looks up the in-memory `NSCache` first (instant render),
/// - Falls back to `URLCache.shared` so the image is available offline,
/// - Issues a network request only as a last resort (with `returnCacheDataElseLoad`),
/// - Aggressively rewrites the upstream response's `Cache-Control` header so
///   PocketBase-served images (which often ship `Cache-Control: no-store`) are
///   actually persisted to the disk cache for the next launch.
struct CachedAsyncImage<Content: View, Placeholder: View>: View {
    let url: URL?
    let content: (Image) -> Content
    let placeholder: () -> Placeholder

    @State private var loaded: UIImage?

    init(
        url: URL?,
        @ViewBuilder content: @escaping (Image) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder
    ) {
        self.url = url
        self.content = content
        self.placeholder = placeholder
        // Seed state synchronously from the memory cache so recycled List/LazyVStack
        // cells render the image on the first frame — no placeholder flash on scroll.
        // Chiave canonica → due URL diversi che puntano allo stesso file
        // PocketBase condividono la stessa entry RAM.
        self._loaded = State(initialValue: url.flatMap {
            MensaImageMemoryCache.shared.image(for: canonicalCacheKey(for: $0))
        })
    }

    var body: some View {
        // ZStack + conditional content so SwiftUI vede l'immagine come una
        // view che "entra" la prima volta che `loaded` diventa non-nil. La
        // `.transition(.opacity)` viene attivata solo quando l'assegnazione
        // di `loaded` è fatta dentro un `withAnimation` (vedi `load()`); per
        // il path "seed da memory cache" l'immagine è già presente al primo
        // frame → niente animazione, niente flash su scroll di celle riciclate.
        ZStack {
            placeholder()
            if let img = loaded {
                content(Image(uiImage: img))
                    .transition(.opacity)
            }
        }
        .task(id: url) { await load() }
    }

    private func load() async {
        guard let url else { return }
        let cacheKey = canonicalCacheKey(for: url)

        // 1. Memory cache (RAM) sulla chiave canonica.
        if let cached = MensaImageMemoryCache.shared.image(for: cacheKey) {
            self.loaded = cached
            return
        }

        // 2. URLCache disco sulla chiave canonica (manual lookup — la
        // sessione non ha più la sua urlCache automatica).
        let cacheLookupRequest = URLRequest(url: cacheKey)
        if let cached = imageURLCache.cachedResponse(for: cacheLookupRequest),
           let img = UIImage(data: cached.data) {
            MensaImageMemoryCache.shared.store(img, for: cacheKey)
            withAnimation(.easeOut(duration: 0.25)) {
                self.loaded = img
            }
            return
        }

        // 3. Network — l'URL reale viene usato SOLO per la fetch; la
        // CachedURLResponse risultante viene scritta sotto la chiave canonica
        // così tutti i futuri lookup (con qualunque URL reale che condivide
        // filename+thumb) hittano la stessa entry.
        let fetchRequest = URLRequest(
            url: url,
            cachePolicy: .reloadIgnoringLocalCacheData,
            timeoutInterval: 30
        )
        do {
            let (data, response) = try await imageURLSession.data(for: fetchRequest)
            guard let http = response as? HTTPURLResponse else { return }
            if http.statusCode != 200 {
                Log.app.error("CachedAsyncImage non-200 \(http.statusCode) for \(url.absoluteString)")
                return
            }

            // Reinterpreta la response come se fosse arrivata dalla chiave
            // canonica, con Cache-Control esplicito (PocketBase non lo emette).
            var headers = http.allHeaderFields as? [String: String] ?? [:]
            headers["Cache-Control"] = "max-age=86400, public"
            if let rewritten = HTTPURLResponse(
                url: cacheKey,
                statusCode: 200,
                httpVersion: "HTTP/1.1",
                headerFields: headers
            ) {
                let stored = CachedURLResponse(
                    response: rewritten,
                    data: data,
                    userInfo: nil,
                    storagePolicy: .allowed
                )
                imageURLCache.storeCachedResponse(stored, for: cacheLookupRequest)
            }

            if let img = UIImage(data: data) {
                MensaImageMemoryCache.shared.store(img, for: cacheKey)
                // `withAnimation` qui abilita la `.transition(.opacity)` →
                // fade-in 250ms quando l'immagine arriva da rete.
                withAnimation(.easeOut(duration: 0.25)) {
                    self.loaded = img
                }
            }
        } catch {
            // Swallow — placeholder stays visible.
        }
    }
}

extension CachedAsyncImage where Placeholder == Color {
    init(
        url: URL?,
        @ViewBuilder content: @escaping (Image) -> Content
    ) {
        self.init(url: url, content: content, placeholder: { Color.gray.opacity(0.15) })
    }
}
