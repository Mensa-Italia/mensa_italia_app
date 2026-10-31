package it.mensa.shared.leaderboard

import it.mensa.shared.model.LeaderboardSeasonModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HallOfFameTest {

    private fun row(board: String, season: Int, position: Int, subject: String, score: Int = 100 - position) =
        LeaderboardSeasonModel(
            id = "$season-$board-$position", board = board, season = season.toDouble(), subject = subject,
            name = "SOCIO $subject", position = position.toDouble(), score = score.toDouble(),
        )

    @Test
    fun seasonsComeLatestFirstWithTheFirstThreePlaces() {
        val rows = (1..10).map { row("members", 2025, it, "m$it") } +
            (1..4).map { row("members", 2024, it, "o$it") } +
            listOf(row("regions", 2025, 1, "office-a"))
        val hall = HallOfFame.build(rows, "m2", emptyMap())
        assertEquals(listOf(2025, 2024), hall.seasons.map { it.season })
        assertEquals(2025, hall.latest?.season)
        assertEquals("2025/26", hall.latest?.label)
        assertEquals(listOf("m1", "m2", "m3"), hall.latest!!.members.podium.map { it.model.subject })
        assertEquals(listOf("o1", "o2", "o3"), hall.seasons[1].members.podium.map { it.model.subject })
        assertTrue(hall.seasons[1].regions.isEmpty)
        assertEquals(1, hall.latest!!.board(LeaderboardKind.REGIONS).podium.size)
    }

    @Test
    fun theOwnRowIsKeptApartOnlyBelowThePodium() {
        val rows = (1..3).map { row("members", 2025, it, "m$it") } + row("members", 2025, 17, "me")
        val below = HallOfFame.build(rows, "me", emptyMap()).latest!!.members
        assertEquals(17, below.me?.position)
        assertTrue(below.me!!.isMe)

        val onPodium = HallOfFame.build(rows, "m2", emptyMap()).latest!!.members
        assertNull(onPodium.me)
        assertTrue(onPodium.podium[1].isMe)

        assertNull(HallOfFame.build(rows, "someone", emptyMap()).latest!!.members.me)
    }

    @Test
    fun tiesOnThePodiumAreAllShown() {
        val rows = listOf(
            row("secretaries", 2025, 1, "a", 90), row("secretaries", 2025, 2, "b", 80),
            row("secretaries", 2025, 2, "c", 80), row("secretaries", 2025, 4, "d", 70),
        )
        assertEquals(listOf("a", "b", "c"), HallOfFame.build(rows, null, emptyMap()).latest!!.secretaries.podium.map { it.model.subject })
    }

    @Test
    fun anUnknownBoardOrSeasonAddsNothing() {
        val hall = HallOfFame.build(listOf(row("teams", 2025, 1, "x"), row("members", 0, 1, "y")), null, emptyMap())
        assertTrue(hall.isEmpty)
    }

    @Test
    fun namesAreTitleCased() {
        val hall = HallOfFame.build(listOf(row("members", 2025, 1, "X")), null, emptyMap())
        assertEquals("Socio X", hall.latest!!.members.podium.single().displayName)
        assertEquals("SX", hall.latest!!.members.podium.single().initials)
    }

    @Test
    fun theFixtureHasTwoSeasonsAndTheMembersOwnRow() {
        val hall = HallOfFame.build(LeaderboardFixtures.seasons(2026), LeaderboardFixtures.ME_ID, emptyMap())
        assertEquals(listOf(2025, 2024), hall.seasons.map { it.season })
        assertEquals(3, hall.latest!!.members.podium.size)
        assertEquals(3, hall.latest!!.regions.podium.size)
        assertEquals(3, hall.latest!!.secretaries.podium.size)
        assertEquals(23, hall.latest!!.members.me?.position)
        assertNull(hall.seasons[1].members.me)
    }
}
