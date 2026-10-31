package it.mensa.shared.medals

import it.mensa.shared.model.MedalHistoryModel
import it.mensa.shared.time.Season
import kotlin.math.roundToInt

/** How one seasonal medal ended in one closed season. */
data class MedalSeasonResult(
    val season: Int,
    val medalId: String,
    /** The metal won; null when the season closed without one. */
    val tier: MedalTier?,
    /** The counter that decided it. */
    val value: Int,
) {
    val isEarned: Boolean get() = tier != null
    val seasonLabel: String get() = Season.label(season)
}

/** The seasonal medals of one closed season, in the table's order. */
data class MedalHistorySeason(
    val season: Int,
    val results: List<MedalSeasonResult>,
) {
    val label: String get() = Season.label(season)
    val earnedCount: Int get() = results.count { it.isEarned }
}

/**
 * The member's past seasons, read from `medals_history`: one result per
 * seasonal medal per closed season. A secret medal (the occasions) shows in
 * a past season only if it was earned there, as in the cabinet; a medal the
 * app doesn't know, or a permanent one, is left out.
 */
class MedalHistory(
    /** Closed seasons, latest first. */
    val seasons: List<MedalHistorySeason>,
) {
    val isEmpty: Boolean get() = seasons.isEmpty()

    /** One medal's result in every closed season, latest first; seasons it isn't listed in are skipped. */
    fun results(medalId: String): List<MedalSeasonResult> =
        seasons.mapNotNull { season -> season.results.firstOrNull { it.medalId == medalId } }

    companion object {
        val EMPTY = MedalHistory(emptyList())

        /** Builds the history from the member's rows; only seasons before [currentSeason] count. */
        fun build(rows: List<MedalHistoryModel>, currentSeason: Int = Season.current()): MedalHistory {
            val order = MedalRules.all.withIndex().associate { (index, rule) -> rule.id to index }
            val seasons = rows.mapNotNull { row ->
                val rule = MedalRules.rule(row.medal)?.takeIf { it.family == MedalFamily.SEASONAL } ?: return@mapNotNull null
                val season = row.season.roundToInt()
                if (season >= currentSeason) return@mapNotNull null
                val tier = tierOf(row.tier)
                if (tier == null && rule.secret) return@mapNotNull null
                MedalSeasonResult(season, rule.id, tier, row.value.roundToInt())
            }
            return MedalHistory(
                seasons.groupBy { it.season }
                    .toList()
                    .sortedByDescending { it.first }
                    .map { (season, list) ->
                        MedalHistorySeason(season, list.distinctBy { it.medalId }.sortedBy { order[it.medalId] ?: Int.MAX_VALUE })
                    },
            )
        }

        /** "gold" is [MedalTier.GOLD]; "none" or anything else is null. */
        internal fun tierOf(key: String): MedalTier? = MedalTier.entries.firstOrNull { it.name.equals(key, ignoreCase = true) }
    }
}
