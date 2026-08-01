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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "mensa_theme",
)

/** User preference: app color scheme (mirrors iOS `ThemeChoice`). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * ThemeManager — app-level light/dark override with DataStore persistence.
 *
 * - Exposes [mode] as a StateFlow; MainActivity observes it and feeds the
 *   resolved value to MensaTheme, so a change re-themes the whole app live.
 * - Persists the choice across restarts.
 *
 * Inject as singleton via Koin (see AppModule).
 */
class ThemeManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _mode = MutableStateFlow(ThemeMode.SYSTEM)

    /** Currently selected theme mode (SYSTEM until the persisted value loads). */
    val mode: StateFlow<ThemeMode> = _mode.asStateFlow()

    init {
        scope.launch {
            context.themeDataStore.data
                .map { prefs -> prefs[KEY_MODE] }
                .collect { saved ->
                    _mode.value = saved
                        ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                        ?: ThemeMode.SYSTEM
                }
        }
    }

    suspend fun setMode(mode: ThemeMode) {
        _mode.value = mode
        context.themeDataStore.edit { prefs ->
            prefs[KEY_MODE] = mode.name
        }
    }

    companion object {
        private val KEY_MODE = stringPreferencesKey("theme_mode")
    }
}
