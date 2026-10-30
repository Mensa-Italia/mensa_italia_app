package it.mensa.shared.leaderboard

import it.mensa.shared.model.LeaderboardEntryModel
import it.mensa.shared.model.RegSociModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LeaderboardBoardTest {

    private fun entry(id: String, position: Int, score: Double, name: String = "SOCIO $id") =
        LeaderboardEntryModel(id = id, name = name, position = position.toDouble(), score = score)

    @Test
    fun rowsComeBestFirstAndTheMemberIsFound() {
        val board = LeaderboardRepository.build(
            entries = listOf(entry("c", 3, 50.0), entry("a", 1, 90.0), entry("b", 2, 70.0)),
            meId = "b",
            members = emptyMap(),
        )
        assertEquals(listOf("a", "b", "c"), board.rows.map { it.id })
        assertEquals("b", board.me?.id)
        assertTrue(board.rows.single { it.id == "b" }.isMe)
    }

    @Test
    fun theGapIsToTheClosestHigherScoreRoundedUp() {
        val board = LeaderboardRepository.build(
            entries = listOf(entry("a", 1, 90.0), entry("b", 2, 70.2), entry("me", 3, 61.5)),
            meId = "me",
            members = emptyMap(),
        )
        assertEquals(9, board.pointsToNext)
        assertEquals(2, board.nextPosition)
    }

    @Test
    fun aTieAboveIsSkippedBecausePassingItGainsNoPlace() {
        val board = LeaderboardRepository.build(
            entries = listOf(entry("a", 1, 80.0), entry("b", 2, 60.0), entry("me", 2, 60.0)),
            meId = "me",
            members = emptyMap(),
        )
        assertEquals(20, board.pointsToNext)
        assertEquals(1, board.nextPosition)
    }

    @Test
    fun theLeaderHasNoGapAndAnUnrankedMemberNoRow() {
        val top = LeaderboardRepository.build(listOf(entry("me", 1, 90.0)), "me", emptyMap())
        assertNull(top.pointsToNext)
        assertEquals(1, top.myPodiumPlace)
        val absent = LeaderboardRepository.build(listOf(entry("a", 1, 90.0)), "me", emptyMap())
        assertNull(absent.me)
        assertNull(absent.myPodiumPlace)
    }

    @Test
    fun onlyTheFirstTenAreShownAndTheMemberBelowStandsApart() {
        val entries = (1..14).map { entry("m$it", it, 100.0 - it) } + entry("x", 10, 90.0)
        val board = LeaderboardRepository.build(entries, "m13", emptyMap())
        // The tie at 10th stays in.
        assertEquals(11, board.top.size)
        assertTrue(board.top.all { it.position <= 10 })
        assertEquals("m13", board.meBelowTop?.id)
        assertNull(board.myPodiumPlace)
        val inside = LeaderboardRepository.build(entries, "m4", emptyMap())
        assertNull(inside.meBelowTop)
    }

    @Test
    fun namesPreferTheDirectoryAndAreTitleCased() {
        val board = LeaderboardRepository.build(
            entries = listOf(entry("a", 1, 10.0, name = "MARIO ROSSI"), entry("b", 2, 5.0, name = "anna de luca")),
            meId = "a",
            members = mapOf("b" to RegSociModel(id = "b", name = "Anna De Luca")),
        )
        assertEquals("Mario Rossi", board.rows[0].displayName)
        assertEquals("MR", board.rows[0].initials)
        assertEquals("Anna De Luca", board.rows[1].displayName)
    }
}
