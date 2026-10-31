package it.mensa.shared.leaderboard

import it.mensa.shared.model.LeaderboardEntryModel
import it.mensa.shared.model.LeaderboardRegionModel
import it.mensa.shared.model.LeaderboardSecretaryModel
import it.mensa.shared.model.LeaderboardSeasonModel
import it.mensa.shared.time.Season
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
            season = Season.current().toDouble(),
            name = name,
            position = (index + 1).toDouble(),
            score = score,
            stampsPoints = score - medals - events,
            medalPoints = medals,
            eventsPoints = events,
            stampsTotal = (34 - index * 0.7).coerceAtLeast(2.0),
            stampsDistinctRegions = (6 - index / 8).coerceAtLeast(1).toDouble(),
            // Yesterday's place, for the arrows: some up, some down, some still.
            previousPosition = (index + 1 + listOf(0, 2, -1)[index % 3]).coerceAtLeast(1).toDouble(),
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
            id = "fixture-office-${s.region}", season = Season.current().toDouble(), name = s.region, region = s.region, regionKey = s.region,
            position = position.toDouble(), score = scoreOf(s), participation = s.participation.toDouble(),
            communityPct = s.community.toDouble(), nationalPoints = s.national.toDouble(),
            nationalEvents = if (s.national > 0) 1.0 else 0.0,
            effectiveAttendees = s.effective.toDouble(), distinctAttendees = s.distinct.toDouble(),
            corePct = 100.0 * s.effective / s.members, reachPct = 100.0 * s.distinct / s.members,
            eventsTotal = s.events.toDouble(), eventsScoring = (s.events * 0.6).toDouble(),
            attendanceTotal = s.attendance.toDouble(), membersActive = s.members.toDouble(),
            avgSharePct = if (s.events > 0) 100.0 * s.attendance / s.events / s.members else 0.0,
            previousPosition = if (position % 2 == 0) position + 1.0 else (position - 1).coerceAtLeast(1).toDouble(),
        )
    }

    /** One secretary per region, named from [names]; the signed-in member is the fourth. */
    val secretaries: List<LeaderboardSecretaryModel> get() = ranked.mapIndexed { index, (s, position) ->
        LeaderboardSecretaryModel(
            id = if (index == 3) ME_ID else "fixture-secretary-$index", season = Season.current().toDouble(),
            name = names[index + 5],
            localOffice = "fixture-office-${s.region}", region = s.region,
            position = position.toDouble(), score = scoreOf(s), participation = s.participation.toDouble(),
            communityPct = s.community.toDouble(), nationalPoints = s.national.toDouble(),
            nationalEvents = if (s.national > 0) 1.0 else 0.0,
            effectiveAttendees = s.effective.toDouble(), distinctAttendees = s.distinct.toDouble(),
            corePct = 100.0 * s.effective / s.members, reachPct = 100.0 * s.distinct / s.members,
            eventsTotal = s.events.toDouble(), eventsScoring = (s.events * 0.6).toDouble(),
            attendanceTotal = s.attendance.toDouble(), membersActive = s.members.toDouble(),
            avgSharePct = if (s.events > 0) 100.0 * s.attendance / s.events / s.members else 0.0,
            previousPosition = if (position % 2 == 0) position + 1.0 else (position - 1).coerceAtLeast(1).toDouble(),
        )
    }

    // The hall of fame: invented winners of the two seasons before the current one.
    private val hallMembers = listOf(
        listOf(
            "Matilde Corsi" to 214, "Ettore Bassi" to 201, "Nadia Pagani" to 196, "Orlando Fiore" to 188,
            "Teresa Gallo" to 172, "Valerio Dini" to 165, "Lidia Ferraro" to 151, "Sergio Amato" to 149,
            "Cinzia Rota" to 133, "Fabio Guerra" to 128,
        ),
        listOf(
            "Ornella Piras" to 187, "Dario Stella" to 181, "Marta Cosentino" to 170, "Ivan Basile" to 163,
            "Luisa Marini" to 158, "Raffaele Pace" to 150, "Elisa Zanetti" to 144, "Guido Carbone" to 139,
            "Paola Sanna" to 131, "Cesare Poli" to 122,
        ),
    )
    private val hallRegions = listOf(
        listOf("Valle Chiara" to 18, "Terre Alte" to 15, "Laguna" to 11, "Pianura Grande" to 9, "Colline" to 7),
        listOf("Laguna" to 16, "Valle Chiara" to 14, "Altopiano" to 12, "Terre Alte" to 8, "Costa Verde" to 5),
    )
    private val hallSecretaries = listOf(
        listOf("Bruno Sartori", "Cecilia Monaco", "Domenico Vitale", "Emma Palumbo", "Flavio Rossetti"),
        listOf("Gaia Santoro", "Livio Benedetti", "Monica Pellegrino", "Nello Cattani", "Olga Fumagalli"),
    )

    /**
     * `leaderboard_seasons` as the server sends it: for each closed season the
     * top 10 of the members' board, the regions' and the secretaries', and the
     * signed-in member's own row in the first one (a place below the podium).
     */
    fun seasons(current: Int = Season.current()): List<LeaderboardSeasonModel> =
        hallMembers.indices.flatMap { index ->
            val season = current - 1 - index
            fun row(board: String, subject: String, name: String, position: Int, score: Int, key: String = "", office: String = "") =
                LeaderboardSeasonModel(
                    id = "fixture-$season-$board-$position", board = board, season = season.toDouble(), subject = subject,
                    name = name, position = position.toDouble(), score = score.toDouble(), regionKey = key, localOffice = office,
                )
            val members = hallMembers[index].mapIndexed { place, (name, score) ->
                row("members", "fixture-hall-$season-$place", name, place + 1, score)
            }
            val ownRow = if (index == 0) listOf(row("members", ME_ID, names[mePlace - 1], 23, 74)) else emptyList()
            val regions = hallRegions[index].mapIndexed { place, (region, score) ->
                row("regions", "fixture-office-$region", region, place + 1, score, key = region, office = "fixture-office-$region")
            }
            val secretaries = hallSecretaries[index].mapIndexed { place, name ->
                val (region, score) = hallRegions[index][place]
                row("secretaries", "fixture-hall-secretary-$season-$place", name, place + 1, score, key = region, office = "fixture-office-$region")
            }
            members + ownRow + regions + secretaries
        }
}
