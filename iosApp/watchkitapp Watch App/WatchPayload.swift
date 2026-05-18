import Foundation

/// Snapshot dei dati che l'iOS app scrive nell'App Group condiviso
/// (`group.it.mensa.app`) e che Watch app + Widget Extension consumano.
///
/// La logica di business resta nel modulo KMP `shared`: l'iOS app osserva
/// `auth.currentUser` e `events.observeAll()`, deriva questo snapshot, lo
/// serializza e lo persiste. Watch e Widget non parlano con il backend.
///
/// TODO: quando vorremo un Watch autonomo (richieste API dirette), sostituire
/// questa struttura con una `KeychainSettings(accessGroup:)` per condividere
/// il token + `import Shared` per usare repository/Flow direttamente.
struct WatchPayload: Codable, Equatable {
    var card: CardSnapshot?
    var nextEvent: EventSnapshot?
    var generatedAt: Date

    struct CardSnapshot: Codable, Equatable {
        var memberId: String
        var fullName: String
        var expiryFormatted: String
        var isActive: Bool
        /// PNG del QR generato lato iOS (CoreImage non risolve come modulo su
        /// watchOS in questa toolchain, quindi il QR arriva pre-renderizzato).
        var qrPng: Data?
    }

    struct EventSnapshot: Codable, Equatable {
        var id: String
        var name: String
        var startDate: Date
        var endDate: Date?
        var locationName: String?
        var isNational: Bool
    }
}

enum WatchAppGroup {
    static let identifier = "group.it.mensa.app"
    static let payloadKey = "watch_payload_v1"

    static var defaults: UserDefaults? {
        UserDefaults(suiteName: identifier)
    }

    static func read() -> WatchPayload? {
        guard let data = defaults?.data(forKey: payloadKey) else { return nil }
        return try? JSONDecoder.iso8601.decode(WatchPayload.self, from: data)
    }

    static func write(_ payload: WatchPayload) {
        guard let data = try? JSONEncoder.iso8601.encode(payload) else { return }
        defaults?.set(data, forKey: payloadKey)
    }
}

extension JSONDecoder {
    static var iso8601: JSONDecoder {
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return d
    }
}

extension JSONEncoder {
    static var iso8601: JSONEncoder {
        let e = JSONEncoder()
        e.dateEncodingStrategy = .iso8601
        return e
    }
}
