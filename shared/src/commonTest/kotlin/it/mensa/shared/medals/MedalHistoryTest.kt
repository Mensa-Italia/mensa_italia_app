package it.mensa.shared.medals

import it.mensa.shared.model.MedalHistoryModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MedalHistoryTest {

    private fun row(season: Int, medal: String, tier: String, value: Int = 0) =
        MedalHistoryModel(user = "me", season = season.toDouble(), medal = medal, tier = tier, value = value.toDouble())

    @Test
    fun seasonsComeLatestFirstAndMedalsInTheTablesOrder() {
        val history = MedalHistory.build(
            listOf(
                row(2024, "organizer", "bronze", 1),
                row(2025, "steady", "silver", 4),
                row(2025, "collector", "gold", 7),
                row(2024, "collector", "none", 0),
            ),
            currentSeason = 2026,
        )
        assertEquals(listOf(2025, 2024), history.seasons.map { it.season })
        assertEquals(listOf("collector", "steady"), history.seasons[0].results.map { it.medalId })
        assertEquals(listOf("collector", "organizer"), history.seasons[1].results.map { it.medalId })
        assertEquals("2025/26", history.seasons[0].label)
        assertEquals(2, history.seasons[0].earnedCount)
        assertEquals(1, history.seasons[1].earnedCount)
    }

    @Test
    fun noMetalIsNotEarned() {
        val history = MedalHistory.build(listOf(row(2025, "marathon", "none", 1)), currentSeason = 2026)
        val result = history.seasons.single().results.single()
        assertNull(result.tier)
        assertEquals(1, result.value)
    }

    @Test
    fun aSecretMedalShowsOnlyIfItWasEarned() {
        val history = MedalHistory.build(
            listOf(row(2025, "halloween", "none"), row(2025, "pi_day", "platinum", 1)),
            currentSeason = 2026,
        )
        assertEquals(listOf("pi_day"), history.seasons.single().results.map { it.medalId })
    }

    @Test
    fun unknownPermanentAndCurrentSeasonRowsAreLeftOut() {
        val history = MedalHistory.build(
            listOf(
                row(2025, "no_such_medal", "gold"),
                row(2025, "up_to_date", "platinum"),
                row(2026, "collector", "gold"),
            ),
            currentSeason = 2026,
        )
        assertTrue(history.isEmpty)
    }

    @Test
    fun aMedalsResultsRunAcrossTheSeasons() {
        val history = MedalHistory.build(
            listOf(row(2025, "collector", "silver", 3), row(2024, "collector", "none"), row(2025, "steady", "bronze")),
            currentSeason = 2026,
        )
        assertEquals(listOf(2025, 2024), history.results("collector").map { it.season })
        assertEquals(listOf(MedalTier.SILVER, null), history.results("collector").map { it.tier })
        assertEquals(emptyList(), history.results("organizer"))
    }

    @Test
    fun tierKeysReadCaseInsensitively() {
        assertEquals(MedalTier.PLATINUM, MedalHistory.tierOf("platinum"))
        assertEquals(MedalTier.GOLD, MedalHistory.tierOf("GOLD"))
        assertNull(MedalHistory.tierOf("none"))
        assertNull(MedalHistory.tierOf(""))
    }

    @Test
    fun theFixtureHasTwoClosedSeasons() {
        val history = MedalHistory.build(MedalFixtures.history(2026), currentSeason = 2026)
        assertEquals(listOf(2025, 2024), history.seasons.map { it.season })
        // Occasions not earned in a season are hidden; the earned ones stay.
        val ids = history.seasons[0].results.map { it.medalId }
        assertTrue("halloween" in ids)
        assertTrue("new_year" !in ids)
        assertTrue(history.seasons.all { s -> s.results.all { MedalRules.rule(it.medalId)!!.family == MedalFamily.SEASONAL } })
    }
}
