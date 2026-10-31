package it.mensa.shared.leaderboard

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.LeaderboardEntryModel
import it.mensa.shared.model.LeaderboardRegionModel
import it.mensa.shared.model.LeaderboardSecretaryModel
import it.mensa.shared.model.LeaderboardSeasonModel
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.repository.MetadataRepository
import it.mensa.shared.repository.RegSociRepository
import it.mensa.shared.time.Season
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.roundToInt

/** A member in the ranking, with what the app needs to show them. */
data class LeaderboardRow(
    val entry: LeaderboardEntryModel,
    /** The member's directory record, for the photo; null until the directory is cached. */
    val member: RegSociModel?,
    val isMe: Boolean,
) {
    val id: String get() = entry.id
    val position: Int get() = entry.position.roundToInt()
    val score: Int get() = entry.score.roundToInt()
    val stampsPoints: Int get() = entry.stampsPoints.roundToInt()
    val medalPoints: Int get() = entry.medalPoints.roundToInt()
    val eventsPoints: Int get() = entry.eventsPoints.roundToInt()
    val stampsTotal: Int get() = entry.stampsTotal.roundToInt()
    val regions: Int get() = entry.stampsDistinctRegions.roundToInt()

    /** The season the row is for; 0 from an older view. */
    val season: Int get() = entry.season.roundToInt()

    /** Places gained since the last daily snapshot (negative = lost); null without a snapshot. */
    val trend: Int? get() = trendOf(entry.previousPosition, position)

    /** The name as written in the directory, or the account's when it isn't cached. */
    val displayName: String get() = titleCase(member?.name?.takeIf { it.isNotBlank() } ?: entry.name)

    /** Up to two initials, for when there is no photo. */
    val initials: String
        get() = displayName.split(" ").filter { it.isNotBlank() }.take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "?" }
}

/** The whole ranking as the signed-in member sees it. */
data class LeaderboardBoard(
    /** Everyone in the ranking, best first. */
    val rows: List<LeaderboardRow>,
    /** The signed-in member's row; null when they aren't ranked. */
    val me: LeaderboardRow?,
    /**
     * Points the member needs to overtake the closest member above; null at
     * the top or when not ranked.
     */
    val pointsToNext: Int?,
    /** The position they would reach with [pointsToNext]. */
    val nextPosition: Int?,
    /** The season the ranking is for (the rows', or the current one while it has no rows). */
    val season: Int = Season.current(),
) {
    val podium: List<LeaderboardRow> get() = rows.take(3)

    /** What the app shows: the first [TOP_SIZE] places (ties included). */
    val top: List<LeaderboardRow> get() = rows.filter { it.position <= TOP_SIZE }

    /** The member's row when it falls below [top], to show apart. */
    val meBelowTop: LeaderboardRow? get() = me?.takeIf { it.position > TOP_SIZE }

    /** The member's podium place, 1 to 3; null off the podium. */
    val myPodiumPlace: Int? get() = me?.position?.takeIf { it in 1..3 }

    companion object {
        const val TOP_SIZE: Int = 10
    }
}

/**
 * A region's score and the numbers behind it, as `view_leaderboard_regioni`
 * computes them: score = participation × community / 100 + nationalPoints.
 * Shared by the region's own row and its secretary's, so one sheet can show
 * the calculation for both.
 */
interface RegionScore {
    /** The region as the local office calls itself (a seat covering two regions names both). */
    val regionName: String
    val position: Int
    val score: Int
    /** Places gained since the last daily snapshot (negative = lost); null without a snapshot. */
    val trend: Int?
    /** Points of the region's own events over 12 months, before the community factor. */
    val participation: Int
    /** The community factor, 0 to 100. */
    val communityPct: Int
    val nationalPoints: Int
    val nationalEvents: Int
    val eventsTotal: Int
    /** Events above the 2% threshold, the ones that scored. */
    val eventsScoring: Int
    val attendanceTotal: Int
    val distinctAttendees: Int
    val effectiveAttendees: Int
    val membersActive: Int
    /** Effective attendees as a share of the region's members. */
    val corePct: Int
    /** Distinct attendees as a share of the region's members. */
    val reachPct: Int
    /** Average share of the region's members present at an event. */
    val avgSharePct: Int

    /** Whether there is anything to rank: an event in the region, or a national one hosted. */
    val hasEvents: Boolean get() = eventsTotal > 0 || nationalEvents > 0
}

/** A region in its ranking, with what the app needs to show it. */
data class RegionRow(val entry: LeaderboardRegionModel) : RegionScore {
    val id: String get() = entry.id
    override val regionName: String get() = entry.region.ifBlank { entry.name }
    override val position: Int get() = entry.position.roundToInt()
    override val score: Int get() = entry.score.roundToInt()
    override val trend: Int? get() = trendOf(entry.previousPosition, position)
    override val participation: Int get() = entry.participation.roundToInt()
    override val communityPct: Int get() = entry.communityPct.roundToInt()
    override val nationalPoints: Int get() = entry.nationalPoints.roundToInt()
    override val nationalEvents: Int get() = entry.nationalEvents.roundToInt()
    override val eventsTotal: Int get() = entry.eventsTotal.roundToInt()
    override val eventsScoring: Int get() = entry.eventsScoring.roundToInt()
    override val attendanceTotal: Int get() = entry.attendanceTotal.roundToInt()
    override val distinctAttendees: Int get() = entry.distinctAttendees.roundToInt()
    override val effectiveAttendees: Int get() = entry.effectiveAttendees.roundToInt()
    override val membersActive: Int get() = entry.membersActive.roundToInt()
    override val corePct: Int get() = entry.corePct.roundToInt()
    override val reachPct: Int get() = entry.reachPct.roundToInt()
    override val avgSharePct: Int get() = entry.avgSharePct.roundToInt()

    val displayName: String get() = regionName

    /** The season the row is for; 0 from an older view. */
    val season: Int get() = entry.season.roundToInt()
}

/** A regional secretary in their ranking: their region's numbers, their face. */
data class SecretaryRow(
    val entry: LeaderboardSecretaryModel,
    /** The member's directory record, for the photo; null until the directory is cached. */
    val member: RegSociModel?,
    val isMe: Boolean,
) : RegionScore {
    val id: String get() = entry.id
    override val regionName: String get() = entry.region
    override val position: Int get() = entry.position.roundToInt()
    override val trend: Int? get() = trendOf(entry.previousPosition, position)
    override val score: Int get() = entry.score.roundToInt()
    override val participation: Int get() = entry.participation.roundToInt()
    override val communityPct: Int get() = entry.communityPct.roundToInt()
    override val nationalPoints: Int get() = entry.nationalPoints.roundToInt()
    override val nationalEvents: Int get() = entry.nationalEvents.roundToInt()
    override val eventsTotal: Int get() = entry.eventsTotal.roundToInt()
    override val eventsScoring: Int get() = entry.eventsScoring.roundToInt()
    override val attendanceTotal: Int get() = entry.attendanceTotal.roundToInt()
    override val distinctAttendees: Int get() = entry.distinctAttendees.roundToInt()
    override val effectiveAttendees: Int get() = entry.effectiveAttendees.roundToInt()
    override val membersActive: Int get() = entry.membersActive.roundToInt()
    override val corePct: Int get() = entry.corePct.roundToInt()
    override val reachPct: Int get() = entry.reachPct.roundToInt()
    override val avgSharePct: Int get() = entry.avgSharePct.roundToInt()
    val region: String get() = entry.region

    val displayName: String get() = titleCase(member?.name?.takeIf { it.isNotBlank() } ?: entry.name)

    /** Up to two initials, for when there is no photo. */
    val initials: String
        get() = displayName.split(" ").filter { it.isNotBlank() }.take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifEmpty { "?" }
}

/**
 * The three rankings, read from `view_leaderboard`, `view_leaderboard_regioni`
 * and `view_leaderboard_segretari`, all for the current season (1 October to
 * 30 September), and the hall of fame of the closed ones (`leaderboard_seasons`). In memory, reread whenever a screen that
 * shows them appears; the last rows read are also kept in SQLite (KeyValue,
 * as JSON), so after a cold start they show at once and the network only
 * brings them up to date. The members' one is tied to the user it was read
 * for, so another account never sees the previous one's place.
 */
class LeaderboardRepository(
    private val pb: PocketBaseClient,
    private val auth: AuthRepository,
    private val regSoci: RegSociRepository,
    private val metadata: MetadataRepository,
    private val db: MensaDatabase,
    private val json: Json,
) {
    private val cache = MutableStateFlow<Pair<String, List<LeaderboardEntryModel>>?>(null)
    /** What the fixtures stand in for [KEY_HIDDEN] with. */
    private val fixtureHidden = MutableStateFlow(false)
    private val regionsCache = MutableStateFlow<List<LeaderboardRegionModel>?>(null)
    private val secretariesCache = MutableStateFlow<List<LeaderboardSecretaryModel>?>(null)
    private val hallCache = MutableStateFlow<List<LeaderboardSeasonModel>?>(null)

    /**
     * The regions' ranking, best first; null until read. The server decides
     * who sees what (see [VISIBLE_MIN_SCORE]): everyone gets the regions at
     * or above the threshold, a region's staff also gets their own.
     */
    fun observeRegions(): Flow<List<RegionRow>?> =
        regionsCache.map { cached ->
            // Same points, same position: within a tie, the more active first.
            cached?.sortedWith(
                compareBy<LeaderboardRegionModel> { it.position }.thenByDescending { it.participation }
                    .thenByDescending { it.eventsTotal }.thenBy { it.region },
            )
                ?.map { RegionRow(it) }
        }

    /** The secretaries' ranking, best first; null until read. Same visibility as [observeRegions]. */
    fun observeSecretaries(): Flow<List<SecretaryRow>?> =
        combine(secretariesCache, auth.currentUser, regSoci.observeAll()) { cached, user, members ->
            if (cached == null) return@combine null
            val meId = if (LeaderboardFixtures.enabled) LeaderboardFixtures.ME_ID else user?.id
            val byId = members.associateBy { it.id }
            cached.sortedWith(
                compareBy<LeaderboardSecretaryModel> { it.position }.thenByDescending { it.participation }
                    .thenByDescending { it.eventsTotal }.thenBy { it.region }.thenBy { it.name },
            )
                .map { SecretaryRow(it, byId[it.id], isMe = it.id == meId) }
        }

    /** Rereads the regions' and secretaries' rankings. Throws on network errors; the last read stays. */
    @Throws(Exception::class)
    suspend fun refreshRegions() {
        if (LeaderboardFixtures.enabled) {
            regionsCache.value = LeaderboardFixtures.regions
            secretariesCache.value = LeaderboardFixtures.secretaries
            return
        }
        if (regionsCache.value == null) {
            regionsCache.value = readSaved(CACHE_KEY_REGIONS, ListSerializer(LeaderboardRegionModel.serializer()))
            secretariesCache.value = readSaved(CACHE_KEY_SECRETARIES, ListSerializer(LeaderboardSecretaryModel.serializer()))
        }
        val regions = pb.fullList<LeaderboardRegionModel>(REGIONS_COLLECTION, sort = "position,region")
        val secretaries = pb.fullList<LeaderboardSecretaryModel>(SECRETARIES_COLLECTION, sort = "position,region")
        regionsCache.value = regions
        secretariesCache.value = secretaries
        writeSaved(CACHE_KEY_REGIONS, ListSerializer(LeaderboardRegionModel.serializer()), regions)
        writeSaved(CACHE_KEY_SECRETARIES, ListSerializer(LeaderboardSecretaryModel.serializer()), secretaries)
    }

    /**
     * The hall of fame: the closed seasons, latest first, with the top of each
     * board and the member's own row; null until read.
     */
    fun observeHall(): Flow<HallOfFame?> =
        combine(hallCache, auth.currentUser, regSoci.observeAll()) { cached, user, members ->
            if (cached == null) return@combine null
            val meId = if (LeaderboardFixtures.enabled) LeaderboardFixtures.ME_ID else user?.id
            HallOfFame.build(cached, meId, members.associateBy { it.id })
        }

    /** Rereads the hall of fame. Throws on network errors; the last read stays. */
    @Throws(Exception::class)
    suspend fun refreshHall() {
        if (LeaderboardFixtures.enabled) {
            hallCache.value = LeaderboardFixtures.seasons()
            return
        }
        val serializer = ListSerializer(LeaderboardSeasonModel.serializer())
        if (hallCache.value == null) {
            // The top 10 is public; a row below it is the reader's own, and the saved one may be another account's.
            val meId = auth.currentUser.value?.id
            hallCache.value = readSaved(CACHE_KEY_SEASONS, serializer)
                ?.filter { it.position <= HALL_TOP_SIZE || it.subject == meId }
        }
        val rows: List<LeaderboardSeasonModel> = pb.fullList(SEASONS_COLLECTION, sort = "-season,board,position")
        hallCache.value = rows
        writeSaved(CACHE_KEY_SEASONS, serializer, rows)
    }

    private suspend fun <T> readSaved(key: String, serializer: kotlinx.serialization.KSerializer<T>): T? =
        runCatching {
            db.keyValueQueries.selectById(key).awaitAsOneOrNull()?.value_?.let { json.decodeFromString(serializer, it) }
        }.getOrNull()

    private suspend fun <T> writeSaved(key: String, serializer: kotlinx.serialization.KSerializer<T>, value: T) {
        runCatching { db.keyValueQueries.insertOrReplace(key = key, value_ = json.encodeToString(serializer, value)) }
    }

    /**
     * Whether the member asked to be left out of the members' ranking:
     * `users_metadata` key [KEY_HIDDEN] = "true". The view skips them
     * entirely (no row, no place, the others move up); medals and the
     * regions' and secretaries' rankings are untouched.
     */
    fun observeHidden(): Flow<Boolean> =
        if (LeaderboardFixtures.enabled) fixtureHidden
        else metadata.state.map { it[KEY_HIDDEN] == "true" }

    /** Rereads the member's metadata, so [observeHidden] is current. Throws on network errors. */
    @Throws(Exception::class)
    suspend fun refreshHidden() {
        if (LeaderboardFixtures.enabled) return
        val userId = auth.currentUser.value?.id ?: return
        metadata.refresh(userId)
    }

    /** Leaves or rejoins the members' ranking, then rereads it. Throws on network errors. */
    @Throws(Exception::class)
    suspend fun setHidden(hidden: Boolean) {
        if (LeaderboardFixtures.enabled) {
            fixtureHidden.value = hidden
            return
        }
        val userId = auth.currentUser.value?.id ?: return
        metadata.set(userId, KEY_HIDDEN, if (hidden) "true" else "false")
        refresh()
    }

    /** The ranking, once read for the current user; null until then. */
    fun observe(): Flow<LeaderboardBoard?> =
        combine(cache, auth.currentUser, regSoci.observeAll()) { cached, user, members ->
            val meId = if (LeaderboardFixtures.enabled) LeaderboardFixtures.ME_ID else user?.id
            if (cached == null || meId == null || cached.first != meId) return@combine null
            val byId = members.associateBy { it.id }
            build(cached.second, meId, byId)
        }

    /** Rereads the ranking. Throws on network errors; the last read stays. */
    @Throws(Exception::class)
    suspend fun refresh() {
        if (LeaderboardFixtures.enabled) {
            cache.value = LeaderboardFixtures.ME_ID to LeaderboardFixtures.entries
            return
        }
        val userId = auth.currentUser.value?.id ?: return
        val serializer = ListSerializer(LeaderboardEntryModel.serializer())
        if (cache.value?.first != userId) {
            // The rows saved for this user, shown while the network answers.
            readSaved("$CACHE_KEY_MEMBERS$userId", serializer)?.let { saved ->
                if (cache.value?.first != userId) cache.value = userId to saved
            }
        }
        val entries: List<LeaderboardEntryModel> = pb.fullList(COLLECTION, sort = "position,name")
        cache.value = userId to entries
        writeSaved("$CACHE_KEY_MEMBERS$userId", serializer, entries)
    }

    companion object {
        private const val COLLECTION = "view_leaderboard"
        private const val REGIONS_COLLECTION = "view_leaderboard_regioni"
        private const val SECRETARIES_COLLECTION = "view_leaderboard_segretari"
        private const val SEASONS_COLLECTION = "leaderboard_seasons"

        /** Places the server keeps for each board in `leaderboard_seasons`, besides the reader's own row. */
        private const val HALL_TOP_SIZE = 10

        /** `users_metadata` key read by `view_leaderboard`: change both together. */
        const val KEY_HIDDEN: String = "leaderboard_hidden"

        // KeyValue rows holding the last rows read, as JSON.
        private const val CACHE_KEY_MEMBERS = "leaderboard.members."
        private const val CACHE_KEY_REGIONS = "leaderboard.regions"
        private const val CACHE_KEY_SECRETARIES = "leaderboard.secretaries"
        private const val CACHE_KEY_SEASONS = "leaderboard.seasons"

        /**
         * The regions' and secretaries' views are public from this score up;
         * below it, PocketBase's rules return a row only to that region's
         * staff (`local_offices_admins`). So a row under the threshold is
         * the member's own region, shown apart under the list. Mirrors the
         * views' API rules: change both together.
         */
        const val VISIBLE_MIN_SCORE: Int = 3

        internal fun build(
            entries: List<LeaderboardEntryModel>,
            meId: String,
            members: Map<String, RegSociModel>,
        ): LeaderboardBoard {
            val rows = entries
                .sortedWith(compareBy<LeaderboardEntryModel> { it.position }.thenByDescending { it.score })
                .map { LeaderboardRow(it, members[it.id], isMe = it.id == meId) }
            val me = rows.firstOrNull { it.isMe }
            // The closest member above with a higher score: ties share a place,
            // so passing someone level with you gains nothing.
            val above = me?.let { mine -> rows.lastOrNull { it.entry.score > mine.entry.score } }
            val gap = if (me != null && above != null) {
                ceil(above.entry.score - me.entry.score).toInt().coerceAtLeast(1)
            } else null
            return LeaderboardBoard(
                rows = rows,
                me = me,
                pointsToNext = gap,
                nextPosition = above?.position,
                season = rows.firstOrNull { it.season > 0 }?.season ?: Season.current(),
            )
        }
    }
}

/** "MARIO ROSSI" and "mario rossi" both become "Mario Rossi"; mixed case is left alone. */
internal fun titleCase(name: String): String {
    val trimmed = name.trim()
    if (trimmed.any { it.isLowerCase() } && trimmed.any { it.isUpperCase() }) return trimmed
    return trimmed.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercaseChar() }
    }
}

/** Places gained between the last snapshot and now; null when the server has no snapshot yet (previous = 0). */
internal fun trendOf(previous: Double, current: Int): Int? {
    val before = previous.roundToInt()
    return if (before <= 0) null else before - current
}
