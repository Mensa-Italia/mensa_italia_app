package it.mensa.shared.medals

import it.mensa.shared.model.MedalHistoryModel
import it.mensa.shared.model.MedalStatsModel
import it.mensa.shared.time.Season
import kotlinx.datetime.Instant

/**
 * Made-up counters for capturing the medal screens without signing in
 * (`MENSA_MEDALS_FIXTURE=1` on iOS, extra `mensa_medals_fixture` on
 * Android). Only debug harnesses switch it on; off, the repository reads the
 * server. A mix of earned, half-way and untouched medals, and two closed
 * seasons behind the current one.
 */
object MedalFixtures {

    /** True only when a debug harness turns it on. */
    var enabled: Boolean = false

    internal const val ME_ID: String = "fixture-me"

    internal val stats: MedalStatsModel
        get() = MedalStatsModel(
            id = ME_ID,
            season = Season.current().toDouble(),
            appSince = Instant.parse("2024-03-01T00:00:00Z"),
            isMembershipActive = true,
            stampsTotal = 4.0,
            stampsDistinctMonths = 2.0,
            stampsMaxMonthStreak = 2.0,
            stampsMaxSameDay = 1.0,
            stampsPioneer = 2.0,
            stampsDistinctRegions = 3.0,
            stampsNational = 1.0,
            stampsNight = 1.0,
            stampsPiDay = 1.0,
            eventRegistrations = 2.0,
            devices = 2.0,
            calendarLinked = true,
            calendarRegions = 2.0,
        )

    // Per closed season (latest first): medal id to (metal key, counter). A season
    // of a keener member, then a quieter one; the occasions only where earned.
    private val closed: List<Map<String, Pair<String, Int>>> = listOf(
        mapOf(
            "collector" to ("gold" to 7), "steady" to ("silver" to 4), "unstoppable" to ("bronze" to 2),
            "marathon" to ("none" to 1), "four_seasons" to ("platinum" to 4), "pioneer" to ("silver" to 2),
            "globetrotter" to ("silver" to 3), "traveller" to ("bronze" to 1), "national_rally" to ("none" to 0),
            "night_owl" to ("platinum" to 1), "early_bird" to ("none" to 0), "halloween" to ("platinum" to 1),
            "new_year" to ("none" to 0), "valentine" to ("none" to 0), "pi_day" to ("platinum" to 1),
            "birthday" to ("none" to 0), "front_row" to ("platinum" to 1), "better_late" to ("none" to 0),
            "always_there" to ("bronze" to 1), "organizer" to ("none" to 0), "national_director" to ("none" to 0),
        ),
        mapOf(
            "collector" to ("silver" to 3), "steady" to ("bronze" to 2), "unstoppable" to ("none" to 1),
            "marathon" to ("bronze" to 2), "four_seasons" to ("none" to 2), "pioneer" to ("bronze" to 1),
            "globetrotter" to ("none" to 1), "traveller" to ("none" to 0), "national_rally" to ("bronze" to 1),
            "night_owl" to ("none" to 0), "early_bird" to ("platinum" to 1), "halloween" to ("none" to 0),
            "new_year" to ("platinum" to 1), "valentine" to ("none" to 0), "pi_day" to ("none" to 0),
            "birthday" to ("none" to 0), "front_row" to ("none" to 0), "better_late" to ("platinum" to 1),
            "always_there" to ("none" to 0), "organizer" to ("none" to 0), "national_director" to ("none" to 0),
        ),
    )

    /** The member's two closed seasons before [current], as `medals_history` rows. */
    internal fun history(current: Int = Season.current()): List<MedalHistoryModel> =
        closed.flatMapIndexed { index, medals ->
            val season = current - 1 - index
            medals.map { (medal, result) ->
                MedalHistoryModel(
                    id = "fixture-$season-$medal",
                    user = ME_ID,
                    season = season.toDouble(),
                    medal = medal,
                    tier = result.first,
                    value = result.second.toDouble(),
                )
            }
        }
}
