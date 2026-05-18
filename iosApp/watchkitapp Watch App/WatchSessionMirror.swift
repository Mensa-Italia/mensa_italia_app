import Foundation
import WatchConnectivity
import WidgetKit

/// Riceve gli `applicationContext` inviati dall'iOS app via `WCSession` e
/// li persiste in `UserDefaults.standard` locale del Watch (gli App Groups
/// fra iPhone e Watch non esistono — sono device-locali, quindi usiamo lo
/// UserDefaults dell'app Watch come unica sorgente).
///
/// `RootWatchView` osserva `UserDefaults.didChangeNotification` per
/// ridisegnarsi quando arriva un nuovo payload.
final class WatchSessionMirror: NSObject, WCSessionDelegate {
    static let shared = WatchSessionMirror()
    private override init() { super.init() }

    /// Chiave locale (no App Group prefix) in `UserDefaults.standard`.
    static let localKey = "watch_payload_v1"

    func activate() {
        guard WCSession.isSupported() else { return }
        let session = WCSession.default
        session.delegate = self
        session.activate()
        // Se l'iOS app ha gia' inviato un applicationContext mentre il Watch
        // era spento, viene riconsegnato al `delegate` dopo `activate()`.
    }

    // MARK: - WCSessionDelegate

    func session(
        _ session: WCSession,
        activationDidCompleteWith activationState: WCSessionActivationState,
        error: Error?
    ) {
        // Su attivazione riuscita, prova a leggere il context corrente
        // (consegnato anche se la app non era attiva quando arrivo').
        if !session.receivedApplicationContext.isEmpty {
            store(session.receivedApplicationContext)
        }
    }

    func session(_ session: WCSession, didReceiveApplicationContext applicationContext: [String: Any]) {
        store(applicationContext)
    }

    private func store(_ context: [String: Any]) {
        guard let data = context["payload"] as? Data else { return }
        UserDefaults.standard.set(data, forKey: Self.localKey)
        // Forza un reload della complication appena arrivano nuovi dati.
        DispatchQueue.main.async {
            WidgetCenter.shared.reloadAllTimelines()
        }
    }

    /// Lettura sincrona del payload locale ricevuto. Usata da `RootWatchView`
    /// e dal TimelineProvider del widget.
    static func readPayload() -> WatchPayload? {
        guard let data = UserDefaults.standard.data(forKey: localKey) else { return nil }
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        return try? decoder.decode(WatchPayload.self, from: data)
    }
}
