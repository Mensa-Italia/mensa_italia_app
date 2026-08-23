package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.MetadataApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Per-user key/value metadata. In-memory cache only — values are typically
 * small and tied to user session.
 */
class MetadataRepository(
    private val api: MetadataApi,
) {
    private val _state = MutableStateFlow<Map<String, String>>(emptyMap())
    val state: StateFlow<Map<String, String>> = _state.asStateFlow()

    suspend fun refresh(userId: String): Map<String, String> {
        val map = api.getMetadataMap(userId)
        _state.value = map
        return map
    }

    suspend fun set(userId: String, key: String, value: String) {
        api.setMetadata(userId, key, value)
        _state.value = _state.value.toMutableMap().apply { put(key, value) }
    }

    fun get(key: String): String? = _state.value[key]

    /**
     * Pubblica la versione dell'app in esecuzione sotto [KEY_APP_VERSION].
     *
     * Prima di questo, il backend non aveva alcun modo di sapere che versione
     * stesse usando un socio: la versione esisteva solo a schermo, nella riga
     * "Versione" del profilo, e non usciva mai dal dispositivo. Per chi arriva
     * da Flutter il valore era anche peggio che assente — era fermo all'ultima
     * versione Flutter aperta, quindi sembrava un dato valido.
     *
     * La versione la passa la piattaforma: `BuildConfig.VERSION_NAME` su
     * Android, `CFBundleShortVersionString` su iOS. Il modulo condiviso non ha
     * modo di leggerla da solo.
     *
     * Scrive solo quando il valore cambia davvero. Il chiamante gira a ogni
     * ritorno in foreground, quindi senza il confronto sarebbe una scrittura
     * per ogni volta che l'utente riapre l'app.
     */
    suspend fun syncAppVersion(userId: String, version: String) {
        if (userId.isBlank() || version.isBlank()) return
        // La cache e' in memoria e parte vuota a ogni avvio: senza una lettura
        // il confronto qui sotto sarebbe sempre "diverso" e riscriverebbe.
        if (_state.value.isEmpty()) runCatching { refresh(userId) }
        if (_state.value[KEY_APP_VERSION] == version) return
        runCatching { set(userId, KEY_APP_VERSION, version) }
    }

    companion object {
        const val KEY_APP_VERSION: String = "mobile_app_version"
    }
}
