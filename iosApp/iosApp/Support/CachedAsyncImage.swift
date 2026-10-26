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
        // A passport spread is twelve decoded stamps, ~0.4 MB each: at 50 MB
        // the pages just turned were already gone when turned back to.
        c.totalCostLimit = 100_000_000
        return c
    }()

    func image(for url: URL) -> UIImage? { cache.object(forKey: url as NSURL) }
    func store(_ image: UIImage, for url: URL) {
        let cost = Int(image.size.width * image.size.height * image.scale * image.scale * 4)
        cache.setObject(image, forKey: url as NSURL, cost: cost)
    }
}

/// The one way our images are fetched: memory, then disk, then network, a
/// file asked for twice at once fetched once. `CachedAsyncImage` and the
/// prefetchers both go through it, so what is prefetched is exactly what a
/// view then finds — the stamp prefetcher used to warm `URLCache.shared`, a
/// cache no view reads, and every page turned downloaded its stamps anew.
enum MensaImageLoader {
    private static let inflight = InflightImages()

    /// Memory only, synchronous: what a view can show on its first frame.
    static func memoryImage(for url: URL) -> UIImage? {
        MensaImageMemoryCache.shared.image(for: canonicalCacheKey(for: url))
    }

    /// The decoded image, from wherever it is; nil if it can't be had.
    static func image(for url: URL, priority: TaskPriority = .userInitiated) async -> UIImage? {
        let key = canonicalCacheKey(for: url)
        if let cached = MensaImageMemoryCache.shared.image(for: key) { return cached }
        let image = await inflight.image(for: key, priority: priority) {
            await fetch(url: url, key: key)
        }
        // Agganciata a un download di `warmDisk`, che salva su disco e non
        // decodifica: l'immagine adesso e' su disco, la si legge da li'.
        if image == nil, isCached(url) { return await fetch(url: url, key: key) }
        return image
    }

    /// Loads into memory and disk ahead of time, at low priority.
    static func prefetch(_ url: URL) {
        Task(priority: .utility) { _ = await image(for: url, priority: .utility) }
    }

    /// Scarica su disco, in sottofondo, quello che non c'e' ancora: una
    /// collezione intera, prima che qualcuno la sfogli. Tre alla volta, cosi'
    /// le immagini a schermo non finiscono in coda dietro a queste; niente
    /// decodifica e niente RAM, il disco basta perche' poi la vista le trovi
    /// in un frame. Chiamarla di nuovo con la stessa lista non riscarica
    /// niente.
    static func warmDisk(_ urls: [URL]) {
        Task(priority: .background) { await diskWarmer.enqueue(urls) }
    }

    private static let diskWarmer = DiskWarmer()

    /// Su disco o in memoria, senza decodificare.
    fileprivate static func isCached(_ url: URL) -> Bool {
        let key = canonicalCacheKey(for: url)
        if MensaImageMemoryCache.shared.image(for: key) != nil { return true }
        return imageURLCache.cachedResponse(for: URLRequest(url: key)) != nil
    }

    /// Solo rete → disco, per `DiskWarmer`.
    fileprivate static func download(_ url: URL) async {
        let key = canonicalCacheKey(for: url)
        _ = await inflight.image(for: key, priority: .background) {
            await fetch(url: url, key: key, decode: false)
        }
    }

    /// `decode: false` salva soltanto su disco e torna nil: e' il percorso
    /// di `warmDisk`, che non deve riempire la RAM con una collezione intera.
    private static func fetch(url: URL, key: URL, decode: Bool = true) async -> UIImage? {
        // 1. Disk, under the canonical key (the session has no automatic cache).
        let lookup = URLRequest(url: key)
        if let cached = imageURLCache.cachedResponse(for: lookup) {
            guard decode else { return nil }
            if let image = decoded(cached.data) {
                MensaImageMemoryCache.shared.store(image, for: key)
                return image
            }
        }

        // 2. Network. The real URL is only used to fetch; the response is
        // stored under the canonical key, so every URL naming the same
        // filename + thumb hits the same entry. With the Bearer the backend
        // answers 307 to a signed S3 link; the delegate drops the
        // Authorization when the redirect changes host. See `MensaAuth`.
        let request = MensaAuth.request(
            url: url,
            cachePolicy: .reloadIgnoringLocalCacheData,
            timeoutInterval: 30
        )
        do {
            let (data, response) = try await imageURLSession.data(
                for: request,
                delegate: MensaRedirectAuthStripper.shared
            )
            guard let http = response as? HTTPURLResponse else { return nil }
            guard http.statusCode == 200 else {
                Log.app.error("CachedAsyncImage non-200 \(http.statusCode) for \(url.absoluteString)")
                return nil
            }
            // Stored as if served from the canonical key, with an explicit
            // Cache-Control (PocketBase doesn't send one).
            var headers = http.allHeaderFields as? [String: String] ?? [:]
            headers["Cache-Control"] = "max-age=86400, public"
            if let rewritten = HTTPURLResponse(
                url: key,
                statusCode: 200,
                httpVersion: "HTTP/1.1",
                headerFields: headers
            ) {
                imageURLCache.storeCachedResponse(
                    CachedURLResponse(response: rewritten, data: data, userInfo: nil, storagePolicy: .allowed),
                    for: lookup
                )
            }
            guard decode, let image = decoded(data) else { return nil }
            MensaImageMemoryCache.shared.store(image, for: key)
            return image
        } catch {
            return nil
        }
    }

    /// Decoded here, off the main thread: `UIImage(data:)` alone decodes on
    /// first draw, which hitched a page curling in with six new stamps.
    private static func decoded(_ data: Data) -> UIImage? {
        guard let image = UIImage(data: data) else { return nil }
        return image.preparingForDisplay() ?? image
    }
}

/// La coda di `MensaImageLoader.warmDisk`: in ordine, al massimo tre
/// download insieme, e quello che e' gia' su disco saltato.
private actor DiskWarmer {
    private var queue: [URL] = []
    private var queued: Set<URL> = []
    private var running = 0
    private let maxConcurrent = 3

    func enqueue(_ urls: [URL]) {
        for url in urls where !queued.contains(url) {
            queued.insert(url)
            queue.append(url)
        }
        pump()
    }

    private func pump() {
        while running < maxConcurrent, !queue.isEmpty {
            let url = queue.removeFirst()
            running += 1
            Task.detached(priority: .background) {
                if !MensaImageLoader.isCached(url) {
                    await MensaImageLoader.download(url)
                }
                await self.finished(url)
            }
        }
    }

    private func finished(_ url: URL) {
        running -= 1
        queued.remove(url)
        pump()
    }
}

/// Requests in flight, by canonical key, so a view and the prefetcher asking
/// for the same file share one download.
private actor InflightImages {
    private var tasks: [URL: Task<UIImage?, Never>] = [:]

    func image(
        for key: URL,
        priority: TaskPriority,
        load: @escaping @Sendable () async -> UIImage?
    ) async -> UIImage? {
        if let task = tasks[key] { return await task.value }
        let task = Task.detached(priority: priority) { await load() }
        tasks[key] = task
        let image = await task.value
        tasks[key] = nil
        return image
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
    /// Dimensione in pixel dell'immagine decodificata, appena si conosce.
    ///
    /// La closure `content` riceve una `SwiftUI.Image`, che non espone `.size`:
    /// senza questo aggancio il chiamante non ha nessun modo di sapere quanto
    /// e' larga e alta davvero la copertina. Serve alle card che si
    /// dimensionano sul rapporto dell'immagine (vedi `CoverRatio`).
    /// Opzionale con default nil: i 35 punti che gia' usano questa view non
    /// cambiano di una riga.
    let onLoad: ((CGSize) -> Void)?

    @State private var loaded: UIImage?
    /// Sale quando la connessione torna e l'immagine non era arrivata: fa
    /// ripartire `load()` (vedi `ReconnectSignal`).
    @State private var retry = 0

    init(
        url: URL?,
        onLoad: ((CGSize) -> Void)? = nil,
        @ViewBuilder content: @escaping (Image) -> Content,
        @ViewBuilder placeholder: @escaping () -> Placeholder
    ) {
        self.url = url
        self.onLoad = onLoad
        self.content = content
        self.placeholder = placeholder
        // Seed state synchronously from the memory cache so recycled List/LazyVStack
        // cells render the image on the first frame — no placeholder flash on scroll.
        // Chiave canonica → due URL diversi che puntano allo stesso file
        // PocketBase condividono la stessa entry RAM.
        let seeded = url.flatMap { MensaImageLoader.memoryImage(for: $0) }
        self._loaded = State(initialValue: seeded)
        if let seeded { onLoad?(seeded.size) }
    }

    var body: some View {
        // ZStack + conditional content so SwiftUI vede l'immagine come una
        // view che "entra" la prima volta che `loaded` diventa non-nil. La
        // `.transition(.opacity)` viene attivata solo quando l'assegnazione
        // di `loaded` è fatta dentro un `withAnimation` (vedi `load()`); per
        // il path "seed da memory cache" l'immagine è già presente al primo
        // frame → niente animazione, niente flash su scroll di celle riciclate.
        ZStack {
            // Il placeholder esce di scena quando l'immagine c'e'. Prima
            // restava sotto, sempre: con un'altezza fissa piu' grande
            // dell'immagine, il suo gradiente spuntava sopra e sotto — ed era
            // esattamente lo "spazio vuoto" delle copertine dei SIG, che sono
            // banner larghe piu' del doppio di quanto sono alte.
            if loaded == nil {
                placeholder()
            } else if let img = loaded {
                content(Image(uiImage: img))
                    .transition(.opacity)
            }
        }
        .task(id: LoadKey(url: url, retry: retry)) { await load() }
        .onChange(of: ReconnectSignal.shared.count) { _, _ in
            if loaded == nil { retry += 1 }
        }
    }

    private struct LoadKey: Equatable {
        let url: URL?
        let retry: Int
    }

    private func load() async {
        guard let url else { return }
        if let cached = MensaImageLoader.memoryImage(for: url) {
            self.loaded = cached
            onLoad?(cached.size)
            return
        }
        let started = Date()
        guard let image = await MensaImageLoader.image(for: url) else { return }
        // Fade in only what really came from far (the network): a disk hit
        // is there in a frame, and fading it looked like a fresh download.
        if Date().timeIntervalSince(started) > 0.15 {
            withAnimation(.easeOut(duration: 0.25)) { self.loaded = image }
        } else {
            self.loaded = image
        }
        onLoad?(image.size)
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
