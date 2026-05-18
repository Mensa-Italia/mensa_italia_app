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
}
