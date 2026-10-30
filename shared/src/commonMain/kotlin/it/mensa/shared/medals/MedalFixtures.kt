package it.mensa.shared.medals

import it.mensa.shared.model.MedalStatsModel
import kotlinx.datetime.Instant

/**
 * Made-up counters for capturing the medal screens without signing in
 * (`MENSA_MEDALS_FIXTURE=1` on iOS, extra `mensa_medals_fixture` on
 * Android). Only debug harnesses switch it on; off, the repository reads the
 * server. A mix of earned, half-way and untouched medals.
 */
object MedalFixtures {

    /** True only when a debug harness turns it on. */
    var enabled: Boolean = false

    internal const val ME_ID: String = "fixture-me"

    internal val stats: MedalStatsModel
        get() = MedalStatsModel(
            id = ME_ID,
            appSince = Instant.parse("2024-03-01T00:00:00Z"),
            isMembershipActive = true,
            stampsTotal = 42.0,
            stampsDistinctMonths = 9.0,
            stampsMaxMonthStreak = 4.0,
            stampsMaxSameDay = 2.0,
            stampsPioneer = 3.0,
            stampsDistinctRegions = 5.0,
            stampsNational = 1.0,
            stampsNight = 1.0,
            stampsPiDay = 1.0,
            eventRegistrations = 12.0,
            devices = 2.0,
            calendarLinked = true,
            calendarRegions = 2.0,
        )
}
