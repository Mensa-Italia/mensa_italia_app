import Foundation
import Shared

extension Foundation.Notification.Name {
    /// Posted by `AppDelegate` when the user taps a remote notification (or it
    /// cold-launches the app). The `userInfo` carries a parsed
    /// `NotificationTarget` under the `payload` key.
    static let mensaDeepLink = Foundation.Notification.Name("it.mensa.deepLink")
}

/// Centralizes parsing of an APNs/FCM `userInfo` payload into the typed
/// `NotificationTarget` enum (declared in
/// `Features/Notifications/NotificationsListView.swift`) and routing it via
/// `NotificationCenter`.
///
/// Parsing itself is delegated to the shared KMP `NotificationRouter` —
/// this file only handles the iOS-specific bridging from
/// `[AnyHashable: Any]` to `Map<String, String>`.
enum PushDeepLinkRouter {
    static let payloadKey = "payload"

    /// Parse a raw APNs/FCM dictionary into a `NotificationTarget` by
    /// stringifying values and delegating to the KMP router.
    static func target(from userInfo: [AnyHashable: Any]) -> NotificationTarget? {
        var data: [String: String] = [:]
        for (k, v) in userInfo {
            guard let key = k as? String else { continue }
            if let s = v as? String {
                data[key] = s
            } else {
                data[key] = String(describing: v)
            }
        }
        guard let shared = NotificationRouter.shared.targetFromData(data: data) else {
            return nil
        }
        return NotificationTarget(shared: shared)
    }

    /// Posts a `.mensaDeepLink` notification on the main queue so observers
    /// in SwiftUI views can drive navigation. Also stashes the target in
    /// `PendingDeepLink` so a cold-launch tap isn't lost when `MainTabView`
    /// hasn't subscribed yet (mirrors Flutter's `getInitialMessage()` path).
    static func post(target: NotificationTarget) {
        Task { @MainActor in
            PendingDeepLink.shared.set(target)
        }
        NotificationCenter.default.post(
            name: .mensaDeepLink,
            object: nil,
            userInfo: [payloadKey: target]
        )
    }

    /// Convenience: parse + post in one step. No-op when payload doesn't map
    /// to a known target (e.g. a marketing push without `type`).
    static func route(_ userInfo: [AnyHashable: Any]) {
        if let target = target(from: userInfo) {
            post(target: target)
        }
    }
}
