package it.mensa.shared.medals

import it.mensa.shared.model.MedalStatsModel
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MedalRulesTest {
    private val now = Instant.parse("2026-10-26T00:00:00Z")

    private fun status(id: String, stats: MedalStatsModel) =
        MedalRules.evaluate(stats, now).first { it.id == id }

    @Test
    fun tieredMedalClimbsWithTheCounter() {
        val none = status("collector", MedalStatsModel())
        assertNull(none.earnedTier)
        assertEquals(MedalTier.BRONZE, none.nextTier)
        assertEquals(MedalTier.BRONZE, none.displayTier)

        val silver = status("collector", MedalStatsModel(stampsTotal = 30.0))
        assertEquals(MedalTier.SILVER, silver.earnedTier)
        assertEquals(MedalTier.GOLD, silver.nextTier)
        assertEquals(100, silver.nextThreshold)
        assertEquals(0.3, silver.progress, 0.0001)

        val top = status("collector", MedalStatsModel(stampsTotal = 999.0))
        assertEquals(MedalTier.PLATINUM, top.earnedTier)
        assertNull(top.nextTier)
        assertEquals(1.0, top.progress)
    }

    @Test
    fun decimalCountersCountAsWholeNumbers() {
        assertNull(status("marathon", MedalStatsModel(stampsMaxSameDay = 1.9)).earnedTier)
    }

    @Test
    fun occasionsAreSecretSingleMedals() {
        val pi = status("pi_day", MedalStatsModel())
        assertTrue(pi.secret)
        assertTrue(pi.isSingle)
        assertFalse(pi.isEarned)
        assertEquals(MedalTier.GOLD, status("pi_day", MedalStatsModel(stampsPiDay = 1.0)).earnedTier)
    }

    @Test
    fun flagsAndYearsSinceJoining() {
        assertEquals(MedalTier.GOLD, status("farsighted", MedalStatsModel(renewedAhead = true)).earnedTier)
        val since = Instant.parse("2023-09-01T00:00:00Z")
        assertEquals(MedalTier.GOLD, status("early_adopter", MedalStatsModel(appSince = since)).earnedTier)
        assertNull(status("early_adopter", MedalStatsModel()).earnedTier)
    }

    @Test
    fun theTableHasThirtyMedals() {
        assertEquals(30, MedalRules.all.size)
        assertEquals(MedalRules.all.size, MedalRules.all.map { it.id }.toSet().size)
    }
}
