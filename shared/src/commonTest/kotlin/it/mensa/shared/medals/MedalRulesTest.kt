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

        val silver = status("collector", MedalStatsModel(stampsTotal = 4.0))
        assertEquals(MedalTier.SILVER, silver.earnedTier)
        assertEquals(MedalTier.GOLD, silver.nextTier)
        assertEquals(6, silver.nextThreshold)
        assertEquals(4.0 / 6.0, silver.progress, 0.0001)

        val top = status("collector", MedalStatsModel(stampsTotal = 99.0))
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
        assertEquals(MedalTier.PLATINUM, status("pi_day", MedalStatsModel(stampsPiDay = 1.0)).earnedTier)
    }

    @Test
    fun flagsAndYearsSinceJoining() {
        assertEquals(MedalTier.PLATINUM, status("up_to_date", MedalStatsModel(calendarLinked = true)).earnedTier)
        val since = Instant.parse("2023-09-01T00:00:00Z")
        assertEquals(MedalTier.GOLD, status("early_adopter", MedalStatsModel(appSince = since)).earnedTier)
        assertNull(status("early_adopter", MedalStatsModel()).earnedTier)
    }

    @Test
    fun everyMedalTopsOutAtPlatinum() {
        MedalRules.all.forEach { assertEquals(MedalTier.PLATINUM, it.levels.last().first, it.id) }
        val single = status("night_owl", MedalStatsModel())
        assertTrue(single.isSingle)
        assertEquals(MedalTier.PLATINUM, single.displayTier)
    }

    @Test
    fun seasonalThresholdsAreTheOnesOfASeason() {
        val expected = mapOf(
            "collector" to listOf(1, 3, 6, 10),
            "steady" to listOf(1, 3, 5, 7),
            "unstoppable" to listOf(2, 3, 4, 6),
            "marathon" to listOf(2, 3, 4, 5),
            "pioneer" to listOf(1, 2, 4, 6),
            "globetrotter" to listOf(2, 3, 4, 5),
            "traveller" to listOf(1, 3, 6, 10),
            "national_rally" to listOf(1, 2, 3, 4),
            "always_there" to listOf(1, 3, 6, 10),
            "organizer" to listOf(1, 3, 8, 15),
        )
        expected.forEach { (id, thresholds) ->
            assertEquals(thresholds, MedalRules.rule(id)!!.levels.map { it.second }, id)
        }
        assertEquals(listOf(4), MedalRules.rule("four_seasons")!!.levels.map { it.second })
    }

    @Test
    fun familiesSplitTheTable() {
        val permanent = setOf("active_member", "early_adopter", "up_to_date", "cartographer", "multiscreen")
        MedalRules.all.forEach {
            assertEquals(if (it.id in permanent) MedalFamily.PERMANENT else MedalFamily.SEASONAL, it.family, it.id)
        }
        assertEquals(permanent, MedalRules.all.filter { it.family == MedalFamily.PERMANENT }.map { it.id }.toSet())
        assertEquals(MedalFamily.SEASONAL, status("collector", MedalStatsModel()).family)
        assertTrue(status("pi_day", MedalStatsModel()).isSeasonal)
        assertFalse(status("up_to_date", MedalStatsModel()).isSeasonal)
    }

    @Test
    fun theTableHasTwentySixMedals() {
        assertEquals(26, MedalRules.all.size)
        assertEquals(MedalRules.all.size, MedalRules.all.map { it.id }.toSet().size)
    }
}
