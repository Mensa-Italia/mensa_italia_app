import Foundation
import UIKit
import UserNotifications

/// Requests push notification permission from the user and, on grant, asks
/// the system to register for remote notifications (which triggers APNs token
/// delivery on `AppDelegate.didRegisterForRemoteNotificationsWithDeviceToken`).
///
/// Mirrors the Flutter flow in `Api.addDevice` (api.dart): permission first,
/// then `FirebaseMessaging.getToken()` (here driven by the APNs callback).
enum PushPermissionRequester {
    /// Marker stored in UserDefaults to avoid asking again every cold launch.
    /// We still call `registerForRemoteNotifications` on each launch when granted
    /// so the APNs token refreshes if it ever changes.
    private static let askedKey = "push.permission.asked"

    @discardableResult
    static func requestIfNeeded() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let current = await center.notificationSettings()

        switch current.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            await MainActor.run { UIApplication.shared.registerForRemoteNotifications() }
            return true
        case .denied:
            return false
        case .notDetermined:
            fallthrough
        @unknown default:
            do {
                let granted = try await center.requestAuthorization(options: [.alert, .badge, .sound])
                UserDefaults.standard.set(true, forKey: askedKey)
                if granted {
                    await MainActor.run { UIApplication.shared.registerForRemoteNotifications() }
                }
                return granted
            } catch {
                return false
            }
        }
    }
}
