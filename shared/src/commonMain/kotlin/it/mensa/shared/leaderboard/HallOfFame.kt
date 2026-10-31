package it.mensa.shared.leaderboard

import it.mensa.shared.model.LeaderboardSeasonModel
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.time.Season
import kotlin.math.roundToInt

/** A place in the final ranking of a closed season. */
data class HallEntry(
    val model: LeaderboardSeasonModel,
    /** The member's directory record, for the photo; null for a region or an uncached member. */
    val member: RegSociModel?,
    val isMe: Boolean,
) {
    val id: String get() = model.id
    val position: Int get() = model.position.roundToInt()
    val score: Int get() = model.score.roundToInt()

    /** The name as written in the directory when it's a member, or as the season recorded it. */
    val displayName: String get() = titleCase(member?.name?.takeIf { it.isNotBlank() } ?: model.name)

    /** Up to two initials, for when there is no photo. */
    val initials: String
        get() = displayName.split(" ").filter { it.isNotBlank() }.take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "?" }
}

/** One board of one closed season: the first three places and, apart, the member's own row. */
data class HallBoard(
    val kind: LeaderboardKind,
    /** Places 1 to 3, ties included, best first. */
    val podium: List<HallEntry>,
    /** The member's own row when it falls outside [podium]; null otherwise. */
    val me: HallEntry?,
) {
    val isEmpty: Boolean get() = podium.isEmpty()
}

/** A closed season in the hall of fame. */
data class HallSeason(
    val season: Int,
    val members: HallBoard,
    val regions: HallBoard,
    val secretaries: HallBoard,
) {
    /** "2025/26". */
    val label: String get() = Season.label(season)

    fun board(kind: LeaderboardKind): HallBoard = when (kind) {
        LeaderboardKind.MEMBERS -> members
        LeaderboardKind.REGIONS -> regions
        LeaderboardKind.SECRETARIES -> secretaries
    }
}

/**
 * The hall of fame (albo d'oro), read from `leaderboard_seasons`: the closed
 * seasons, latest first, each with the podium of the members', regions' and
 * secretaries' rankings. The server sends the top 10 of each board plus the
 * reader's own row; the app shows the first three places and the own row.
 */
data class HallOfFame(
    /** Closed seasons, latest first. */
    val seasons: List<HallSeason>,
) {
    val isEmpty: Boolean get() = seasons.isEmpty()

    /** The latest closed season; null before the first one closes. */
    val latest: HallSeason? get() = seasons.firstOrNull()

    companion object {
        val EMPTY = HallOfFame(emptyList())

        /** Names of the boards in `leaderboard_seasons.board`. */
        internal fun kindOf(board: String): LeaderboardKind? = when (board) {
            "members" -> LeaderboardKind.MEMBERS
            "regions" -> LeaderboardKind.REGIONS
            "secretaries" -> LeaderboardKind.SECRETARIES
            else -> null
        }

        fun build(
            rows: List<LeaderboardSeasonModel>,
            meId: String?,
            members: Map<String, RegSociModel>,
        ): HallOfFame {
            val seasons = rows.groupBy { it.season.roundToInt() }
                .filterKeys { it > 0 }
                .toList()
                .sortedByDescending { it.first }
                .map { (season, seasonRows) ->
                    fun board(kind: LeaderboardKind): HallBoard {
                        val entries = seasonRows
                            .filter { kindOf(it.board) == kind }
                            .map {
                                val person = kind != LeaderboardKind.REGIONS
                                HallEntry(it, if (person) members[it.subject] else null, isMe = person && meId != null && it.subject == meId)
                            }
                            .sortedWith(compareBy<HallEntry> { it.position }.thenByDescending { it.score }.thenBy { it.model.name })
                        val podium = entries.filter { it.position in 1..PODIUM_SIZE }
                        return HallBoard(kind, podium, entries.firstOrNull { it.isMe && it !in podium })
                    }
                    HallSeason(
                        season = season,
                        members = board(LeaderboardKind.MEMBERS),
                        regions = board(LeaderboardKind.REGIONS),
                        secretaries = board(LeaderboardKind.SECRETARIES),
                    )
                }
                .filter { !it.members.isEmpty || !it.regions.isEmpty || !it.secretaries.isEmpty }
            return HallOfFame(seasons)
        }

        private const val PODIUM_SIZE = 3
    }
}
