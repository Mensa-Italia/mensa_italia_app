package it.mensa.shared.auth

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/** Quando la cache locale va buttata: vedi [OfflineCache]. */
class OfflineCacheTest {

    private val now = Instant.parse("2026-11-09T12:00:00Z")

    @Test
    fun aRecentConfirmationKeepsTheCache() {
        assertFalse(OfflineCache.isStale(now - 1.hours, now))
        assertFalse(OfflineCache.isStale(now - 23.hours, now))
    }

    @Test
    fun exactlyOneDayIsStillValid() {
        assertFalse(OfflineCache.isStale(now - OfflineCache.MAX_AGE, now))
    }

    @Test
    fun moreThanOneDayWithoutTheServerIsStale() {
        assertTrue(OfflineCache.isStale(now - 24.hours - 1.minutes, now))
        assertTrue(OfflineCache.isStale(now - 30.hours, now))
    }

    @Test
    fun noConfirmationAtAllIsStale() {
        assertTrue(OfflineCache.isStale(null, now))
    }

    /** La data del telefono spostata indietro non deve tenere viva la cache. */
    @Test
    fun aConfirmationInTheFutureIsAClockMovedBack() {
        assertTrue(OfflineCache.isStale(now + 2.hours, now))
    }

    @Test
    fun aFewMinutesOfClockSkewAreTolerated() {
        assertFalse(OfflineCache.isStale(now + 3.minutes, now))
    }
}
