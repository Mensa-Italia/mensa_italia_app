import Foundation

/// Mirror della struct definita in `iosApp/watchkitapp Watch App/WatchPayload.swift`
/// e `iosApp/Mensa Italia/WatchPayload.swift`. Tenere allineate le 3 copie:
/// gli stessi field name, gli stessi nested type, perché il JSON è il
/// contratto tra iOS (writer) e Watch + Widget (reader).
///
/// Le 3 copie servono perché iosApp.xcodeproj usa gruppi tradizionali per
/// `iosApp/` e `fileSystemSynchronizedGroups` per `Mensa Italia/` e
/// `watchkitapp Watch App/`: condividere un singolo file fisico tra i tre
/// target richiederebbe patch al pbxproj per ogni target membership, mentre
/// duplicare lo schema mantiene il pbxproj quieto.
struct WatchPayload: Codable, Equatable {
    var card: CardSnapshot?
    var nextEvent: EventSnapshot?
    var generatedAt: Date

    struct CardSnapshot: Codable, Equatable {
        var memberId: String
        var fullName: String
        var expiryFormatted: String
        var isActive: Bool
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

    static func write(_ payload: WatchPayload) {
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        guard let data = try? encoder.encode(payload) else { return }
        defaults?.set(data, forKey: payloadKey)
    }

    static func read() -> WatchPayload? {
        guard let data = defaults?.data(forKey: payloadKey) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try? decoder.decode(WatchPayload.self, from: data)
    }
}
