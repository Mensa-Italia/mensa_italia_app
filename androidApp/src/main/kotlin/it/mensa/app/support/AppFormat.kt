package it.mensa.app.support

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import kotlinx.datetime.Instant
import org.koin.core.context.GlobalContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import android.text.format.DateFormat as AndroidDateFormat

/**
 * Locale-aware formatting for dates and money.
 *
 * The formatting locale is [LocaleManager]'s, **not** [Locale.getDefault]: the
 * app ships its own language picker (Profilo → Lingua), so someone reading the
 * app in English on an Italian phone has to get English months and English
 * number grouping.
 *
 * Formatters are cached per (locale, pattern). Caching a single instance — the
 * shape this code had before — means the first language the app formats in is
 * the language it keeps formatting in for the rest of the process, because
 * nothing resets object state when the user switches.
 */
object AppFormat {

    /** Active in-app language. Falls back to the system locale before Koin is up. */
    fun locale(): Locale {
        val tag = runCatching {
            GlobalContext.get().get<LocaleManager>().currentLocale.value
        }.getOrNull().orEmpty()
        return if (tag.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(tag)
    }

    private val dateCache = ConcurrentHashMap<String, SimpleDateFormat>()

    /**
     * Builds a formatter from an ICU **skeleton** (`dMMMy`, `EEEEdMMMMyHHmm`) —
     * a set of fields, not a literal pattern. `getBestDateTimePattern` then
     * orders those fields the way the language actually writes dates, so
     * Japanese gets `2026年8月17日` instead of the Italian day-month-year layout
     * with the month name swapped out.
     *
     * Use [Skeleton] rather than passing raw strings.
     */
    fun formatter(skeleton: String, locale: Locale = locale()): SimpleDateFormat =
        prototype(skeleton, locale).clone() as SimpleDateFormat

    /**
     * Cached instance, never handed out: `SimpleDateFormat` is mutable and not
     * thread-safe, so a caller setting `timeZone` on it (the test-date screens
     * pin UTC) would silently change every other screen's formatter. [formatter]
     * clones, which is far cheaper than parsing the pattern again.
     */
    private fun prototype(skeleton: String, locale: Locale): SimpleDateFormat =
        dateCache.getOrPut("${locale.toLanguageTag()}|$skeleton") {
            SimpleDateFormat(AndroidDateFormat.getBestDateTimePattern(locale, skeleton), locale)
        }

    fun format(date: Date, skeleton: String, locale: Locale = locale()): String =
        prototype(skeleton, locale).format(date)

    /** Returns `"-"` for a null or zero instant, mirroring the iOS helper. */
    fun format(instant: Instant?, skeleton: String, locale: Locale = locale()): String {
        val millis = instant?.toEpochMilliseconds() ?: return "-"
        if (millis <= 0L) return "-"
        return format(Date(millis), skeleton, locale)
    }

    private val moneyCache = ConcurrentHashMap<String, NumberFormat>()

    /**
     * Euro amounts. The currency stays EUR in every language — the association
     * bills in euro no matter who is reading — while grouping, decimal
     * separator and symbol position follow the locale: `1.234,56 €` in Italian,
     * `€1,234.56` in English.
     */
    fun money(locale: Locale = locale()): NumberFormat =
        moneyPrototype(locale).clone() as NumberFormat

    fun money(amount: Number, locale: Locale = locale()): String =
        moneyPrototype(locale).format(amount)

    /**
     * Returns a **clone** to callers ([money]) because `NumberFormat` is mutable
     * and not thread-safe: a screen setting `maximumFractionDigits` on the
     * cached instance would change it for every other screen too.
     */
    private fun moneyPrototype(locale: Locale): NumberFormat =
        moneyCache.getOrPut(locale.toLanguageTag()) {
            NumberFormat.getCurrencyInstance(locale).apply {
                currency = Currency.getInstance(EURO)
            }
        }

    /** Upper-cases the first character using the active locale's casing rules. */
    fun titlecase(value: String, locale: Locale = locale()): String =
        value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

    private const val EURO = "EUR"

    /**
     * Field sets shared across screens. Named after what they show, not after a
     * literal pattern, because the rendered order changes per language.
     */
    object Skeleton {
        /** 17 ago 2026 */
        const val DAY_MONTH_YEAR = "dMMMy"

        /** 17 agosto 2026 */
        const val DAY_MONTH_LONG_YEAR = "dMMMMy"

        /** 17/08/2026 */
        const val NUMERIC_DATE = "dMy"

        /** 17/08/2026 19:45 */
        const val NUMERIC_DATE_TIME = "dMyHHmm"

        /** lunedì 17 agosto 2026 */
        const val FULL_DAY = "EEEEdMMMMy"

        /** lunedì 17 agosto 2026, 19:45 */
        const val FULL_WITH_TIME = "EEEEdMMMMyHHmm"

        /** 17 ago 2026, 19:45 */
        const val MEDIUM_WITH_TIME = "dMMMyHHmm"

        /** 17 ago */
        const val DAY_MONTH = "dMMM"

        /** lunedì, 17 agosto */
        const val WEEKDAY_DAY_MONTH = "EEEEdMMMM"

        /** 19:45 */
        const val TIME = "HHmm"

        /** agosto 2026 */
        const val MONTH_YEAR = "LLLLy"
    }
}

/**
 * Active locale, read during composition so the screen re-renders when the
 * user switches language. Compose call sites should take this and hand it to
 * [AppFormat] explicitly — calling `AppFormat.locale()` inside a composable
 * reads no snapshot state and would leave the screen showing the old language
 * until something else happens to recompose it.
 */
@Composable
fun rememberAppLocale(): Locale {
    val manager = remember { GlobalContext.get().get<LocaleManager>() }
    val tag by manager.currentLocale.collectAsState()
    return remember(tag) { if (tag.isEmpty()) Locale.getDefault() else Locale.forLanguageTag(tag) }
}
