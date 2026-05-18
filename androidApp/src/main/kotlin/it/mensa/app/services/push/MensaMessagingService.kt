package it.mensa.app.services.push

import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

// NOTE: FirebaseMessagingService is commented out to avoid a compile error
// when firebase-messaging-ktx is commented out in build.gradle.kts.
// Uncomment both the import AND the parent class once google-services.json
// is present and Firebase deps are re-enabled.
//
// import com.google.firebase.messaging.FirebaseMessagingService
// import com.google.firebase.messaging.RemoteMessage

/**
 * MensaMessagingService — Firebase Cloud Messaging service skeleton.
 *
 * Handles:
 * - [onNewToken]: persist token via [PushTokenStore], upload to PocketBase devices collection
 * - [onMessageReceived]: parse payload via [PushDeepLinkRouter], show notification
 *
 * TODO:
 *  1. Uncomment FirebaseMessagingService inheritance and imports
 *  2. Implement NotificationManager channel creation in Application.onCreate
 *  3. Build and show Notification using NotificationCompat.Builder
 *  4. Route incoming deep-links to live NavController via a shared StateFlow
 *  5. Handle data-only messages (no notification body) for silent syncs
 */
// class MensaMessagingService : FirebaseMessagingService() {
class MensaMessagingService {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pushTokenStore: PushTokenStore by lazy {
        PushTokenStore(appContext())
    }

    @Suppress("UNUSED_PARAMETER")
    fun onNewToken(token: String) {
        Logger.i("Push", "onNewToken", "New FCM token received")
        scope.launch {
            pushTokenStore.set(token)
            // Upload to PocketBase devices collection
            runCatching {
                // TODO: koinAccess().devices.registerToken(token)
            }.onFailure { e ->
                Logger.e("Push", "onNewToken", "Failed to upload token", e)
            }
        }
    }

    // Stub for when FirebaseMessagingService is enabled
    // override fun onMessageReceived(message: RemoteMessage) {
    //     val target = PushDeepLinkRouter.parse(message.data)
    //     Logger.i("Push", "onMessageReceived", "Target: $target")
    //     // TODO: show notification + store pending deep link
    // }

    private fun appContext(): android.content.Context {
        // TODO: replace with injected Context from Koin when FirebaseMessagingService is active
        throw UnsupportedOperationException("Use FirebaseMessagingService context")
    }
}
