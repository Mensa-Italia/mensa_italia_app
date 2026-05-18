@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.i18n.I18n
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

/**
 * JS-facing i18n surface. Wraps [I18n] (which is sync post-bootstrap) so the
 * web layer can resolve Tolgee keys without an async round-trip per call.
 *
 * The Tolgee catalog is loaded during [MensaWebSdk.initialize] (initKoin boots
 * [TranslationLoader] which fetches it.json from the CDN). After that,
 * [t] is fully synchronous.
 *
 * [paramKeysFlat] follows the same flat-array convention used by
 * [MensaWebNotification.params]: `["key1","val1","key2","val2",...]`.
 */
@JsExport
class MensaWebI18n internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val i18n: I18n get() = KoinPlatform.getKoin().get()

    /**
     * Returns the translated string for [key]. Uses [fallback] when the catalog
     * is still loading or the key is absent. [paramKeysFlat] is a flat
     * `[k1, v1, k2, v2, ...]` array that is reconstructed into the args map
     * before ICU `{name}` substitution.
     */
    fun t(key: String, fallback: String, paramKeysFlat: Array<String>): String {
        val args: Map<String, String> = if (paramKeysFlat.isEmpty()) {
            emptyMap()
        } else {
            buildMap {
                var idx = 0
                while (idx + 1 < paramKeysFlat.size) {
                    put(paramKeysFlat[idx], paramKeysFlat[idx + 1])
                    idx += 2
                }
            }
        }
        return i18n.t(key, fallback, args)
    }

    /**
     * Resolves when the SDK (including the Tolgee bootstrap) is ready.
     * After this promise resolves, [t] will return real translations.
     */
    fun awaitReady(): Promise<Unit> = scope.promise { sdk.awaitReady() }
}
