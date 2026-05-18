import Foundation
import UIKit
import Shared

/// Bridges FCM registration tokens to the shared `DevicesRepository`.
///
/// Mirrors Flutter `Api.addDevice` (api.dart):
///   1. Wait for an authenticated user.
///   2. Register the device with `{user, firebase_id, device_name, language}`.
///   3. Skip re-registration when the same `(userId, token)` pair has already
///      been uploaded — persisted in UserDefaults so we don't hammer the API
///      on every cold launch / token refresh callback.
///
/// We do NOT delete duplicates from other users here (Flutter's
/// `removeSimilarDevice`): the backend already de-dupes via `findByFirebaseId`,
/// and on this side `register()` will fail with a unique-constraint error which
/// we swallow.
@MainActor
final class PushTokenStore {
    static let shared = PushTokenStore()
    private init() {}

    private let lastUploadedKey = "push.token.lastUploaded"

    /// Latest FCM token observed in this process. Set by `AppDelegate`'s
    /// `MessagingDelegate.messaging(_:didReceiveRegistrationToken:)`.
    private(set) var currentToken: String?

    /// Called by the `MessagingDelegate` callback. Stashes the token and
    /// attempts an upload (no-op if user not yet authenticated — the next
    /// post-login call to `uploadIfPossible()` will retry).
    func handle(token: String) {
        currentToken = token
        Task { await uploadIfPossible() }
    }

    /// Call after a successful login (or from `MainTabView.onAppear`) to
    /// flush the latest token to the backend now that we know the user id.
    func uploadIfPossible() async {
        guard let token = currentToken, !token.isEmpty else { return }
        guard let user = koin.auth.currentUser.value as? UserModel, !user.id.isEmpty else {
            return
        }

        let cacheKey = "\(user.id)|\(token)"
        if UserDefaults.standard.string(forKey: lastUploadedKey) == cacheKey {
            return
        }

        let deviceName = await UIDevice.current.model
        let language = Locale.current.identifier

        do {
            _ = try await koin.devices.register(
                userId: user.id,
                firebaseToken: token,
                deviceName: deviceName,
                language: language
            )
            UserDefaults.standard.set(cacheKey, forKey: lastUploadedKey)
        } catch {
            // Likely unique-constraint (token already registered for this user
            // by a previous install). Cache anyway so we don't retry forever.
            UserDefaults.standard.set(cacheKey, forKey: lastUploadedKey)
        }
    }
}
