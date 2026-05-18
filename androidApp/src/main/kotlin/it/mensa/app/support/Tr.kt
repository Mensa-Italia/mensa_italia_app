package it.mensa.app.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

/**
 * Tr — Composable translation helper.
 *
 * Looks up [key] in the shared I18n catalog. Recomposes automatically when
 * the LocaleManager bumps its version (locale change). Falls back to [fallback]
 * or the key itself when the catalog is not yet loaded.
 *
 * @param key       dot-notation i18n key (e.g. "events.title")
 * @param fallback  string shown when key is missing (defaults to key itself)
 * @param args      ICU-style `{name}` substitution pairs
 *
 * Example:
 * ```
 * Text(tr("events.noResults", fallback = "Nessun risultato"))
 * Text(tr("events.itemCount", args = "count" to events.size))
 * ```
 */
@Composable
fun tr(
    key: String,
    fallback: String = key,
    vararg args: Pair<String, Any>,
): String {
    val i18n = remember { koinAccess().i18n }
    // Observe ready state so this recomposes when locale changes
    val ready by i18n.ready.collectAsState()
    return i18n.t(key, fallback, args.associate { (k, v) -> k to v.toString() })
}
