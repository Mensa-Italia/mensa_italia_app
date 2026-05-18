import SwiftUI

@main
struct MensaWatchApp: App {
    init() {
        // Demo mode: launch arg `--demo` popola un payload mock per dimostrare
        // la UI senza richiedere l'iOS app loggato. Solo per sviluppo.
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--demo") {
            injectDemoPayload()
        }
        #endif
        WatchSessionMirror.shared.activate()
    }

    var body: some Scene {
        WindowGroup {
            RootWatchView()
        }
    }

    #if DEBUG
    private func injectDemoPayload() {
        let payload = WatchPayload(
            card: .init(
                memberId: "12345",
                fullName: "Matteo Sipione",
                expiryFormatted: "31 dic 2026",
                isActive: true,
                qrPng: nil
            ),
            nextEvent: .init(
                id: "evt-001",
                name: "Raduno Mensa Roma",
                startDate: Date().addingTimeInterval(2 * 24 * 3600),
                endDate: nil,
                locationName: "Roma",
                isNational: false
            ),
            generatedAt: Date()
        )
        let encoder = JSONEncoder()
        encoder.dateEncodingStrategy = .iso8601
        if let data = try? encoder.encode(payload) {
            UserDefaults.standard.set(data, forKey: WatchSessionMirror.localKey)
        }
    }
    #endif
}
