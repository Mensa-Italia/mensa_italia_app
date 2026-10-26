import Foundation
import Shared

/// Loads the stamps of the pages around the open one ahead of time, so a
/// page turned to already has them (in memory, decoded) and one turned back
/// to still does.
///
/// Through `MensaImageLoader`, the same path the stamps on screen take: it
/// used to warm `URLCache.shared`, which no view reads, so nothing prefetched
/// was ever found.
enum StampImagePrefetcher {
    /// Scalda solo la pagina attorno a `page`, non tutta la collezione.
    ///
    /// `warmAll` accodava una richiesta per ogni timbro. Le connessioni per
    /// host sono poche, quindi le immagini della pagina che l'utente sta
    /// *guardando* finivano in fila dietro decine di prefetch di pagine mai
    /// aperte: e' esattamente il caricamento in ritardo che si vede a schermo.
    /// Una pagina avanti e una indietro bastano, il libro si sfoglia una
    /// pagina per volta.
    ///
    /// `ahead`: pages warmed past the open one. A continuous scroll covers
    /// several pages in one fling, so it asks for more than a book does.
    static func warmAround(_ stamps: [StampUserModel], page: Int, perPage: Int, ahead: Int = 1) {
        guard !stamps.isEmpty, perPage > 0 else { return }
        let from = max(0, (page - 1) * perPage)
        let to = min(stamps.count, (page + 1 + ahead) * perPage)
        guard from < to else { return }
        warm(Array(stamps[from..<to]))
    }

    /// Tutta la collezione su disco, in sottofondo e tre alla volta (vedi
    /// `MensaImageLoader.warmDisk`): sfogliando, il timbro nuovo arriva dal
    /// disco in un frame invece che dalla rete. Parte dalla Home, prima che
    /// il passaporto si apra, e di nuovo all'apertura per i timbri appena
    /// arrivati; quello che e' gia' su disco non si riscarica.
    static func warmCollection(_ stamps: [StampUserModel]) {
        MensaImageLoader.warmDisk(stamps.compactMap(url(for:)))
    }

    private static func warm(_ stamps: [StampUserModel]) {
        for stamp in stamps {
            if let url = url(for: stamp) { MensaImageLoader.prefetch(url) }
        }
    }

    private static func url(for stamp: StampUserModel) -> URL? {
        guard let r = stamp.stampRecord, !r.image.isEmpty else { return nil }
        return Files.url(collection: "stamp", recordId: r.id, filename: r.image, thumb: PassportLayout.gridThumb)
    }
}
