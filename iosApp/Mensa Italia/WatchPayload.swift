import Foundation

/// Vedi `iosApp/watchkitapp Watch App/WatchPayload.swift` per il commento
/// completo. Duplicato qui perché iosApp.xcodeproj usa file system
/// synchronized groups separati per Watch e Widget: lo stesso file fisico non
/// può essere referenziato da entrambi i target senza patch al pbxproj.
/// Tenere le due copie in sync.
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

    static func read() -> WatchPayload? {
        guard let data = defaults?.data(forKey: payloadKey) else { return nil }
        let d = JSONDecoder()
        d.dateDecodingStrategy = .iso8601
        return try? d.decode(WatchPayload.self, from: data)
    }
}
