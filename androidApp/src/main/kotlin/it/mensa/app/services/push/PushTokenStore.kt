package it.mensa.app.services.push

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.pushTokenDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mensa_push_token",
)

/**
 * PushTokenStore — DataStore-backed FCM token registry.
 *
 * - [tokenFlow]: observe the current FCM token (null before first token issued)
 * - [set]: persist a new token (called by [MensaMessagingService.onNewToken])
 * - [clear]: remove token on logout
 */
class PushTokenStore(private val context: Context) {

    /** Current FCM token as a Flow — null until first token is received */
    val tokenFlow: Flow<String?> = context.pushTokenDataStore.data
        .map { prefs -> prefs[KEY_TOKEN] }

    /** Persist a new FCM token */
    suspend fun set(token: String) {
        context.pushTokenDataStore.edit { prefs ->
            prefs[KEY_TOKEN] = token
        }
    }

    /** Clear the stored token (e.g. on user logout) */
    suspend fun clear() {
        context.pushTokenDataStore.edit { prefs ->
            prefs.remove(KEY_TOKEN)
        }
    }

    companion object {
        private val KEY_TOKEN = stringPreferencesKey("fcm_token")
    }
}
