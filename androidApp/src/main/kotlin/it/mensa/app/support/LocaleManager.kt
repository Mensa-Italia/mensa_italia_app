package it.mensa.app.support

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale

private val Context.localeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mensa_locale",
)

/**
 * LocaleManager — app-level locale override with DataStore persistence.
 *
 * - Exposes [currentLocale] as a StateFlow for Compose observation.
 * - Persists locale choice across restarts.
 * - Call [setLocale] to switch language; the Compose i18n system will re-bootstrap.
 *
 * Inject as singleton via Koin (see AppModule).
 */
class LocaleManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _currentLocale = MutableStateFlow(Locale.getDefault().language)

    /** Currently active locale language code (e.g. "it", "en") */
    val currentLocale: StateFlow<String> = _currentLocale.asStateFlow()

    init {
        scope.launch {
            // Restore persisted locale on startup
            val saved = context.localeDataStore.data
                .map { prefs -> prefs[KEY_LOCALE] }
                .first()
            if (saved != null) {
                _currentLocale.value = saved
            }
        }
    }

    /**
     * Change the active locale and persist the choice.
     * Triggers re-bootstrap of I18n in upstream consumers.
     */
    suspend fun setLocale(languageCode: String) {
        _currentLocale.value = languageCode
        context.localeDataStore.edit { prefs ->
            prefs[KEY_LOCALE] = languageCode
        }
    }

    /** Clear persisted locale override, reverting to system default */
    suspend fun clearLocale() {
        context.localeDataStore.edit { prefs ->
            prefs.remove(KEY_LOCALE)
        }
        _currentLocale.value = Locale.getDefault().language
    }

    companion object {
        private val KEY_LOCALE = stringPreferencesKey("locale_override")
    }
}
