package it.mensa.app.features.quid.util

import it.mensa.app.support.AppFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * QuidDateParser — mirrors iOS QuidDateParser.swift.
 *
 * WordPress REST returns dates like "2026-03-31T17:30:00" — no timezone designator.
 * Tries multiple patterns, treating the value as UTC for display purposes.
 */
object QuidDateParser {

    private val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ssZ",
        "yyyy-MM-dd'T'HH:mm:ss.SSSZ",
    ).map { pattern ->
        SimpleDateFormat(pattern, Locale.US).also {
            it.timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    fun parse(raw: String): Date? {
        for (fmt in formats) {
            try {
                return fmt.parse(raw)
            } catch (_: Exception) {
                // try next
            }
        }
        return null
    }

    /**
     * Returns relative time string ("2 giorni fa") or the raw string on failure.
     */
    fun relativeDateText(raw: String): String {
        val date = parse(raw) ?: return raw
        val now = System.currentTimeMillis()
        val diffMs = now - date.time
        val diffMin = diffMs / 60_000
        val diffHours = diffMs / 3_600_000
        val diffDays = diffMs / 86_400_000

        return when {
            diffMin < 1 -> "adesso"
            diffMin < 60 -> "circa ${diffMin}m fa"
            diffHours < 24 -> "circa ${diffHours}h fa"
            diffDays == 1L -> "ieri"
            diffDays < 7 -> "${diffDays} giorni fa"
            diffDays < 30 -> "${diffDays / 7} sett. fa"
            diffDays < 365 -> "${diffDays / 30} mesi fa"
            else -> "${diffDays / 365} ann. fa"
        }
    }

    /**
     * Long date in the in-app language ("31 marzo 2026" / "31 March 2026"), or
     * the raw string when [parse] cannot make sense of it.
     *
     * Note this is the *output* side only — [parse] still reads the Italian
     * month names the backend publishes, and must not follow the UI language.
     */
    fun longDateText(raw: String, locale: Locale = AppFormat.locale()): String {
        val date = parse(raw) ?: return raw
        val fmt = AppFormat.formatter(AppFormat.Skeleton.DAY_MONTH_LONG_YEAR, locale)
        fmt.timeZone = TimeZone.getDefault()
        return fmt.format(date)
    }
}
