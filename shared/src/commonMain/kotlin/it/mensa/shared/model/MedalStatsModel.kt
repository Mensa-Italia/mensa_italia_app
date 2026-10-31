@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * One row of `view_medals`: the
 * counters medals are earned from, for the signed-in member. The view has a
 * row per user, keyed by the user's id. The seasonal medals' counters are
 * limited to [season] (the current one) by the server; the permanent ones
 * are lifetime.
 *
 * Counters are declared as numbers in PocketBase's schema, so they may come
 * as `12` or `12.0`: read as Double, used as whole numbers.
 */
@Serializable
data class MedalStatsModel(
    val id: String = "",
    /** The season the seasonal counters are for, by the year it starts in; 0 from an older view. */
    val season: Double = 0.0,
    @SerialName("app_since") val appSince: Instant = Instant.fromEpochMilliseconds(0),
    @SerialName("expire_membership") val expireMembership: Instant = Instant.fromEpochMilliseconds(0),
    @SerialName("is_membership_active") val isMembershipActive: Boolean = false,

    @SerialName("stamps_total") val stampsTotal: Double = 0.0,
    @SerialName("stamps_distinct_months") val stampsDistinctMonths: Double = 0.0,
    @SerialName("stamps_night") val stampsNight: Double = 0.0,
    @SerialName("stamps_morning") val stampsMorning: Double = 0.0,
    @SerialName("stamps_halloween") val stampsHalloween: Double = 0.0,
    @SerialName("stamps_new_year") val stampsNewYear: Double = 0.0,
    @SerialName("stamps_valentine") val stampsValentine: Double = 0.0,
    @SerialName("stamps_pi_day") val stampsPiDay: Double = 0.0,
    @SerialName("stamps_birthday") val stampsBirthday: Double = 0.0,
    @SerialName("stamps_first_day") val stampsFirstDay: Double = 0.0,
    @SerialName("stamps_late") val stampsLate: Double = 0.0,
    @SerialName("stamps_max_same_day") val stampsMaxSameDay: Double = 0.0,
    @SerialName("stamps_max_month_streak") val stampsMaxMonthStreak: Double = 0.0,
    @SerialName("stamps_max_seasons_in_year") val stampsMaxSeasonsInYear: Double = 0.0,
    @SerialName("stamps_pioneer") val stampsPioneer: Double = 0.0,
    @SerialName("stamps_distinct_regions") val stampsDistinctRegions: Double = 0.0,
    @SerialName("stamps_national") val stampsNational: Double = 0.0,
    @SerialName("stamps_out_of_region") val stampsOutOfRegion: Double = 0.0,

    @SerialName("events_created") val eventsCreated: Double = 0.0,
    @SerialName("events_created_national") val eventsCreatedNational: Double = 0.0,
    @SerialName("event_registrations") val eventRegistrations: Double = 0.0,
    @SerialName("devices") val devices: Double = 0.0,
    @SerialName("calendar_linked") val calendarLinked: Boolean = false,
    @SerialName("calendar_regions") val calendarRegions: Double = 0.0,
)
