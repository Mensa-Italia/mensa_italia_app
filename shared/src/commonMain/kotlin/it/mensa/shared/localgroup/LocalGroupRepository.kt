package it.mensa.shared.localgroup

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.endpoints.DealsApi
import it.mensa.shared.api.endpoints.EventsApi
import it.mensa.shared.api.endpoints.LocalOfficesApi
import it.mensa.shared.api.endpoints.RegSociApi
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.auth.Powers
import it.mensa.shared.config.FeatureFlags
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.geo.ItalianRegions
import it.mensa.shared.geo.TextNormalizer
import it.mensa.shared.medals.RegionKey
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LeaderboardRegionModel
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeMonthlyStatsModel
import it.mensa.shared.model.LocalOfficeStatsModel
import it.mensa.shared.model.MemberChangeModel
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The member's local office ("Gruppo locale"): the office of the region the
 * member is registered in now (`members_registry.area` -> [RegionKey] ->
 * `local_offices.region_key`), with its people, links, figures, ranking,
 * next events and deals, plus the admin area's data for its secretaries.
 *
 * [observe] is tied to the signed-in user: another account never sees the
 * previous one's office. The region key and the whole snapshot are kept in
 * SQLite (KeyValue), so after a cold start the tab shows at once and
 * [refresh] only brings it up to date.
 *
 * Off unless the `local_group_enabled` flag is on ([observeAvailable]). With
 * [LocalGroupFixtures.enabled] everything answers made-up data and the
 * network is never touched.
 */
class LocalGroupRepository(
    private val pb: PocketBaseClient,
    private val auth: AuthRepository,
    private val regSoci: RegSociApi,
    private val db: MensaDatabase,
    private val json: Json,
    private val flags: FeatureFlags,
    private val offices: LocalOfficesApi,
    private val events: EventsApi,
    private val deals: DealsApi,
) {
    /** The state read for [userId]. */
    private data class Held(val userId: String, val state: LocalGroupState)

    private val held = MutableStateFlow<Held?>(null)

    /** A board member's choice of office, for one user: the next member to sign in must not inherit it. */
    private data class Choice(val userId: String, val officeId: String)

    /** The office a board member chose to look at instead of their own; in memory only, so it resets when the app closes. */
    private val override = MutableStateFlow<Choice?>(null)

    /** [override] as the signed-in member sees it: null for their own office. */
    private val selected: StateFlow<String?> by lazy {
        combine(override, auth.currentUser) { choice, user -> choice?.takeIf { user != null && it.userId == user.id }?.officeId }
            .stateIn(scope, SharingStarted.Eagerly, null)
    }

    /** Whether the signed-in member sits on the board, read once per process (null = not read yet). */
    private var boardCache: Pair<String, Boolean>? = null

    /** The member's own office id (user id to office id), from the disk or the network: [listOffices] leaves it out and [selectOffice] of it means "back to mine". */
    private var ownOffice: Pair<String, String>? = null

    private fun chosenFor(userId: String): String? = override.value?.takeIf { it.userId == userId }?.officeId

    private fun ownOfficeIdFor(userId: String): String? = ownOffice?.takeIf { it.first == userId }?.second
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** All 18 offices, read once per process. */
    private var officesCache: List<LocalOfficeModel>? = null

    private val state: StateFlow<LocalGroupState> =
        combine(held, auth.currentUser) { held, user ->
            if (user == null || held == null || held.userId != user.id) LocalGroupState.Loading else held.state
        }.stateIn(scope, SharingStarted.Eagerly, LocalGroupState.Loading)

    private val fixtureOverride = MutableStateFlow<String?>(null)

    private val fixtureState: StateFlow<LocalGroupState> by lazy {
        fixtureOverride.map { chosen -> LocalGroupState.Ready(LocalGroupFixtures.snapshot(officeId = chosen)) }
            .stateIn(scope, SharingStarted.Eagerly, LocalGroupState.Ready(LocalGroupFixtures.snapshot()))
    }

    /**
     * The tab's state for the signed-in member. [LocalGroupState.Loading]
     * only while nothing was ever read for them; the saved snapshot shows as
     * [LocalGroupState.Ready] as soon as [refresh] starts.
     */
    fun observe(): StateFlow<LocalGroupState> = if (LocalGroupFixtures.enabled) fixtureState else state

    /** Whether the tab exists: the `local_group_enabled` flag is on and a member is signed in. */
    fun observeAvailable(): Flow<Boolean> =
        combine(flags.flags, auth.currentUser) { current, user ->
            LocalGroupFixtures.enabled || (current.localGroup && user != null)
        }

    /**
     * Rereads everything: region -> office -> the office's pieces, in
     * parallel. A piece that fails (stats, ranking, events, deals, links,
     * people) comes back empty or null; only a failure to resolve the
     * office makes the state [LocalGroupState.Failed], with the last
     * snapshot in it. Network errors never throw: they end up in the state.
     */
    @Throws(Exception::class)
    suspend fun refresh() {
        if (LocalGroupFixtures.enabled) return
        if (!flags.localGroupEnabled) return
        val user = auth.currentUser.value ?: return
        chosenFor(user.id)?.let { chosen ->
            refreshOverride(chosen, user)
            return
        }
        primeFromDisk(user.id)
        val resolved = try {
            resolveKey(user.id)?.let { key -> key to findOffice(key) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            publishOwn(user.id, LocalGroupState.Failed(e.message, lastOwnSnapshot(user.id)))
            return
        }
        val (key, office) = resolved ?: run {
            publishOwn(user.id, LocalGroupState.NoRegion)
            return
        }
        if (office == null) {
            publishOwn(user.id, LocalGroupState.NoOffice)
            return
        }
        ownOffice = user.id to office.id
        val snapshot = load(office, key, user)
        // Still the member's own office on screen? A switch made meanwhile wins.
        publishOwn(user.id, LocalGroupState.Ready(snapshot))
        save(user.id, snapshot)
    }

    // ---- Admin area ----

    /**
     * The members who joined, left or moved in or out of the office's region
     * between [from] and [to] (both included), newest first. Only the
     * office's admins and super can read them: anyone else gets an error.
     */
    @Throws(Exception::class)
    suspend fun loadChanges(officeId: String, from: LocalDate, to: LocalDate): List<MemberChangeModel> {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.changes(from, to)
        val start = LocalGroupDates.romeMidnight(from)
        val until = LocalGroupDates.romeMidnight(to.plus(1, DateTimeUnit.DAY))
        return pb.fullList<MemberChangeModel>(
            CHANGES,
            filter = "local_office = ${quote(officeId)} && day >= ${quote(start)} && day < ${quote(until)}",
            sort = "-day,name",
        ).sortedWith(compareByDescending<MemberChangeModel> { it.dayIso }.thenBy { it.name })
    }

    /**
     * The members who joined the office's region in the last
     * [NEW_MEMBERS_DAYS] days (new or arrived from another region), by
     * name: the people behind [LocalOfficeStatsModel.membersNew7d]. Every
     * signed-in member can read them, like the members' directory.
     */
    @Throws(Exception::class)
    suspend fun loadNewMembers(officeId: String): List<RegSociModel> {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.newMembers()
        // the server's window: day > today - 7 (Rome midnights)
        val after = LocalGroupDates.romeMidnight(LocalGroupDates.today().minus(NEW_MEMBERS_DAYS, DateTimeUnit.DAY))
        val ids = pb.fullList<MemberChangeModel>(
            CHANGES,
            filter = "local_office = ${quote(officeId)} && (kind = \"joined\" || kind = \"moved_in\") && day > ${quote(after)}",
            sort = "-day",
        ).map { it.member }.filter { it.isNotBlank() }.distinct()
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(IDS_PER_QUERY)
            .flatMap { chunk -> regSoci.list(filter = chunk.joinToString(" || ") { "id = ${quote(it)}" }) }
            .sortedBy { it.name.lowercase() }
    }

    /** The office's months, oldest first. Only the office's admins and super can read them. */
    @Throws(Exception::class)
    suspend fun loadMonthly(officeId: String): List<LocalOfficeMonthlyStatsModel> {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.monthly(LocalGroupDates.today())
        return pb.fullList<LocalOfficeMonthlyStatsModel>(MONTHLY, filter = "local_office = ${quote(officeId)}", sort = "month")
            .sortedBy { it.month }
    }

    /**
     * Every event of the office, past and future: the ones with
     * `local_office` = the office and the ones placed in its region. Latest
     * first.
     */
    @Throws(Exception::class)
    suspend fun loadOfficeEvents(officeId: String): List<EventModel> {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.officeEvents()
        val key = allOffices().firstOrNull { it.id == officeId }?.let(::officeRegionKey)
        val filter = regionFilter(officeId, key)
        return events.list(filter = filter, sort = "-when_start")
            .filter { belongsTo(it, officeId, key) }
            .sortedByDescending { it.whenStart }
    }

    /**
     * The .xlsx of the office's members ([kind]; [from]/[to] for the
     * movements, ignored for [ExportKind.ACTIVE]), as bytes. Only the
     * office's admins and super: anyone else gets an error (403). Save it
     * with [LocalGroupExportFile.fileName] and share it.
     */
    @Throws(Exception::class)
    suspend fun exportMembers(officeId: String, kind: ExportKind, from: LocalDate?, to: LocalDate?): ByteArray {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.exportBytes()
        val query = buildMap {
            put("kind", kind.wire)
            from?.let { put("from", it.toString()) }
            to?.let { put("to", it.toString()) }
        }
        return pb.getBytes("/api/cs/local-office/$officeId/members-export", query)
    }

    // ---- Board: other offices and the association ----

    /** The id of the office chosen with [selectOffice], null while the tab shows the member's own. */
    fun observeSelectedOffice(): StateFlow<String?> = if (LocalGroupFixtures.enabled) fixtureOverride.asStateFlow() else selected

    /**
     * Shows [officeId] in the tab instead of the member's own office (null
     * goes back to it). For board members and super only: the caller checks
     * [LocalGroupSnapshot.isBoard]. In memory only: closing the app comes
     * back to the member's office. The chosen office is never saved to disk.
     */
    @Throws(Exception::class)
    suspend fun selectOffice(officeId: String?) {
        if (LocalGroupFixtures.enabled) {
            fixtureOverride.value = officeId?.takeIf { it.isNotBlank() && it != LocalGroupFixtures.OFFICE_ID }
            return
        }
        val user = auth.currentUser.value ?: return
        val chosen = officeId?.takeIf { it.isNotBlank() && it != ownOfficeIdFor(user.id) }
        override.value = chosen?.let { Choice(user.id, it) }
        // The page shows its loader until the chosen office is in: the old one must not linger.
        publish(user.id, LocalGroupState.Loading)
        refresh()
    }

    /** The other offices, by name: the picker of [selectOffice]. The member's own is not in it, it has its own row. */
    @Throws(Exception::class)
    suspend fun listOffices(): List<LocalOfficeModel> {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.offices()
        val own = auth.currentUser.value?.let { ownOfficeIdFor(it.id) }
        return allOffices().filter { it.id != own }.sortedBy { it.name.lowercase() }
    }

    /** Every office's name by id, the member's own included: to label rows that carry only an office id. */
    @Throws(Exception::class)
    suspend fun officeNames(): Map<String, String> {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.officeNames()
        return allOffices().associate { it.id to it.name }
    }

    /**
     * The association's figures now (one row, id "association"): the same
     * numbers as an office's, over every member, event and deal. Board and
     * super only: anyone else gets an error. Null when the server has none yet.
     */
    @Throws(Exception::class)
    suspend fun loadAssociationStats(): LocalOfficeStatsModel? {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.associationStats()
        return pb.getOne<LocalOfficeStatsModel>(ASSOCIATION_STATS, ASSOCIATION_ID)
    }

    /** The association's months, oldest first, from the previous season's October. Board and super only. */
    @Throws(Exception::class)
    suspend fun loadAssociationMonthly(): List<LocalOfficeMonthlyStatsModel> {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.associationMonthly(LocalGroupDates.today())
        return pb.fullList<LocalOfficeMonthlyStatsModel>(ASSOCIATION_MONTHLY, sort = "month").sortedBy { it.month }
    }

    /** Every office's figures now, most members first: the comparison table of the association page. */
    @Throws(Exception::class)
    suspend fun loadAllOfficeStats(): List<LocalOfficeStatsModel> {
        if (LocalGroupFixtures.enabled) return LocalGroupFixtures.allOfficeStats()
        return pb.fullList<LocalOfficeStatsModel>(STATS, sort = "-members_active").sortedByDescending { it.membersActive }
    }

    /** Loads the chosen office like [refresh] does the member's own, without touching the disk cache. */
    private suspend fun refreshOverride(officeId: String, user: UserModel) {
        val office = try {
            allOffices().firstOrNull { it.id == officeId }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            publishOverride(user.id, officeId, LocalGroupState.Failed(e.message, lastOverrideSnapshot(user.id, officeId)))
            return
        }
        if (office == null) {
            override.value = null
            refresh()
            return
        }
        val snapshot = load(office, officeRegionKey(office).orEmpty(), user).copy(isOverride = true)
        // Still the chosen office? A later choice (or "back to mine") wins.
        publishOverride(user.id, officeId, LocalGroupState.Ready(snapshot))
    }

    /** Publishes a state of the member's own office, unless another office was chosen meanwhile. */
    private fun publishOwn(userId: String, value: LocalGroupState) {
        if (chosenFor(userId) == null) publish(userId, value)
    }

    /** Publishes a state of the chosen [officeId], unless the choice changed meanwhile. */
    private fun publishOverride(userId: String, officeId: String, value: LocalGroupState) {
        if (chosenFor(userId) == officeId) publish(userId, value)
    }

    /** The last snapshot of the member's own office: the one in memory if it is theirs, else the saved one. */
    private suspend fun lastOwnSnapshot(userId: String): LocalGroupSnapshot? =
        heldSnapshot(userId)?.takeUnless { it.isOverride } ?: readSnapshot(userId)

    /** The last snapshot of the chosen [officeId], if that is what memory holds; never another office's. */
    private fun lastOverrideSnapshot(userId: String, officeId: String): LocalGroupSnapshot? =
        heldSnapshot(userId)?.takeIf { it.isOverride && it.office.id == officeId }

    private fun heldSnapshot(userId: String): LocalGroupSnapshot? =
        when (val current = held.value?.takeIf { it.userId == userId }?.state) {
            is LocalGroupState.Ready -> current.snapshot
            is LocalGroupState.Failed -> current.cached
            else -> null
        }

    /** Whether [user] sits on the board (an active row of the org chart's "consiglio" group) or is super. Read once. */
    private suspend fun isBoard(user: UserModel): Boolean {
        if (Powers.has(user.powers, Powers.SUPER)) return true
        boardCache?.takeIf { it.first == user.id }?.let { return it.second }
        val rows = pb.fullList<BoardRow>(
            ORG_CHART_MEMBERS,
            filter = "user = ${quote(user.id)} && inactive = false",
            expand = "group",
        )
        val sits = rows.any { it.expand?.group?.title.equals(BOARD_GROUP, ignoreCase = true) }
        boardCache = user.id to sits
        return sits
    }

    /** One row of `org_chart_members` with its group expanded, enough to tell the board apart. */
    @Serializable
    private data class BoardRow(val id: String = "", val expand: BoardRowExpand? = null)

    @Serializable
    private data class BoardRowExpand(val group: BoardGroup? = null)

    @Serializable
    private data class BoardGroup(val title: String = "")

    // ---- Loading ----

    private suspend fun load(office: LocalOfficeModel, key: String, user: UserModel): LocalGroupSnapshot = coroutineScope {
        val admins = async { optional(emptyList()) { sortAdmins(offices.adminsByOffice(office.id)) } }
        val links = async { optional(emptyList()) { activeLinks(offices.linktreeByOffice(office.id)) } }
        val stats = async { optional(null) { pb.getOne<LocalOfficeStatsModel>(STATS, office.id) } }
        val ranking = async { optional(null) { pb.getOne<LeaderboardRegionModel>(RANKING, office.id) } }
        val next = async { optional(emptyList()) { nextEvents(office.id, key) } }
        val localDeals = async { optional(emptyList()) { regionDeals(key) } }
        val board = async { optional(false) { isBoard(user) } }
        val people = admins.await()
        LocalGroupSnapshot(
            office = office,
            admins = people,
            links = links.await(),
            stats = stats.await(),
            ranking = ranking.await(),
            nextEvents = next.await(),
            deals = localDeals.await(),
            isAdmin = isAdmin(user, people),
            regionKey = key,
            isBoard = board.await(),
        )
    }

    private suspend fun nextEvents(officeId: String, key: String): List<EventModel> =
        events.list(filter = "when_end >= @now && ${regionFilter(officeId, key)}", sort = "when_start")
            .filter { belongsTo(it, officeId, key) }
            .sortedBy { it.whenStart }
            .take(MAX_EVENTS)

    /**
     * The deals a member of the region can use: the region's local ones
     * first, then the national ones (not tied to a place: hotels, shops
     * online), at most [MAX_DEALS] in all. A region with no local deal
     * still has the national ones to show.
     */
    private suspend fun regionDeals(key: String): List<DealModel> = coroutineScope {
        val local = async { localDeals(key) }
        val national = async { nationalDeals() }
        (local.await() + national.await()).distinctBy { it.id }.take(MAX_DEALS)
    }

    private suspend fun localDeals(key: String): List<DealModel> {
        val states = statesOf(key).joinToString(" || ") { "position.state = ${quote(it)}" }
        val filter = "is_local = true && is_active = true && (ending = \"\" || ending > @now) && ($states)"
        return deals.list(filter = filter, sort = "-created")
            .filter { RegionKey.of(it.position?.state) == key }
    }

    private suspend fun nationalDeals(): List<DealModel> =
        deals.list(filter = "is_local = false && is_active = true && (ending = \"\" || ending > @now)", sort = "-created")

    private suspend fun findOffice(key: String): LocalOfficeModel? =
        allOffices().firstOrNull { officeRegionKey(it) == key }

    private suspend fun allOffices(): List<LocalOfficeModel> =
        officesCache ?: offices.listAll().also { officesCache = it }

    /**
     * The key of the member's region: from their registry row, or the one
     * saved when the network doesn't answer. Null when they live abroad.
     */
    private suspend fun resolveKey(userId: String): String? {
        val fresh = try {
            RegionKey.of(regSoci.get(userId).area)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val saved = readValue(keyKey(userId)) ?: throw e
            return saved.ifEmpty { null }
        }
        writeValue(keyKey(userId), fresh.orEmpty())
        return fresh
    }

    // ---- State and cache ----

    private fun publish(userId: String, value: LocalGroupState) {
        held.value = Held(userId, value)
    }

    /** Publishes what was saved for [userId], while nothing for them is in memory. */
    private suspend fun primeFromDisk(userId: String) {
        if (held.value?.userId == userId && ownOfficeIdFor(userId) != null) return
        val saved = readSnapshot(userId)
        // The own office is known from the disk before the network answers:
        // the picker leaves it out and choosing it means "back to mine" from the first second.
        if (saved != null && ownOfficeIdFor(userId) == null) ownOffice = userId to saved.office.id
        if (held.value?.userId == userId) return
        val primed = when {
            saved != null -> LocalGroupState.Ready(saved)
            readValue(keyKey(userId)) == "" -> LocalGroupState.NoRegion
            else -> return
        }
        if (held.value?.userId != userId) publish(userId, primed)
    }

    private suspend fun readSnapshot(userId: String): LocalGroupSnapshot? =
        readValue(snapshotKey(userId))?.let { raw ->
            runCatching { json.decodeFromString(LocalGroupSnapshot.serializer(), raw) }.getOrNull()
        }

    private suspend fun save(userId: String, snapshot: LocalGroupSnapshot) {
        val raw = runCatching { json.encodeToString(LocalGroupSnapshot.serializer(), snapshot) }.getOrNull() ?: return
        writeValue(snapshotKey(userId), raw)
    }

    private suspend fun readValue(key: String): String? =
        runCatching { db.keyValueQueries.selectById(key).awaitAsOneOrNull()?.value_ }.getOrNull()

    private suspend fun writeValue(key: String, value: String) {
        runCatching { db.keyValueQueries.insertOrReplace(key = key, value_ = value) }
    }

    private fun keyKey(userId: String) = "$CACHE_KEY_PREFIX.key.$userId"
    private fun snapshotKey(userId: String) = "$CACHE_KEY_PREFIX.snapshot.$userId"

    internal companion object {
        const val STATS = "local_office_stats"
        const val MONTHLY = "local_office_stats_monthly"
        const val ASSOCIATION_STATS = "association_stats"
        const val ASSOCIATION_MONTHLY = "association_stats_monthly"
        const val ASSOCIATION_ID = "association"
        const val ORG_CHART_MEMBERS = "org_chart_members"

        /** The org chart group of the board (`org_chart_groups.title`). */
        const val BOARD_GROUP = "consiglio"
        const val CHANGES = "members_registry_changes"
        const val RANKING = "view_leaderboard_regioni"
        const val CACHE_KEY_PREFIX = "local_group"
        const val MAX_EVENTS = 5
        const val MAX_DEALS = 10

        /** The window of [loadNewMembers]: the server's `members_new_7d`. */
        const val NEW_MEMBERS_DAYS = 7

        /** Registry ids per query: a filter with hundreds of `id =` would be too long a URL. */
        const val IDS_PER_QUERY = 50

        /**
         * `local_offices.region` -> region key, for a server without
         * `region_key`. The same map as the server's migration: change both
         * together.
         */
        private val officeRegionFallback: Map<String, String> = mapOf(
            "Basilicata e Puglia" to "Puglia",
            "Piemonte e Val d'Aosta" to "Piemonte",
            "Emilia Romagna" to "Emilia-Romagna",
            "Friuli Venezia Giulia" to "Friuli-Venezia Giulia",
            "Trentino Alto Adige" to "Trentino-Alto Adige",
        ).mapKeys { (name, _) -> TextNormalizer.normalize(name) }

        /** The office's region key: `region_key`, or derived from `region` on an older server. */
        fun officeRegionKey(office: LocalOfficeModel): String? {
            office.regionKey?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
            val mapped = officeRegionFallback[TextNormalizer.normalize(office.region)] ?: office.region
            return RegionKey.of(mapped)
        }

        /** The canonical regions that fold into [key]: "Piemonte" -> Piemonte and Valle d'Aosta. */
        fun statesOf(key: String): List<String> = ItalianRegions.all.filter { RegionKey.of(it) == key }

        /** True when [event] is the office's: its `local_office`, or placed in its region. */
        fun belongsTo(event: EventModel, officeId: String, key: String?): Boolean =
            event.localOfficeId == officeId || (key != null && RegionKey.of(event.position?.state) == key)

        /** The secretary first, then the co-secretaries by name. */
        fun sortAdmins(admins: List<LocalOfficeAdminModel>): List<LocalOfficeAdminModel> =
            admins.sortedWith(compareByDescending<LocalOfficeAdminModel> { it.isTheOfficer }.thenBy { it.name.lowercase() })

        /** The active links, no sections, in their order. */
        fun activeLinks(rows: List<LocalOfficeLinktreeRowModel>): List<LocalOfficeLinktreeRowModel> =
            rows.filter { it.active && it.kind != "section" && it.url.isNotBlank() }.sortedBy { it.sortOrder }

        fun isAdmin(user: UserModel, admins: List<LocalOfficeAdminModel>): Boolean =
            Powers.has(user.powers, Powers.SUPER) || admins.any { it.user == user.id }

        /** `local_office = id || position.state = ...`, in parentheses. */
        fun regionFilter(officeId: String, key: String?): String {
            val states = key?.let(::statesOf).orEmpty().map { "position.state = ${quote(it)}" }
            return (listOf("local_office = ${quote(officeId)}") + states).joinToString(" || ", "(", ")")
        }

        /** A PocketBase filter string literal; quotes and backslashes are dropped. */
        fun quote(value: String): String = "\"" + value.replace("\\", "").replace("\"", "") + "\""

        /** [block]'s result, or [fallback] when it fails: for the pieces the page can do without. */
        suspend fun <T> optional(fallback: T, block: suspend () -> T): T = try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            fallback
        }
    }
}
