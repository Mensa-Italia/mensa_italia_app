package it.mensa.shared.leaderboard

import it.mensa.shared.config.FeatureFlags

/** The three rankings the leaderboard page can show, in the order of its switch. */
enum class LeaderboardKind { MEMBERS, REGIONS, SECRETARIES }

/**
 * Which rankings the association has switched on, from `configs`
 * (`leaderboard_members_enabled`, `leaderboard_regions_enabled`,
 * `leaderboard_secretaries_enabled`). Empty means no leaderboard at all: no
 * row in Today, no page. The debug fixture shows all three, so screenshots
 * don't depend on the server.
 */
fun FeatureFlags.enabledLeaderboards(): List<LeaderboardKind> {
    if (LeaderboardFixtures.enabled) return LeaderboardKind.entries.toList()
    return listOfNotNull(
        LeaderboardKind.MEMBERS.takeIf { leaderboardMembersEnabled },
        LeaderboardKind.REGIONS.takeIf { leaderboardRegionsEnabled },
        LeaderboardKind.SECRETARIES.takeIf { leaderboardSecretariesEnabled },
    )
}
