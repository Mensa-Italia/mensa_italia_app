package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One row of `local_office_stats` (id = the local office's id): the office's
 * figures as the server materializes them every 15 minutes. Readable by every
 * signed-in member. [season] is the label, e.g. "2026/27".
 */
@Serializable
data class LocalOfficeStatsModel(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    @SerialName("region_key") val regionKey: String = "",
    /** Active members registered in the region now. */
    @SerialName("members_active") val membersActive: Int = 0,
    /** Members who joined (or moved in) in the last 7 days. */
    @SerialName("members_new_7d") val membersNew7d: Int = 0,
    @SerialName("members_new_30d") val membersNew30d: Int = 0,
    @SerialName("members_left_30d") val membersLeft30d: Int = 0,
    /** Events of the region in the current season. */
    @SerialName("events_season") val eventsSeason: Int = 0,
    @SerialName("events_upcoming") val eventsUpcoming: Int = 0,
    /** Stamps at the region's events in the current season. */
    @SerialName("attendance_season") val attendanceSeason: Int = 0,
    @SerialName("deals_active") val dealsActive: Int = 0,
    val season: String = "",
    /** When the server computed the row, as PocketBase writes it. */
    @SerialName("computed_at") val computedAt: String = "",
)

/**
 * One row of `local_office_stats_monthly` (id = `<officeId>_<YYYY-MM>`): one
 * month of an office. Readable only by the office's admins and by super.
 */
@Serializable
data class LocalOfficeMonthlyStatsModel(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    @SerialName("region_key") val regionKey: String = "",
    /** "YYYY-MM". */
    val month: String = "",
    val season: String = "",
    val events: Int = 0,
    /** Stamps at the month's events. */
    val attendance: Int = 0,
    @SerialName("distinct_attendees") val distinctAttendees: Int = 0,
    @SerialName("members_new") val membersNew: Int = 0,
    @SerialName("members_left") val membersLeft: Int = 0,
    /** Active members at the end of the month. */
    @SerialName("members_active") val membersActive: Int = 0,
) {
    /** The year of [month], 0 when unreadable. */
    val year: Int get() = month.substringBefore('-').toIntOrNull() ?: 0

    /** The month number of [month], 1 to 12; 0 when unreadable. */
    val monthNumber: Int get() = month.substringAfter('-', "").take(2).toIntOrNull() ?: 0
}

/** What happened to a member in a region. */
enum class MemberChangeKind(val wire: String) {
    /** A new active member. */
    JOINED("joined"),

    /** No longer active. */
    LEFT("left"),

    /** Moved into the region from another one. */
    MOVED_IN("moved_in"),

    /** Moved from the region to another one. */
    MOVED_OUT("moved_out"),
    ;

    /** True for the movements that bring a member into the region. */
    val isArrival: Boolean get() = this == JOINED || this == MOVED_IN

    companion object {
        /** The kind for a wire value; null when the server sends one the app doesn't know. */
        fun of(wire: String): MemberChangeKind? = entries.firstOrNull { it.wire == wire }
    }
}

/**
 * One row of `members_registry_changes`: a member who joined, left, or moved
 * in or out of a region on [day]. Readable only by the office's admins and by
 * super.
 */
@Serializable
data class MemberChangeModel(
    val id: String = "",
    /** The `members_registry` id. */
    val member: String = "",
    val name: String = "",
    @SerialName("region_key") val regionKey: String = "",
    @SerialName("local_office") val localOffice: String = "",
    /** The raw kind, see [changeKind]. */
    val kind: String = "",
    /** The day, as PocketBase writes it ("2026-11-03 00:00:00.000Z"). */
    val day: String = "",
) {
    /** The parsed [kind]; null for a value the app doesn't know. */
    val changeKind: MemberChangeKind? get() = MemberChangeKind.of(kind)

    /** True for a member who joined or moved in. */
    val isArrival: Boolean get() = changeKind?.isArrival == true

    /** "YYYY-MM-DD", the date part of [day]. */
    val dayIso: String get() = day.take(DATE_LENGTH)

    private companion object {
        const val DATE_LENGTH = 10
    }
}
