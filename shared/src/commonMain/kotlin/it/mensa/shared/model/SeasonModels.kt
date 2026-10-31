package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `medals_history` (own rows only): the result of one seasonal
 * medal in one closed season. [medal] is a rule id of `MedalRules`, [tier]
 * one of none/bronze/silver/gold/platinum and [value] the counter that
 * decided it. Numbers come as `12` or `12.0`: read as Double.
 */
@Serializable
data class MedalHistoryModel(
    val id: String = "",
    val user: String = "",
    /** The season, by the year it starts in: 2025 is "2025/26". */
    val season: Double = 0.0,
    val medal: String = "",
    val tier: String = "",
    val value: Double = 0.0,
)

/**
 * One row of `leaderboard_seasons` (the hall of fame): a place in the final
 * ranking of a closed season. The server keeps the top 10 of each board plus
 * the reader's own row. [board] is "members", "regions" or "secretaries";
 * [subject] is the user id (members, secretaries) or the local office id
 * (regions).
 */
@Serializable
data class LeaderboardSeasonModel(
    val id: String = "",
    val board: String = "",
    val season: Double = 0.0,
    val subject: String = "",
    val name: String = "",
    val position: Double = 0.0,
    val score: Double = 0.0,
    @SerialName("region_key") val regionKey: String = "",
    @SerialName("local_office") val localOffice: String = "",
)
