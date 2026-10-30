package it.mensa.shared.leaderboard

import it.mensa.shared.model.LeaderboardEntryModel
import it.mensa.shared.model.LeaderboardRegionModel
import it.mensa.shared.model.LeaderboardSecretaryModel
import kotlin.math.round

/**
 * A made-up ranking for capturing the leaderboard screens without signing in
 * (`MENSA_LEADERBOARD_FIXTURE=1` on iOS, extra `mensa_leaderboard_fixture` on
 * Android). Only debug harnesses switch it on; off, the repository reads the
 * server. Every name here is invented, so the screenshots expose nobody.
 */
object LeaderboardFixtures {

    /** True only when a debug harness turns it on. */
    var enabled: Boolean = false

    /** The member the fixture pretends is signed in. */
    const val ME_ID: String = "fixture-me"

    /** Where that member stands, 1 to 40: 1 to 3 to see the podium stamp. */
    var mePlace: Int = 17

    private val names = listOf(
        "Chiara Valenti", "Lorenzo Gatti", "Sofia Marchetti", "Tommaso Ferri", "Giulia Rinaldi",
        "Matteo Bellini", "Aurora Costa", "Edoardo Fontana", "Beatrice Leone", "Davide Moretti",
        "Martina Galli", "Riccardo Conti", "Elena Barbieri", "Francesco Testa", "Alice Serra",
        "Pietro Longo", "Giorgia Bianchi", "Nicolò Ferrara", "Camilla Rizzi", "Andrea Pellegrini",
        "Ginevra Caruso", "Leonardo Mancini", "Vittoria Greco", "Gabriele Marino", "Anna Lombardi",
        "Simone Villa", "Ludovica Sala", "Alessandro Farina", "Rebecca Ricci", "Federico Neri",
        "Irene Colombo", "Samuele De Luca", "Noemi Fabbri", "Jacopo Russo", "Bianca Morelli",
        "Emanuele Silvestri", "Arianna Cattaneo", "Michele Orlando", "Greta Monti", "Luca Bruno",
    )

    val entries: List<LeaderboardEntryModel> get() = names.mapIndexed { index, name ->
        val score = (264 - index * 6.3 - (index % 3) * 1.7).coerceAtLeast(12.0)
        val events = if (index % 7 == 3) 24.0 - index / 3 else 0.0
        val medals = (score * 0.38).coerceAtMost(110.0)
        LeaderboardEntryModel(
            id = if (index == mePlace - 1) ME_ID else "fixture-$index",
            name = name,
            position = (index + 1).toDouble(),
            score = score,
            stampsPoints = score - medals - events,
            medalPoints = medals,
            eventsPoints = events,
            stampsTotal = (34 - index * 0.7).coerceAtLeast(2.0),
            stampsDistinctRegions = (6 - index / 8).coerceAtLeast(1).toDouble(),
        )
    }

    // Regions and their secretaries: the same made-up numbers seen from two sides.
    private data class RegionSeed(
        val region: String, val members: Int, val participation: Int, val community: Int,
        val national: Int, val events: Int, val attendance: Int, val distinct: Int, val effective: Int,
    )

    // Invented seats with invented numbers: none of these is a real region or a
    // real figure, the spread (a small active seat, a large quiet one, a seat
    // with only a national, empty ones) is what the screens need to show.
    private val regionSeeds = listOf(
        RegionSeed("Valle Chiara", 60, 18, 100, 0, 16, 70, 22, 15),
        RegionSeed("Pianura Grande", 600, 8, 90, 0, 24, 240, 100, 50),
        RegionSeed("Isola Maggiore", 80, 0, 0, 8, 0, 0, 0, 0),
        RegionSeed("Riviera", 90, 0, 0, 7, 4, 5, 1, 1),
        RegionSeed("Terre Alte", 250, 5, 100, 0, 12, 55, 45, 40),
        RegionSeed("Laguna", 300, 6, 90, 0, 20, 115, 55, 30),
        RegionSeed("Golfo", 130, 0, 0, 3, 0, 0, 0, 0),
        RegionSeed("Colline", 65, 3, 70, 0, 4, 20, 11, 8),
        RegionSeed("Altopiano", 220, 2, 30, 0, 40, 150, 45, 12),
        RegionSeed("Costa Verde", 70, 1, 50, 0, 3, 11, 9, 7),
        RegionSeed("Vallata", 180, 0, 0, 0, 1, 1, 1, 1),
        RegionSeed("Piana", 310, 0, 0, 0, 0, 0, 0, 0),
        RegionSeed("Promontorio", 20, 0, 0, 0, 0, 0, 0, 0),
    )

    // Rounded like the view does, so ties share a place.
    private fun scoreOf(s: RegionSeed): Double = round(s.participation * s.community / 100.0 + s.national)

    // Like the view: the position comes from the points alone, dense, so the
    // three medals always exist and equal points share a place.
    private val ranked: List<Pair<RegionSeed, Int>>
        get() {
            val sorted = regionSeeds.sortedWith(
                compareByDescending<RegionSeed> { scoreOf(it) }.thenByDescending { it.participation }
                    .thenByDescending { it.events }.thenBy { it.region },
            )
            val scores = sorted.map { scoreOf(it) }.distinct()
            return sorted.map { s -> s to (scores.indexOf(scoreOf(s)) + 1) }
        }

    val regions: List<LeaderboardRegionModel> get() = ranked.map { (s, position) ->
        LeaderboardRegionModel(
            id = "fixture-office-${s.region}", name = s.region, region = s.region, regionKey = s.region,
            position = position.toDouble(), score = scoreOf(s), participation = s.participation.toDouble(),
            communityPct = s.community.toDouble(), nationalPoints = s.national.toDouble(),
            nationalEvents = if (s.national > 0) 1.0 else 0.0,
            effectiveAttendees = s.effective.toDouble(), distinctAttendees = s.distinct.toDouble(),
            corePct = 100.0 * s.effective / s.members, reachPct = 100.0 * s.distinct / s.members,
            eventsTotal = s.events.toDouble(), eventsScoring = (s.events * 0.6).toDouble(),
            attendanceTotal = s.attendance.toDouble(), membersActive = s.members.toDouble(),
            avgSharePct = if (s.events > 0) 100.0 * s.attendance / s.events / s.members else 0.0,
        )
    }

    /** One secretary per region, named from [names]; the signed-in member is the fourth. */
    val secretaries: List<LeaderboardSecretaryModel> get() = ranked.mapIndexed { index, (s, position) ->
        LeaderboardSecretaryModel(
            id = if (index == 3) ME_ID else "fixture-secretary-$index", name = names[index + 5],
            localOffice = "fixture-office-${s.region}", region = s.region,
            position = position.toDouble(), score = scoreOf(s), participation = s.participation.toDouble(),
            communityPct = s.community.toDouble(), nationalPoints = s.national.toDouble(),
            nationalEvents = if (s.national > 0) 1.0 else 0.0,
            effectiveAttendees = s.effective.toDouble(), distinctAttendees = s.distinct.toDouble(),
            corePct = 100.0 * s.effective / s.members, reachPct = 100.0 * s.distinct / s.members,
            eventsTotal = s.events.toDouble(), eventsScoring = (s.events * 0.6).toDouble(),
            attendanceTotal = s.attendance.toDouble(), membersActive = s.members.toDouble(),
            avgSharePct = if (s.events > 0) 100.0 * s.attendance / s.events / s.members else 0.0,
        )
    }
}
