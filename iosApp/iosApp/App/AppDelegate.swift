import UIKit
import UserNotifications
import BackgroundTasks
import FirebaseCore
import FirebaseMessaging

/// Hosts Firebase/APNs lifecycle for the SwiftUI app.
///
/// Mirrors Flutter's setup in `mensa_italia_app/ios/Runner/AppDelegate.swift`
/// + `lib/api/api.dart` + `lib/ui/views/home/home_viewmodel.dart`.
///
///   * Firebase configured at launch (we disable
///     `FirebaseAppDelegateProxyEnabled` so APNs token handoff is explicit).
///   * `MessagingDelegate.messaging(_:didReceiveRegistrationToken:)` forwards
///     the FCM token to `PushTokenStore` for backend upload.
///   * `UNUserNotificationCenterDelegate.userNotificationCenter(_:didReceive:)`
///     routes a tapped push (foreground/background/cold-launch) through
///     `PushDeepLinkRouter`.
///   * `userNotificationCenter(_:willPresent:)` shows the banner in-app.
final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        Messaging.messaging().delegate = self
        UNUserNotificationCenter.current().delegate = self

        // BGTaskScheduler: il register DEVE avvenire qui (prima del ritorno
        // di didFinishLaunchingWithOptions), altrimenti iOS rifiuta i task.
        // Il primo refresh + schedule sono triggerati da `BootstrapGate` via
        // `markBootstrapped()` quando il DB SQLDelight è pronto — toccare koin
        // qui esplode perché `initializeMensaDatabase` non è ancora girato.
        SpotlightRefreshCoordinator.shared.registerBGTask()

        // Cold-launch tap: the system stashes the remote notification payload
        // in launchOptions. Route immediately — `PushDeepLinkRouter.post`
        // stashes the target in `PendingDeepLink`, which `MainTabView`
        // drains on appear once the auth/onboarding gate is past.
        if let payload = launchOptions?[.remoteNotification] as? [AnyHashable: Any] {
            PushDeepLinkRouter.route(payload)
        }
        return true
    }

    // MARK: - APNs token

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        // Hand the APNs token to FCM; the FCM token will arrive on the
        // `MessagingDelegate` callback below.
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(
        _ application: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: Error
    ) {
        // Logged but not fatal — push just won't work this session.
        NSLog("[Push] APNs registration failed: \(error.localizedDescription)")
    }

    func applicationDidBecomeActive(_ application: UIApplication) {
        // Background→foreground: stesso throttle del cold-launch (1h).
        SpotlightRefreshCoordinator.shared.refreshIfNeeded(reason: "active")
    }

    // MARK: - MessagingDelegate

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken, !token.isEmpty else { return }
        Task { @MainActor in
            PushTokenStore.shared.handle(token: token)
        }
    }

    // MARK: - UNUserNotificationCenterDelegate

    /// Foreground presentation — same set Flutter uses (banner/list/badge/sound).
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .list, .sound, .badge])
    }

    /// User tapped the notification (foreground, background, or cold-launch
    /// re-delivery). Mirrors Flutter `FirebaseMessaging.onMessageOpenedApp`.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        PushDeepLinkRouter.route(userInfo)
        // Full rebuild Spotlight per push documents-related (single o multiple).
        // Il backend manda questi tipi quando ci sono nuovi documenti — è il
        // momento giusto per scaricare e reindicizzare tutto.
        if let type = userInfo["type"] as? String,
           type == "single_document" || type == "multiple_documents" {
            SpotlightRefreshCoordinator.shared.fullRebuild(reason: "push:\(type)")
        }
        completionHandler()
    }

    // Silent push (content-available=1): wake up per rebuild senza UI.
    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        if let type = userInfo["type"] as? String,
           type == "single_document" || type == "multiple_documents" {
            SpotlightRefreshCoordinator.shared.fullRebuild(reason: "silent-push:\(type)")
            completionHandler(.newData)
            return
        }
        completionHandler(.noData)
    }
}
