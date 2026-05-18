import Foundation

/// PocketBase file URL builder.
enum Files {
    static func url(
        collection: String,
        recordId: String,
        filename: String,
        thumb: String? = nil
    ) -> URL? {
        guard !filename.isEmpty else { return nil }
        var s = "https://svc.mensa.it/api/files/\(collection)/\(recordId)/\(filename)"
        if let t = thumb { s += "?thumb=\(t)" }
        return URL(string: s)
    }
}
