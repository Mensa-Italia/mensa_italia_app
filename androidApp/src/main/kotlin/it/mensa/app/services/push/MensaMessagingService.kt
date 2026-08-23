package it.mensa.app.services.push

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import it.mensa.app.MainActivity
import it.mensa.app.R
import it.mensa.app.support.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import kotlin.random.Random

/**
 * Servizio FCM di Android.
 *
 * Per mesi questa classe non e' stata un `Service`: l'ereditarieta' da
 * [FirebaseMessagingService] era commentata via, il `<service>` nel manifest
 * stava dentro un commento XML, le dipendenze Firebase erano fuori dal
 * `build.gradle.kts` e l'upload del token era un `TODO`. Risultato: Android non
 * compariva fra i dispositivi registrati e non riceveva niente, mentre iOS
 * funzionava. Adesso il giro e' chiuso.
 *
 * Due responsabilita':
 *
 *  - [onNewToken]: il token nuovo o ruotato finisce in [PushTokenStore] e viene
 *    pubblicato subito con [DeviceRegistrar].
 *  - [onMessageReceived]: mostra la notifica. Serve per i messaggi che arrivano
 *    con l'app in primo piano e per quelli `data`-only; per gli altri, con
 *    l'app in background, ci pensa il sistema.
 *
 * Il tap porta l'utente sulla schermata giusta: il payload viaggia negli extra
 * dell'Intent, [it.mensa.app.MainActivity] lo parcheggia in
 * [PendingPushTarget] e [it.mensa.app.ui.shell.MainAppShell] lo drena quando il
 * NavController esiste. Vale sia ad app aperta sia da cold-launch.
 */
class MensaMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logger.i("Push", "onNewToken", "nuovo token FCM")
        scope.launch {
            runCatching {
                KoinPlatform.getKoin().get<PushTokenStore>().set(token)
                // Se l'utente non e' ancora loggato qui non succede niente:
                // ci ripensa MainActivity.onResume al primo avvio da autenticato.
                KoinPlatform.getKoin().get<DeviceRegistrar>().ensureRegistered()
            }.onFailure { e ->
                Logger.e("Push", "onNewToken", "registrazione device fallita", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notification = message.notification
        val title = notification?.title ?: message.data[KEY_TITLE] ?: return
        val body = notification?.body ?: message.data[KEY_BODY].orEmpty()

        // Il controllo non e' solo per lint (`MissingPermission` su notify()
        // farebbe fallire lintVitalRelease): su Android 13+ senza permesso la
        // notifica non si vedrebbe comunque, tanto vale non costruirla.
        if (!canPostNotifications()) {
            Logger.w("Push", "onMessageReceived", "POST_NOTIFICATIONS non concesso")
            return
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            message.data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pending = PendingIntent.getActivity(
            this,
            Random.nextInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val built = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        getSystemService(NotificationManager::class.java)
            ?.notify(Random.nextInt(), built)
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
