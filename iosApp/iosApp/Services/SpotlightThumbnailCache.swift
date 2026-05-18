import Foundation

/// On-disk cache delle thumbnail già scaricate, indicizzate per id socio.
/// Vive in `Caches/spotlight-member-thumbs/<id>.jpg` — directory `Caches`
/// così iOS può svuotarla in caso di pressione disco (e il prossimo refresh
/// ricostruisce dal server). I file sono già downscalati 540×540 q=0.7
/// (~15-30 KB l'uno).
enum SpotlightThumbnailCache {

    private static let folder: URL = {
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask).first!
        let url = base.appendingPathComponent("spotlight-member-thumbs", isDirectory: true)
        try? FileManager.default.createDirectory(at: url, withIntermediateDirectories: true)
        return url
    }()

    private static func fileURL(id: String) -> URL {
        folder.appendingPathComponent("\(id).jpg")
    }

    static func read(id: String) -> Data? {
        try? Data(contentsOf: fileURL(id: id))
    }

    static func write(id: String, data: Data) {
        try? data.write(to: fileURL(id: id), options: .atomic)
    }

    static func delete(id: String) {
        try? FileManager.default.removeItem(at: fileURL(id: id))
    }

    /// Garbage collect: rimuove i file per id non più presenti nel set fornito.
    /// Da chiamare alla fine di un full refresh dei soci.
    static func pruneNotIn(_ currentIds: Set<String>) {
        guard let entries = try? FileManager.default.contentsOfDirectory(
            at: folder, includingPropertiesForKeys: nil, options: [.skipsHiddenFiles]
        ) else { return }
        for url in entries where url.pathExtension == "jpg" {
            let id = url.deletingPathExtension().lastPathComponent
            if !currentIds.contains(id) {
                try? FileManager.default.removeItem(at: url)
            }
        }
    }

    static func clearAll() {
        try? FileManager.default.removeItem(at: folder)
        _ = folder // forza la ricreazione lazy
    }
}

// SpotlightHashStore was removed: per-member hashes now live in the SQLDelight
// RegSoci table (`dataHash`, `imageHash` columns). The KMP SpotlightSyncEngine
// reads the previous snapshot atomically during refreshAndDiff().
