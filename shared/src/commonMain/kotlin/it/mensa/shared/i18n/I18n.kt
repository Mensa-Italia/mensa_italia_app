package it.mensa.shared.i18n

import kotlinx.coroutines.flow.StateFlow

/**
 * Strings access point used by Swift via Koin. Returns the translated string,
 * or `fallback` (or the key itself if fallback is null) when the catalog is
 * still loading / missing the key.
 *
 * Interpolation: ICU-style `{name}` placeholders are replaced from `args`.
 * Defensive: we also accept Swift-style `\(name)` placeholders because the
 * Tolgee auto-translation has occasionally rewritten `{amount}` → `\(amount)`
 * after seeing Swift code in surrounding strings. Fixing every translation by
 * hand is whack-a-mole; matching both forms in the runtime keeps the UI sane.
 * (No plurals/gender support yet — out of scope.)
 */
class I18n internal constructor(
    private val loader: TranslationLoader,
) {
    val ready: StateFlow<TranslationLoader.Ready?> get() = loader.ready
    val availableLocales: StateFlow<List<String>> get() = loader.availableLocales

    suspend fun bootstrap(preferred: String) = loader.bootstrap(preferred)

    /**
     * Look up `key` in the loaded catalog. If absent, returns `fallback` when
     * provided, otherwise the key itself (so dev can see the unmapped string in UI).
     */
    fun t(key: String, fallback: String? = null, args: Map<String, String> = emptyMap()): String {
        val template = loader.ready.value?.strings?.get(key) ?: fallback ?: key
        if (args.isEmpty()) return template
        var out = template
        args.forEach { (k, v) ->
            out = out
                .replace("{$k}", v)
                .replace("\\($k)", v)
        }
        return out
    }
}
