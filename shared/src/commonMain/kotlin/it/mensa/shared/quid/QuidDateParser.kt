package it.mensa.shared.quid

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant

/**
 * WordPress REST returns dates like `"2026-03-31T17:30:00"` — no timezone designator.
 * Parses with multi-pattern fallback. Timezone-less strings are interpreted as UTC
 * to match the Swift implementation (which sets `timeZone = UTC` on its DateFormatters).
 */
object QuidDateParser {
    fun parse(raw: String): Instant? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        // 1) Try ISO 8601 with explicit timezone (Z or ±HH:MM), with or without fractional seconds.
        runCatching { return Instant.parse(trimmed) }

        // 2) No timezone designator → parse as LocalDateTime and assume UTC (mirrors Swift).
        val withoutFractional = trimmed.substringBefore('.')
        runCatching {
            return LocalDateTime.parse(withoutFractional).toInstant(TimeZone.UTC)
        }

        return null
    }
}
