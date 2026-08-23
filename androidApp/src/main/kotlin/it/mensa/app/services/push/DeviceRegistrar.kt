package it.mensa.app.services.push

import android.os.Build
import com.google.firebase.messaging.FirebaseMessaging
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Tiene allineata la riga di questo telefono nella collection `users_devices`.
 *
 * Prima non c'era proprio: Firebase era fuori dal `build.gradle.kts`, il
 * `<service>` stava dentro un commento nel manifest e l'upload del token era un
 * `TODO`, quindi Android non compariva fra i dispositivi registrati. Su iOS
 * invece la registrazione c'era ma si ricordava di averla fatta in un flag
 * locale: cancellando il device dal server e rifacendo login, quel flag
 * impediva la nuova POST e il telefono restava fuori.
 *
 * La regola sta in `DevicesRepository.ensureRegistered` in :shared — qui c'e'
 * solo il pezzo Android: da dove arriva il token e come si chiama il telefono.
 *
 * Il token lo chiede direttamente a Firebase invece di limitarsi a leggere
 * quello messo da parte: [MensaMessagingService.onNewToken] scatta solo alla
 * creazione o alla rotazione del token, quindi su un'installazione gia'
 * avviata non scatterebbe mai e nessuno registrerebbe niente. Lo store resta
 * come rete di sicurezza quando FCM non risponde (nessuna rete, Play Services
 * assenti).
 */
class DeviceRegistrar(
    private val tokenStore: PushTokenStore,
) {

    /**
     * Verifica sul server che questo telefono risulti registrato, e lo
     * registra se non c'e'. Idempotente: la si puo' chiamare a ogni avvio, a
     * ogni ritorno in foreground e a ogni rotazione del token.
     */
    suspend fun ensureRegistered() {
        val token = fcmToken() ?: tokenStore.tokenFlow.first().orEmpty()
        if (token.isBlank()) return
        // Il token vivo diventa anche quello memorizzato: se e' ruotato senza
        // che `onNewToken` sia arrivato a destinazione, da qui si riallinea.
        runCatching { tokenStore.set(token) }

        val user = koinAccess().auth.currentUser.value ?: return
        if (user.id.isBlank()) return

        val registered = koinAccess().devices.ensureRegistered(
            userId = user.id,
            firebaseToken = token,
            deviceName = deviceName(),
            language = Locale.getDefault().toLanguageTag(),
        )
        if (registered == null) {
            Logger.w("Push", "ensureRegistered", "device non registrato (rete o permessi)")
        }
    }

    /**
     * Token FCM corrente, o null se Firebase non e' in grado di darlo (rete
     * assente, Play Services mancanti, progetto non configurato).
     *
     * `suspendCancellableCoroutine` invece di `Task.await()` per non tirarsi
     * dentro `kotlinx-coroutines-play-services` solo per una chiamata.
     */
    private suspend fun fcmToken(): String? = suspendCancellableCoroutine { cont ->
        runCatching {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!cont.isActive) return@addOnCompleteListener
                cont.resume(if (task.isSuccessful) task.result else null)
            }
        }.onFailure {
            if (cont.isActive) cont.resume(null)
        }
    }

    /** "Pixel 8 Pro", "SM-S918B"… quello che l'utente riconosce nella lista. */
    private fun deviceName(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { it.uppercase() }
        val model = Build.MODEL.orEmpty()
        return when {
            model.isBlank() -> manufacturer.ifBlank { "Android" }
            model.startsWith(manufacturer, ignoreCase = true) -> model
            manufacturer.isBlank() -> model
            else -> "$manufacturer $model"
        }
    }
}
