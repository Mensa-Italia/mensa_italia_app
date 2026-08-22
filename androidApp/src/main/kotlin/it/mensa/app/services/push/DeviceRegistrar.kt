package it.mensa.app.services.push

import android.content.Context
import android.os.Build
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import kotlinx.coroutines.flow.first
import java.util.Locale

/**
 * Tiene allineata la riga di questo telefono nella collection `users_devices`.
 *
 * Prima non c'era: il token FCM finiva in [PushTokenStore] e li' restava
 * ([MensaMessagingService.onNewToken] ha ancora il `TODO` per l'upload), quindi
 * Android non compariva proprio fra i dispositivi registrati. Su iOS invece la
 * registrazione c'era ma si ricordava di averla fatta in un flag locale:
 * cancellando il device dal server e rifacendo login, quel flag impediva la
 * nuova POST e il telefono restava fuori.
 *
 * La regola sta in `DevicesRepository.ensureRegistered` in :shared — qui c'e'
 * solo il pezzo Android: da dove arriva il token e come si chiama il telefono.
 *
 * Nota: senza `google-services.json` non esiste nessun token FCM (le dipendenze
 * Firebase sono commentate in `androidApp/build.gradle.kts`), quindi oggi
 * questa classe non fa niente. E' cablata lo stesso perche' e' il punto in cui
 * la registrazione deve avvenire il giorno in cui Firebase si riaccende:
 * [MensaMessagingService.onNewToken] scrive il token nello store e questa la
 * pubblica.
 */
class DeviceRegistrar(
    private val context: Context,
    private val tokenStore: PushTokenStore,
) {

    /**
     * Verifica sul server che questo telefono risulti registrato, e lo
     * registra se non c'e'. Idempotente: la si puo' chiamare a ogni avvio, a
     * ogni ritorno in foreground e a ogni rotazione del token.
     */
    suspend fun ensureRegistered() {
        val token = tokenStore.tokenFlow.first().orEmpty()
        if (token.isBlank()) return

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
