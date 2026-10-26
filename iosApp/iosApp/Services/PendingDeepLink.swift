import SwiftUI
import Shared

/// Holder per un `NotificationTarget` ricevuto da una push prima che
/// `MainTabView` (il listener di `.mensaDeepLink`) sia montato.
///
/// Replica la semantica di `FirebaseMessaging.getInitialMessage()` usata da
/// Flutter in `home_viewmodel.dart:checkForInitialMessage()`: al cold-launch
/// il payload viene letto solo quando la Home è pronta a gestirlo.
///
/// `AppDelegate` scrive sempre qui (oltre a postare su `NotificationCenter`)
/// così il cold-launch path non perde il deep-link, e `MainTabView.onAppear`
/// drena lo slot.
@MainActor
final class PendingDeepLink: ObservableObject {
    static let shared = PendingDeepLink()

    @Published var target: NotificationTarget?

    /// Francobollo arrivato da un link `svc.mensa.it/links/stamp/…` (il QR
    /// inquadrato con la Fotocamera). Non è un `NotificationTarget` perché non
    /// apre una pagina: apre la conferma che apre anche il lettore QR.
    @Published var stamp: PendingStamp?

    private init() {}

    func set(_ t: NotificationTarget) {
        target = t
    }

    func consumeStamp() -> PendingStamp? {
        let s = stamp
        stamp = nil
        return s
    }

    /// Link universale `https://svc.mensa.it/links/…`. Il server lo dichiara
    /// già nell'`apple-app-site-association`; senza l'entitlement `applinks`
    /// il QR di un francobollo apriva Safari sulla pagina "apri l'app".
    /// Torna false per qualunque altro URL.
    @discardableResult
    func open(url: URL) -> Bool {
        guard let link = AppLinkParser.shared.parse(url: url.absoluteString) else { return false }
        if let event = link as? AppLink.Event {
            target = .event(event.eventId)
        } else if let stampLink = link as? AppLink.Stamp {
            stamp = PendingStamp(
                stampId: stampLink.payload.stampId,
                code: stampLink.payload.verificationCode
            )
        } else {
            return false
        }
        return true
    }

    func consume() -> NotificationTarget? {
        let t = target
        target = nil
        return t
    }
}

struct PendingStamp: Identifiable, Equatable {
    let stampId: String
    let code: String
    var id: String { "\(stampId):::\(code)" }
}
