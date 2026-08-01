package it.mensa.shared.i18n

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import it.mensa.shared.api.SkipAuthAttribute
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.delay
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
            // 1. Cached path: try restore last-known good immediately.
            //    Prefer a cached payload matching `preferred` (exact tag, then
            //    base language) so a persisted language choice survives a cold
            //    start even offline. The last-fetched locale is only used as a
            //    boot-time fallback: mid-session (language switch) restoring a
            //    stale locale would flash the OLD language before the network
            //    refresh lands.
            val cachedBase = try { readKey("i18n.base") } catch (_: Throwable) { null } ?: "it"
            val lastFetched = try { readKey("i18n.locale") } catch (_: Throwable) { null }
            val candidates = buildList {
                add(preferred)
                val base = preferred.substringBefore('-')
                if (base != preferred) add(base)
            }
            var restoredLocale: String? = null
            var restoredJson: String? = null
            for (cand in candidates) {
                val payload = try { readKey("i18n.payload.$cand") } catch (_: Throwable) { null }
                if (payload != null) {
                    restoredLocale = cand
                    restoredJson = payload
                    break
                }
            }
            if (restoredLocale == null && lastFetched != null && _ready.value == null) {
                val payload = try { readKey("i18n.payload.$lastFetched") } catch (_: Throwable) { null }
                if (payload != null) {
                    restoredLocale = lastFetched
                    restoredJson = payload
                }
            }
            if (restoredLocale != null && restoredJson != null) {
                runCatching { parse(restoredJson) }.getOrNull()?.let { strings ->
                    _ready.value = Ready(restoredLocale, cachedBase, strings)
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

                val payload: String = fetchPayload(url)
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

    /**
     * GET the catalog with the auth header suppressed (third-party CDN must
     * never see the user's Bearer token) and the HTTP status validated —
     * without `expectSuccess`, a CDN error page would reach [parse] as XML.
     * The Tolgee CDN occasionally serves a 403 on origin pulls and caches it
     * for ~5s, so retry with a backoff whose last attempt lands past that TTL.
     */
    private suspend fun fetchPayload(url: String): String {
        var lastError: Throwable? = null
        for (delayMs in listOf(0L, 2_000L, 6_000L)) {
            if (delayMs > 0) delay(delayMs)
            try {
                val response = client.get(url) { attributes.put(SkipAuthAttribute, true) }
                if (response.status.value in 200..299) return response.body()
                lastError = IllegalStateException("HTTP ${response.status.value} fetching $url")
            } catch (t: Throwable) {
                lastError = t
            }
        }
        throw lastError ?: IllegalStateException("i18n fetch failed: $url")
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
