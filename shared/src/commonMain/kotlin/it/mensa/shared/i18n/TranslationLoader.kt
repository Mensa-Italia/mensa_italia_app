package it.mensa.shared.i18n

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class TranslationLoader(
    private val client: HttpClient,
    private val settings: SettingsApi,
    private val db: MensaDatabase,
    private val json: Json,
) {
    data class Ready(
        val locale: String,
        val baseLocale: String,
        val strings: Map<String, String>,
    )

    private val _ready = MutableStateFlow<Ready?>(null)
    val ready: StateFlow<Ready?> = _ready.asStateFlow()

    private val _availableLocales = MutableStateFlow<List<String>>(listOf("it"))
    val availableLocales: StateFlow<List<String>> = _availableLocales.asStateFlow()

    /**
     * Bootstrap: load cached strings first if present (so UI can render immediately),
     * then refresh from CDN in background.
     *
     * @param preferred IETF tag (e.g. "en", "it", "zh-Hans"); resolved against the
     *                  Tolgee `languages` list, falling back to `base_language`.
     */
    @Suppress("TooGenericExceptionCaught")
    suspend fun bootstrap(preferred: String) {
        // Defensive: this is invoked from Swift through an ObjC-bridged async
        // wrapper. Any unhandled Throwable here would abort the host process
        // because suspend functions are not @Throws by default. We catch
        // everything and degrade to the bundled fallbacks.
        try {
            // 1. Cached path: try restore last-known good immediately
            val cachedLocale = try { readKey("i18n.locale") } catch (_: Throwable) { null }
            val cachedJson = cachedLocale?.let { try { readKey("i18n.payload.$it") } catch (_: Throwable) { null } }
            val cachedBase = try { readKey("i18n.base") } catch (_: Throwable) { null } ?: "it"
            if (cachedLocale != null && cachedJson != null) {
                runCatching { parse(cachedJson) }.getOrNull()?.let { strings ->
                    _ready.value = Ready(cachedLocale, cachedBase, strings)
                }
            }

            // 2. Refresh path: fetch configs, resolve locale, fetch translations
            runCatching {
                val configs = settings.configs()
                val urlTemplate = configs["i18n_flat_url"].orEmpty()
                val languages = configs["languages"]
                    ?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }
                    ?: listOf("it")
                val baseLocale = configs["base_language"] ?: "it"

                _availableLocales.value = languages

                val resolved = resolveLocale(preferred, languages, baseLocale)
                val url = urlTemplate.replace("{locale}", resolved)

                val payload: String = client.get(url).body()
                val strings = parse(payload)

                try { writeKey("i18n.locale", resolved) } catch (_: Throwable) {}
                try { writeKey("i18n.base", baseLocale) } catch (_: Throwable) {}
                try { writeKey("i18n.payload.$resolved", payload) } catch (_: Throwable) {}
                _ready.value = Ready(resolved, baseLocale, strings)
            }
        } catch (_: Throwable) {
            // Swallow — i18n must never crash the host.
        }
        // Always end with at least an empty Ready so the UI never hangs on splash.
        if (_ready.value == null) {
            _ready.value = Ready(preferred, "it", emptyMap())
        }
    }

    private fun parse(raw: String): Map<String, String> {
        // Strip control chars that PocketBase / Tolgee sometimes emit inside
        // translated strings — they make kotlinx.serialization throw.
        val cleaned = buildString(raw.length) {
            for (c in raw) {
                val code = c.code
                if (code >= 0x20 || code == 0x09 || code == 0x0A || code == 0x0D) append(c)
            }
        }
        val obj = json.parseToJsonElement(cleaned) as? JsonObject ?: return emptyMap()
        return obj.mapValues { (_, v) ->
            // JsonPrimitive content unwrap; falls back to trimmed toString for safety
            (v as? kotlinx.serialization.json.JsonPrimitive)?.content ?: v.toString().trim('"')
        }
    }

    private suspend fun readKey(key: String): String? =
        db.keyValueQueries.selectById(key).awaitAsOneOrNull()?.value_

    private suspend fun writeKey(key: String, value: String) {
        db.keyValueQueries.insertOrReplace(key = key, value_ = value)
    }

    private fun resolveLocale(preferred: String, available: List<String>, fallback: String): String {
        // exact match first, then base language (es-MX → es), then fallback
        if (preferred in available) return preferred
        val base = preferred.substringBefore('-')
        return available.firstOrNull { it == base || it.startsWith("$base-") } ?: fallback
    }
}
