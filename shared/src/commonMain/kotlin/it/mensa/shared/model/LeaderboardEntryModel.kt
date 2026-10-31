package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `view_leaderboard`: a member's place in the ranking and the
 * points behind it. The view computes everything (stamps weighted by how
 * many members took them, medals, events organised, all inside the season
 * from 1 October to 30 September); the app only shows it. Only members with
 * at least one valid stamp in the season have a row.
 *
 * Numbers are declared as numbers in PocketBase's schema, so they may come
 * as `12` or `12.0`: read as Double, shown as whole numbers.
 * `events_points` is optional: an older view without it reads as zero.
 */
@Serializable
data class LeaderboardEntryModel(
    val id: String = "",
    /** The season the row is for, by the year it starts in; 0 from an older view. */
    val season: Double = 0.0,
    val name: String = "",
    val position: Double = 0.0,
    val score: Double = 0.0,
    @SerialName("stamps_points") val stampsPoints: Double = 0.0,
    @SerialName("medal_points") val medalPoints: Double = 0.0,
    @SerialName("events_points") val eventsPoints: Double = 0.0,
    @SerialName("stamps_total") val stampsTotal: Double = 0.0,
    @SerialName("stamps_distinct_regions") val stampsDistinctRegions: Double = 0.0,
    /** Position at the last daily snapshot; 0 when there is none yet (table filled by the server's cron). */
    @SerialName("previous_position") val previousPosition: Double = 0.0,
    @SerialName("previous_score") val previousScore: Double = 0.0,
)
