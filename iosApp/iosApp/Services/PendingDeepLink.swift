import SwiftUI

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

    private init() {}

    func set(_ t: NotificationTarget) {
        target = t
    }

    func consume() -> NotificationTarget? {
        let t = target
        target = nil
        return t
    }
}
