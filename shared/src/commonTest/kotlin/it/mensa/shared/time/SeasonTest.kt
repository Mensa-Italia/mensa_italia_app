package it.mensa.shared.time

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class SeasonTest {
    @Test
    fun theSeasonStartsOnTheFirstOfOctober() {
        // 30 September 23:59 in Rome (21:59 UTC, summer time) is still the old season.
        assertEquals(2025, Season.of(Instant.parse("2026-09-30T21:59:59Z")))
        // Midnight in Rome on 1 October opens the new one.
        assertEquals(2026, Season.of(Instant.parse("2026-09-30T22:00:00Z")))
        assertEquals(2026, Season.of(Instant.parse("2026-10-01T10:00:00Z")))
    }

    @Test
    fun theSeasonSpansTheNewYear() {
        assertEquals(2026, Season.of(Instant.parse("2026-12-31T12:00:00Z")))
        assertEquals(2026, Season.of(Instant.parse("2027-01-01T12:00:00Z")))
        assertEquals(2026, Season.of(Instant.parse("2027-09-15T12:00:00Z")))
    }

    @Test
    fun theBoundaryFollowsRomeNotUtc() {
        // 1 October 00:30 in Rome is 30 September 22:30 UTC: already the new season.
        assertEquals(2026, Season.of(Instant.parse("2026-09-30T22:30:00Z")))
        // 31 December 23:30 UTC is already 1 January in Rome: same season either way.
        assertEquals(2026, Season.of(Instant.parse("2026-12-31T23:30:00Z")))
    }

    @Test
    fun currentUsesTheGivenClock() {
        assertEquals(2025, Season.current(Instant.parse("2026-06-01T00:00:00Z")))
        assertEquals(2026, Season.current(Instant.parse("2026-10-31T00:00:00Z")))
    }

    @Test
    fun labelsUseTwoDigitsForTheEndingYear() {
        assertEquals("2026/27", Season.label(2026))
        assertEquals("2025/26", Season.label(2025))
        assertEquals("2099/00", Season.label(2099))
        assertEquals("2008/09", Season.label(2008))
    }

    @Test
    fun boundsRunFromFirstOctoberToTheNextOne() {
        val (start, end) = Season.bounds(2026)
        assertEquals(LocalDate(2026, 10, 1), start)
        assertEquals(LocalDate(2027, 10, 1), end)
        assertEquals(LocalDate(2027, 9, 30), Season.lastDay(2026))
        val (from, to) = Season.boundsInstants(2026)
        assertEquals(Instant.parse("2026-09-30T22:00:00Z"), from)
        assertEquals(Instant.parse("2027-09-30T22:00:00Z"), to)
    }
}
