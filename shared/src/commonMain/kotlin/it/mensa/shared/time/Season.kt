package it.mensa.shared.time

import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/**
 * The season the leaderboards and the medals run on: from 1 October to 30
 * September, Europe/Rome. A season is named by the year it starts in, so
 * season `2026` runs from 2026-10-01 to 2027-10-01 (excluded) and is written
 * "2026/27". The server computes the same boundaries (`season` columns of the
 * views): this is only for labels and for telling the current season.
 */
object Season {
    val ZONE: TimeZone = TimeZone.of("Europe/Rome")

    /** The season [instant] falls in. */
    fun of(instant: Instant): Int {
        val date = instant.toLocalDateTime(ZONE).date
        return if (date.monthNumber >= 10) date.year else date.year - 1
    }

    /** The season running now. */
    fun current(now: Instant = Clock.System.now()): Int = of(now)

    /** "2026/27". */
    fun label(season: Int): String = "$season/${((season + 1) % 100).toString().padStart(2, '0')}"

    /** First day of the season and first day of the next one (excluded). */
    fun bounds(season: Int): Pair<LocalDate, LocalDate> =
        LocalDate(season, Month.OCTOBER, 1) to LocalDate(season + 1, Month.OCTOBER, 1)

    /** The same bounds as instants, at midnight Europe/Rome. */
    fun boundsInstants(season: Int): Pair<Instant, Instant> {
        val (start, end) = bounds(season)
        return start.atStartOfDayIn(ZONE) to end.atStartOfDayIn(ZONE)
    }

    /** The last day of the season (30 September of the following year). */
    fun lastDay(season: Int): LocalDate = bounds(season).second.minus(1, DateTimeUnit.DAY)
}
