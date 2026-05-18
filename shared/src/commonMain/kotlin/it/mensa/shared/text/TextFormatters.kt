package it.mensa.shared.text

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Pure-logic helpers ported from `LocalOfficeView.swift`.
 *
 * Kept in commonMain so all platforms (iOS / Android / desktop) share the same
 * formatting rules without relying on platform locale APIs (which are not
 * available in commonMain).
 */
object TextFormatters {
    /**
     * Mirrors the Swift `titleCase(_:)` helper:
     *
     *   s.split(separator: " ").map { word in
     *       first.uppercased() + rest.lowercased()
     *   }.joined(separator: " ")
     *
     * Note: Swift's `split(separator: " ")` drops empty subsequences, so multiple
     * consecutive spaces collapse. We replicate that with `split(' ').filter { it.isNotEmpty() }`.
     */
    fun titleCase(s: String): String =
        s.split(' ')
            .filter { it.isNotEmpty() }
            .joinToString(" ") { word ->
                val first = word.substring(0, 1).uppercase()
                val rest = word.substring(1).lowercase()
                first + rest
            }
}

/**
 * Italian-locale date formatting. Mirrors the Swift `italianDate(from:)` in
 * `LocalOfficeView.swift`, which uses `DateFormatter` with `it_IT`, UTC timezone,
 * `dateStyle = .full` and `timeStyle = .short`.
 *
 * The Swift version always includes the time. We expose [includeTime] as a flag
 * so callers (e.g. group-only contexts) can opt out without rewriting the call site.
 */
object DateFormatters {
    private val italianMonths = listOf(
        "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
        "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre"
    )

    private val italianWeekdays = mapOf(
        // kotlinx-datetime DayOfWeek: MONDAY=1 .. SUNDAY=7
        1 to "lunedì",
        2 to "martedì",
        3 to "mercoledì",
        4 to "giovedì",
        5 to "venerdì",
        6 to "sabato",
        7 to "domenica",
    )

    /**
     * Format an [Instant] using the Italian "full date" style.
     *
     * Examples:
     *  - `includeTime = false` → "domenica 15 marzo 2026"
     *  - `includeTime = true`  → "domenica 15 marzo 2026, 17:30"
     *
     * Timezone is fixed to UTC to match the Swift formatter behavior.
     */
    fun italianLongDate(instant: Instant, includeTime: Boolean = false): String {
        val dt = instant.toLocalDateTime(TimeZone.UTC)
        val weekday = italianWeekdays[dt.dayOfWeek.ordinal + 1] ?: ""
        val month = italianMonths.getOrElse(dt.monthNumber - 1) { "" }
        val datePart = "$weekday ${dt.dayOfMonth} $month ${dt.year}"
        if (!includeTime) return datePart
        val hh = dt.hour.toString().padStart(2, '0')
        val mm = dt.minute.toString().padStart(2, '0')
        return "$datePart, $hh:$mm"
    }
}

