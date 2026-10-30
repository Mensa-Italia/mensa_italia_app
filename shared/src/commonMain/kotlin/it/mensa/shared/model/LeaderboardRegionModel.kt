package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `view_leaderboard_regioni`: a local office's place in the
 * regions' ranking. The view computes everything (the region's events over
 * the last 12 months weighted by the share of members present, times how
 * wide the community that attends is, plus the national events it hosted);
 * the app only shows it. Numbers come as `12` or `12.0`: read as Double.
 */
@Serializable
data class LeaderboardRegionModel(
    val id: String = "",
    val name: String = "",
    val region: String = "",
    @SerialName("region_key") val regionKey: String = "",
    val position: Double = 0.0,
    val score: Double = 0.0,
    val participation: Double = 0.0,
    @SerialName("community_pct") val communityPct: Double = 0.0,
    @SerialName("national_points") val nationalPoints: Double = 0.0,
    @SerialName("national_events") val nationalEvents: Double = 0.0,
    @SerialName("effective_attendees") val effectiveAttendees: Double = 0.0,
    @SerialName("distinct_attendees") val distinctAttendees: Double = 0.0,
    @SerialName("core_pct") val corePct: Double = 0.0,
    @SerialName("reach_pct") val reachPct: Double = 0.0,
    @SerialName("events_total") val eventsTotal: Double = 0.0,
    @SerialName("events_scoring") val eventsScoring: Double = 0.0,
    @SerialName("attendance_total") val attendanceTotal: Double = 0.0,
    @SerialName("members_active") val membersActive: Double = 0.0,
    @SerialName("avg_share_pct") val avgSharePct: Double = 0.0,
    /** Position at the last daily snapshot; 0 when there is none yet. */
    @SerialName("previous_position") val previousPosition: Double = 0.0,
    @SerialName("previous_score") val previousScore: Double = 0.0,
)

/**
 * One row of `view_leaderboard_segretari`: a regional secretary, who scores
 * what their region scores. `id` is the member's user id, so the directory
 * photo can be shown; `local_office` points at the region's row.
 */
@Serializable
data class LeaderboardSecretaryModel(
    val id: String = "",
    val name: String = "",
    @SerialName("local_office") val localOffice: String = "",
    val region: String = "",
    val position: Double = 0.0,
    val score: Double = 0.0,
    val participation: Double = 0.0,
    @SerialName("community_pct") val communityPct: Double = 0.0,
    @SerialName("national_points") val nationalPoints: Double = 0.0,
    @SerialName("national_events") val nationalEvents: Double = 0.0,
    @SerialName("effective_attendees") val effectiveAttendees: Double = 0.0,
    @SerialName("distinct_attendees") val distinctAttendees: Double = 0.0,
    @SerialName("core_pct") val corePct: Double = 0.0,
    @SerialName("reach_pct") val reachPct: Double = 0.0,
    @SerialName("events_total") val eventsTotal: Double = 0.0,
    @SerialName("events_scoring") val eventsScoring: Double = 0.0,
    @SerialName("attendance_total") val attendanceTotal: Double = 0.0,
    @SerialName("members_active") val membersActive: Double = 0.0,
    @SerialName("avg_share_pct") val avgSharePct: Double = 0.0,
    /** Position at the last daily snapshot; 0 when there is none yet. */
    @SerialName("previous_position") val previousPosition: Double = 0.0,
    @SerialName("previous_score") val previousScore: Double = 0.0,
)
