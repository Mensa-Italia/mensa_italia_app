package it.mensa.app.support

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * I18nDynamicKeysRegistry — registry for runtime-resolved i18n keys.
 *
 * Keys registered here are looked up at render time via [Tr].
 * This pattern allows feature modules to declare their own key sets
 * without a compile-time dependency on a central strings file.
 *
 * Keys are grouped by namespace (feature module name).
 */
object I18nDynamicKeysRegistry {

    private val _keys = MutableStateFlow<Map<String, Set<String>>>(emptyMap())

    /** Snapshot of all registered keys grouped by namespace */
    val keys: StateFlow<Map<String, Set<String>>> = _keys.asStateFlow()

    /**
     * Register a set of translation keys under a namespace.
     * Safe to call multiple times; keys are merged idempotently.
     *
     * @param namespace feature module identifier (e.g. "events", "profile")
     * @param keySet    i18n dot-notation key strings
     */
    fun register(namespace: String, keySet: Set<String>) {
        val current = _keys.value.toMutableMap()
        current[namespace] = (current[namespace] ?: emptySet()) + keySet
        _keys.value = current
    }

    /** Return all registered keys as a flat set */
    fun allKeys(): Set<String> = _keys.value.values.flatten().toSet()
}
