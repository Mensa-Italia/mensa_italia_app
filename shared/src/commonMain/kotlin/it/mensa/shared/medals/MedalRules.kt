package it.mensa.shared.medals

import it.mensa.shared.model.MedalStatsModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

enum class MedalTier { BRONZE, SILVER, GOLD, PLATINUM }

/**
 * How long a medal lasts. [SEASONAL] medals start from zero every 1 October
 * and keep one result per closed season (`medals_history`); [PERMANENT]
 * ones are lifetime recognitions (membership, app, calendar) with no seasons.
 */
enum class MedalFamily { PERMANENT, SEASONAL }

/**
 * A medal: the counter it's earned from and the threshold for each metal.
 * A tiered medal climbs bronze → platinum as its counter grows; a single
 * medal has one level, platinum straight away: every medal tops out there.
 *
 * Secret medals (the occasions: Halloween, New Year…) aren't shown until
 * earned, so finding them is part of the game.
 */
class MedalRule(
    val id: String,
    val family: MedalFamily,
    /** Ascending: each metal with the counter value that earns it. */
    val levels: List<Pair<MedalTier, Int>>,
    val secret: Boolean = false,
    val value: (MedalStatsModel, Instant) -> Int,
)

/** Where the member stands with one medal. */
/** One metal of a medal and the counter value that earns it. */
data class MedalLevel(val tier: MedalTier, val threshold: Int)

data class MedalStatus(
    val id: String,
    val family: MedalFamily,
    /** The counter's current value. */
    val value: Int,
    /** Highest metal earned; null while not earned. */
    val earnedTier: MedalTier?,
    /** Threshold of [earnedTier]. */
    val earnedThreshold: Int?,
    /** Next metal to earn; null once the highest is reached. */
    val nextTier: MedalTier?,
    val nextThreshold: Int?,
    /** 0…1 toward [nextThreshold]; 1 once the highest is reached. */
    val progress: Double,
    val secret: Boolean,
    /** The metal it starts at: what a locked medal is shown in. */
    val firstTier: MedalTier,
    /** One level only, rather than a bronze → platinum climb. */
    val isSingle: Boolean,
    /** Every metal of the medal, ascending, with the value that earns it. */
    val levels: List<MedalLevel>,
) {
    val isEarned: Boolean get() = earnedTier != null

    val isSeasonal: Boolean get() = family == MedalFamily.SEASONAL

    /** The metal to show: the one earned, or the first to aim for. */
    val displayTier: MedalTier get() = earnedTier ?: firstTier
}

/**
 * The medal table, drafted from the `view_medals` counters. The counters'
 * exact meaning is defined by that view on the backend. The seasonal
 * counters are already limited to the current season by the server; the
 * permanent ones are lifetime.
 *
 * The server's `tools/medals/rules.go` mirrors this table (families and
 * thresholds): change both together.
 */
object MedalRules {
    private val B = MedalTier.BRONZE
    private val S = MedalTier.SILVER
    private val G = MedalTier.GOLD
    private val P = MedalTier.PLATINUM

    private fun tiered(
        id: String,
        family: MedalFamily,
        b: Int,
        s: Int,
        g: Int,
        p: Int,
        value: (MedalStatsModel) -> Double,
    ) = MedalRule(id, family, listOf(B to b, S to s, G to g, P to p)) { stats, _ -> value(stats).toInt() }

    private fun seasonal(id: String, b: Int, s: Int, g: Int, p: Int, value: (MedalStatsModel) -> Double) =
        tiered(id, MedalFamily.SEASONAL, b, s, g, p, value)

    private fun permanent(id: String, b: Int, s: Int, g: Int, p: Int, value: (MedalStatsModel) -> Double) =
        tiered(id, MedalFamily.PERMANENT, b, s, g, p, value)

    private fun single(
        id: String,
        family: MedalFamily = MedalFamily.SEASONAL,
        threshold: Int = 1,
        secret: Boolean = false,
        value: (MedalStatsModel) -> Double,
    ) = MedalRule(id, family, listOf(P to threshold), secret) { stats, _ -> value(stats).toInt() }

    private fun flag(id: String, family: MedalFamily = MedalFamily.PERMANENT, value: (MedalStatsModel) -> Boolean) =
        MedalRule(id, family, listOf(P to 1)) { stats, _ -> if (value(stats)) 1 else 0 }

    val all: List<MedalRule> = listOf(
        // Passport and stamps
        seasonal("collector", 1, 3, 6, 10) { it.stampsTotal },
        seasonal("steady", 1, 3, 5, 7) { it.stampsDistinctMonths },
        seasonal("unstoppable", 2, 3, 4, 6) { it.stampsMaxMonthStreak },
        seasonal("marathon", 2, 3, 4, 5) { it.stampsMaxSameDay },
        single("four_seasons", threshold = 4) { it.stampsMaxSeasonsInYear },
        seasonal("pioneer", 1, 2, 4, 6) { it.stampsPioneer },

        // Travel
        seasonal("globetrotter", 2, 3, 4, 5) { it.stampsDistinctRegions },
        seasonal("traveller", 1, 3, 6, 10) { it.stampsOutOfRegion },
        seasonal("national_rally", 1, 2, 3, 4) { it.stampsNational },

        // Times and occasions
        single("night_owl") { it.stampsNight },
        single("early_bird") { it.stampsMorning },
        single("halloween", secret = true) { it.stampsHalloween },
        single("new_year", secret = true) { it.stampsNewYear },
        single("valentine", secret = true) { it.stampsValentine },
        single("pi_day", secret = true) { it.stampsPiDay },
        single("birthday", secret = true) { it.stampsBirthday },
        single("front_row") { it.stampsFirstDay },
        single("better_late") { it.stampsLate },

        // Community
        seasonal("always_there", 1, 3, 6, 10) { it.eventRegistrations },
        seasonal("organizer", 1, 3, 8, 15) { it.eventsCreated },
        single("national_director") { it.eventsCreatedNational },

        // Membership and app: lifetime
        flag("active_member") { it.isMembershipActive },
        MedalRule("early_adopter", MedalFamily.PERMANENT, listOf(B to 1, S to 2, G to 3, P to 5)) { stats, now ->
            if (stats.appSince.toEpochMilliseconds() <= 0) 0
            else ((now - stats.appSince).inWholeDays / 365).toInt()
        },
        flag("up_to_date") { it.calendarLinked },
        permanent("cartographer", 1, 3, 5, 10) { it.calendarRegions },
        single("multiscreen", MedalFamily.PERMANENT, threshold = 2) { it.devices },
    )

    /** The medal's rule by id; null for one this app doesn't know. */
    fun rule(id: String): MedalRule? = all.firstOrNull { it.id == id }

    fun evaluate(stats: MedalStatsModel, now: Instant = Clock.System.now()): List<MedalStatus> =
        all.map { evaluate(it, stats, now) }

    fun evaluate(rule: MedalRule, stats: MedalStatsModel, now: Instant = Clock.System.now()): MedalStatus {
        val value = rule.value(stats, now).coerceAtLeast(0)
        val earned = rule.levels.lastOrNull { value >= it.second }
        val next = rule.levels.firstOrNull { value < it.second }
        return MedalStatus(
            id = rule.id,
            family = rule.family,
            value = value,
            earnedTier = earned?.first,
            earnedThreshold = earned?.second,
            nextTier = next?.first,
            nextThreshold = next?.second,
            progress = next?.let { (value.toDouble() / it.second).coerceIn(0.0, 1.0) } ?: 1.0,
            secret = rule.secret,
            firstTier = rule.levels.first().first,
            isSingle = rule.levels.size == 1,
            levels = rule.levels.map { MedalLevel(it.first, it.second) },
        )
    }
}
