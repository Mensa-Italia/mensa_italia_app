package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.LocalOfficesApi
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeLinkRecord
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import it.mensa.shared.model.LocalOfficeTestDateRecord
import it.mensa.shared.model.SigModel
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class LocalOfficesRepository(private val api: LocalOfficesApi) {

    // --- All-offices index ---
    private val _allOffices = MutableStateFlow<List<LocalOfficeModel>>(emptyList())

    // --- Per-office caches keyed by office id ---
    private val _linktree = MutableStateFlow<Map<String, List<LocalOfficeLinktreeRowModel>>>(emptyMap())
    private val _admins = MutableStateFlow<Map<String, List<LocalOfficeAdminModel>>>(emptyMap())
    private val _assistants = MutableStateFlow<Map<String, List<LocalOfficeAssistantModel>>>(emptyMap())
    private val _testDates = MutableStateFlow<Map<String, List<LocalOfficeTestDateModel>>>(emptyMap())
    private val _events = MutableStateFlow<Map<String, List<EventModel>>>(emptyMap())
    private val _sigs = MutableStateFlow<Map<String, List<SigModel>>>(emptyMap())

    // ── All-offices index ─────────────────────────────────────────────────────

    fun observeAllOffices(): Flow<List<LocalOfficeModel>> = _allOffices.asStateFlow()

    /// Throws on network failure — Swift bridges this as a throwing async call.
    /// kotlin.Result intentionally NOT used here because it doesn't survive K/N → Swift
    /// interop (Swift would receive an opaque box that won't cast back to T).
    suspend fun refreshAllOffices() {
        _allOffices.value = api.listAll()
    }

    /// Public-area refresh: hits the unauthenticated `view_local_office`
    /// endpoint so the offices list works pre-login. Same cache as the
    /// authenticated path.
    suspend fun refreshAllOfficesPublic() {
        _allOffices.value = api.listAllPublic()
    }

    /// Public-area lookup by id — usa la view pubblica.
    /// Cache-or-fetch: se l'office e' gia' in cache (popolato dalla list)
    /// ritorna quello, altrimenti rifresca la view pubblica.
    suspend fun officeByIdPublic(id: String): LocalOfficeModel? {
        val cached = _allOffices.value.firstOrNull { it.id == id }
        if (cached != null) return cached
        val fetched = api.byIdPublic(id)
        if (fetched != null) {
            // Merge nel cache cosi' i subscriber dell'osservazione lo vedono.
            _allOffices.value = (_allOffices.value + fetched).distinctBy { it.id }
        }
        return fetched
    }

    /// Returns the cached office for a slug if known, otherwise fetches it from
    /// the network (without populating the full list cache).
    suspend fun officeBySlug(slug: String): LocalOfficeModel? {
        val cached = _allOffices.value.firstOrNull { it.slug == slug }
        if (cached != null) return cached
        return api.bySlug(slug)
    }

    /// Cache-or-refresh lookup by PocketBase id. Used by the in-app list →
    /// detail navigation path, where the id is always known and unambiguous
    /// (slug-based lookup is fragile when slugs are missing or duplicated).
    suspend fun officeById(id: String): LocalOfficeModel? {
        val cached = _allOffices.value.firstOrNull { it.id == id }
        if (cached != null) return cached
        refreshAllOffices()
        return _allOffices.value.firstOrNull { it.id == id }
    }

    // ── Per-office linktree ───────────────────────────────────────────────────

    fun observeLinktree(officeId: String): Flow<List<LocalOfficeLinktreeRowModel>> =
        _linktree.map { it[officeId] ?: emptyList() }

    suspend fun refreshLinktreeByOffice(officeId: String) {
        val rows = api.linktreeByOffice(officeId)
        _linktree.value = _linktree.value + (officeId to rows)
    }

    /// Entry path used when navigating from a deep link: resolves slug → office,
    /// caches both the office and its linktree in one shot, returns the office.
    suspend fun loadBySlug(slug: String): LocalOfficeModel? {
        val office = officeBySlug(slug) ?: return null
        val rows = api.linktreeBySlug(slug)
        _linktree.value = _linktree.value + (office.id to rows)
        return office
    }

    // ── Per-office admins / assistants ────────────────────────────────────────

    fun observeAdmins(officeId: String): Flow<List<LocalOfficeAdminModel>> =
        _admins.map { it[officeId] ?: emptyList() }

    suspend fun refreshAdmins(officeId: String) {
        val items = api.adminsByOffice(officeId)
        _admins.value = _admins.value + (officeId to items)
    }

    fun observeAssistants(officeId: String): Flow<List<LocalOfficeAssistantModel>> =
        _assistants.map { it[officeId] ?: emptyList() }

    suspend fun refreshAssistants(officeId: String) {
        val items = api.assistantsByOffice(officeId)
        _assistants.value = _assistants.value + (officeId to items)
    }

    // ── Per-office test dates (upcoming only) ─────────────────────────────────

    fun observeUpcomingTestDates(officeId: String): Flow<List<LocalOfficeTestDateModel>> =
        _testDates.map { it[officeId] ?: emptyList() }

    suspend fun refreshUpcomingTestDates(officeId: String) {
        val items = api.upcomingTestDatesByOffice(officeId)
        _testDates.value = _testDates.value + (officeId to items)
    }

    // ── Per-office events / sigs ──────────────────────────────────────────────

    fun observeEvents(officeId: String): Flow<List<EventModel>> =
        _events.map { it[officeId] ?: emptyList() }

    suspend fun refreshEvents(officeId: String) {
        val items = api.eventsByOffice(officeId)
        _events.value = _events.value + (officeId to items)
    }

    fun observeSigs(officeId: String): Flow<List<SigModel>> =
        _sigs.map { it[officeId] ?: emptyList() }

    suspend fun refreshSigs(officeId: String) {
        val items = api.sigsByOffice(officeId)
        _sigs.value = _sigs.value + (officeId to items)
    }

    // ── Test-date writes ──────────────────────────────────────────────────────

    /// Throws on failure. Refreshes the upcoming-test-dates cache for the office
    /// so observers re-render without an explicit pull.
    suspend fun createTestDate(
        officeId: String,
        record: LocalOfficeTestDateRecord,
    ): LocalOfficeTestDateRecord {
        val created = api.createTestDate(record.copy(localOffice = officeId))
        refreshUpcomingTestDates(officeId)
        return created
    }

    suspend fun updateTestDate(
        officeId: String,
        id: String,
        patch: JsonObject,
    ): LocalOfficeTestDateRecord {
        val updated = api.updateTestDate(id, patch)
        refreshUpcomingTestDates(officeId)
        return updated
    }

    suspend fun deleteTestDate(officeId: String, id: String) {
        api.deleteTestDate(id)
        refreshUpcomingTestDates(officeId)
    }

    // ── Linktree-entry writes ─────────────────────────────────────────────────

    /// Throws on failure. Refreshes the linktree cache for the office
    /// so observers re-render without an explicit pull.
    suspend fun createLink(
        officeId: String,
        record: LocalOfficeLinkRecord,
    ): LocalOfficeLinkRecord {
        val created = api.createLink(record.copy(localOffice = officeId))
        refreshLinktreeByOffice(officeId)
        return created
    }

    suspend fun updateLink(
        officeId: String,
        id: String,
        patch: JsonObject,
    ): LocalOfficeLinkRecord {
        val updated = api.updateLink(id, patch)
        refreshLinktreeByOffice(officeId)
        return updated
    }

    suspend fun deleteLink(officeId: String, id: String) {
        api.deleteLink(id)
        refreshLinktreeByOffice(officeId)
    }

    // ── Swift-friendly typed update wrappers ──────────────────────────────────
    // Swift can't ergonomically build a `JsonObject`, so these helpers accept
    // optional typed fields and construct the patch internally. Any non-null
    // argument is included in the PATCH body; null means "leave unchanged".

    suspend fun updateTestDateFields(
        officeId: String,
        id: String,
        date: kotlinx.datetime.Instant? = null,
        location: String? = null,
        notes: String? = null,
        maxParticipants: Int? = null,
        assistants: List<String>? = null,
    ): LocalOfficeTestDateRecord {
        val patch = kotlinx.serialization.json.buildJsonObject {
            date?.let { put("date", kotlinx.serialization.json.JsonPrimitive(it.toString())) }
            location?.let { put("location", kotlinx.serialization.json.JsonPrimitive(it)) }
            notes?.let { put("notes", kotlinx.serialization.json.JsonPrimitive(it)) }
            maxParticipants?.let { put("max_participants", kotlinx.serialization.json.JsonPrimitive(it)) }
            assistants?.let { ids ->
                put("assistants", kotlinx.serialization.json.JsonArray(
                    ids.map { kotlinx.serialization.json.JsonPrimitive(it) }
                ))
            }
        }
        return updateTestDate(officeId, id, patch)
    }

    /// Swift-friendly create wrapper for linktree entries. The K/N bridge for
    /// Kotlin data-class initializers with `Boolean` defaults occasionally
    /// dropped the value (sent `active=false` even when Swift passed `true`).
    /// Building the record inside Kotlin avoids that footgun.
    suspend fun createLinkFromFields(
        officeId: String,
        kind: String,
        parent: String,
        title: String,
        url: String,
        icon: String,
        sortOrder: Int,
        active: Boolean,
    ): LocalOfficeLinkRecord {
        val record = LocalOfficeLinkRecord(
            id = "",
            localOffice = officeId,
            kind = kind,
            parent = parent,
            title = title,
            url = url,
            icon = icon,
            sortOrder = sortOrder,
            active = active,
        )
        return createLink(officeId, record)
    }

    /// Mirror of `createLinkFromFields` for test-date creation. Keeps the
    /// Swift call site free of Kotlin data-class constructor bridging.
    suspend fun createTestDateFromFields(
        officeId: String,
        date: kotlinx.datetime.Instant,
        location: String,
        notes: String,
        maxParticipants: Int,
        assistants: List<String>,
    ): LocalOfficeTestDateRecord {
        val record = LocalOfficeTestDateRecord(
            id = "",
            localOffice = officeId,
            date = date,
            location = location,
            notes = notes,
            maxParticipants = maxParticipants,
            assistants = assistants,
        )
        return createTestDate(officeId, record)
    }

    suspend fun updateLinkFields(
        officeId: String,
        id: String,
        kind: String? = null,
        parent: String? = null,
        title: String? = null,
        url: String? = null,
        icon: String? = null,
        sortOrder: Int? = null,
        active: Boolean? = null,
    ): LocalOfficeLinkRecord {
        val patch = kotlinx.serialization.json.buildJsonObject {
            kind?.let { put("kind", kotlinx.serialization.json.JsonPrimitive(it)) }
            parent?.let { put("parent", kotlinx.serialization.json.JsonPrimitive(it)) }
            title?.let { put("title", kotlinx.serialization.json.JsonPrimitive(it)) }
            url?.let { put("url", kotlinx.serialization.json.JsonPrimitive(it)) }
            icon?.let { put("icon", kotlinx.serialization.json.JsonPrimitive(it)) }
            sortOrder?.let { put("sort_order", kotlinx.serialization.json.JsonPrimitive(it)) }
            active?.let { put("active", kotlinx.serialization.json.JsonPrimitive(it)) }
        }
        return updateLink(officeId, id, patch)
    }

    // ── Batch refresh ─────────────────────────────────────────────────────────

    /// One-shot batch refresh for a single office — fires all per-office
    /// refreshes in parallel and waits for all to complete. Individual failures
    /// are propagated; the caller's `try` decides whether to log them.
    suspend fun refreshAllForOffice(officeId: String): Unit = coroutineScope {
        val linktreeJob = async { refreshLinktreeByOffice(officeId) }
        val adminsJob = async { refreshAdmins(officeId) }
        val assistantsJob = async { refreshAssistants(officeId) }
        val testDatesJob = async { refreshUpcomingTestDates(officeId) }
        val eventsJob = async { refreshEvents(officeId) }
        val sigsJob = async { refreshSigs(officeId) }

        linktreeJob.await()
        adminsJob.await()
        assistantsJob.await()
        testDatesJob.await()
        eventsJob.await()
        sigsJob.await()
    }
}
