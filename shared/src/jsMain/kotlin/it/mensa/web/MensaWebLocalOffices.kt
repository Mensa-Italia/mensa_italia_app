@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.api.endpoints.LocalOfficesApi
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeLinkRecord
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import it.mensa.shared.model.LocalOfficeTestDateRecord
import it.mensa.shared.repository.LocalOfficesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlinx.datetime.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class LocalOfficeLinkCreatePayload(
    val officeId: String,
    val kind: String,       // "section" | "link"
    val parentId: String,   // "" = root
    val title: String,
    val url: String,
    val icon: String,
    val sortOrder: Int,
)

@JsExport
class LocalOfficeLinkUpdatePayload(
    val kind: String,
    val parentId: String,
    val title: String,
    val url: String,
    val icon: String,
    val sortOrder: Int,
)

@JsExport
class LocalOfficeTestDateCreatePayload(
    val officeId: String,
    val dateMs: Double,          // epoch ms
    val location: String,
    val notes: String,
    val maxParticipants: Int,
    val assistants: Array<String>,
)

@JsExport
class LocalOfficeTestDateUpdatePayload(
    val dateMs: Double,
    val location: String,
    val notes: String,
    val maxParticipants: Int,
    val assistants: Array<String>,
)

@JsExport
class MensaWebLocalOffices internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: LocalOfficesRepository get() = KoinPlatform.getKoin().get()
    private val api: LocalOfficesApi get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (offices: Array<MensaWebLocalOffice>) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.observeAllOffices().collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return { job.cancel() }
    }

    fun refresh(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.refreshAllOffices()
    }

    fun bySlug(slug: String): Promise<MensaWebLocalOffice?> = scope.promise {
        sdk.awaitReady()
        repo.officeBySlug(slug)?.toJs()
    }

    /** Subscribe to admins + assistants merged into a single team view. */
    fun subscribeTeam(officeId: String, callback: (team: Array<MensaWebLocalOfficeMember>) -> Unit): () -> Unit {
        // Eagerly refresh admins/assistants on subscribe so the in-memory
        // StateFlow has data before the first emission lands. Failures are
        // swallowed (a stale empty list is preferable to a thrown promise).
        val adminsJob: Job = scope.launch {
            sdk.awaitReady()
            runCatching { repo.refreshAdmins(officeId) }
            runCatching { repo.refreshAssistants(officeId) }
        }
        val emittedAdmins = mutableListOf<MensaWebLocalOfficeMember>()
        val emittedAssistants = mutableListOf<MensaWebLocalOfficeMember>()
        val a1: Job = scope.launch {
            sdk.awaitReady()
            repo.observeAdmins(officeId).collect { list ->
                emittedAdmins.clear()
                emittedAdmins.addAll(list.map { it.toJs() })
                emit(callback, emittedAdmins, emittedAssistants)
            }
        }
        val a2: Job = scope.launch {
            sdk.awaitReady()
            repo.observeAssistants(officeId).collect { list ->
                emittedAssistants.clear()
                emittedAssistants.addAll(list.map { it.toJs() })
                emit(callback, emittedAdmins, emittedAssistants)
            }
        }
        return {
            adminsJob.cancel()
            a1.cancel()
            a2.cancel()
        }
    }

    private fun emit(
        callback: (team: Array<MensaWebLocalOfficeMember>) -> Unit,
        admins: List<MensaWebLocalOfficeMember>,
        assistants: List<MensaWebLocalOfficeMember>,
    ) {
        callback((admins + assistants).toTypedArray())
    }

    fun subscribeLinktree(
        officeId: String,
        callback: (rows: Array<MensaWebLocalOfficeLink>) -> Unit,
    ): () -> Unit {
        val refresh: Job = scope.launch {
            sdk.awaitReady()
            runCatching { repo.refreshLinktreeByOffice(officeId) }
        }
        val sub: Job = scope.launch {
            sdk.awaitReady()
            repo.observeLinktree(officeId).collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return {
            refresh.cancel()
            sub.cancel()
        }
    }

    // ── Link CRUD ────────────────────────────────────────────────────────────

    fun createLink(payload: LocalOfficeLinkCreatePayload): Promise<MensaWebLocalOfficeLink> = scope.promise {
        sdk.awaitReady()
        api.createLink(
            LocalOfficeLinkRecord(
                localOffice = payload.officeId,
                kind = payload.kind,
                parent = payload.parentId,
                title = payload.title,
                url = payload.url,
                icon = payload.icon,
                sortOrder = payload.sortOrder,
                active = true,
            )
        ).toJsLink()
    }

    fun updateLink(id: String, payload: LocalOfficeLinkUpdatePayload): Promise<MensaWebLocalOfficeLink> = scope.promise {
        sdk.awaitReady()
        val patch = buildJsonObject {
            put("kind", payload.kind)
            put("parent", payload.parentId)
            put("title", payload.title)
            put("url", payload.url)
            put("icon", payload.icon)
            put("sort_order", payload.sortOrder)
        }
        api.updateLink(id, patch).toJsLink()
    }

    fun deleteLink(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        api.deleteLink(id)
    }

    // ── Test dates CRUD ──────────────────────────────────────────────────────

    fun upcomingTestDates(officeId: String): Promise<Array<MensaWebTestDate>> = scope.promise {
        sdk.awaitReady()
        api.upcomingTestDatesByOffice(officeId).map { it.toJs() }.toTypedArray()
    }

    fun assistants(officeId: String): Promise<Array<MensaWebLocalOfficeMember>> = scope.promise {
        sdk.awaitReady()
        api.assistantsByOffice(officeId).map { it.toJs() }.toTypedArray()
    }

    fun createTestDate(payload: LocalOfficeTestDateCreatePayload): Promise<MensaWebTestDate> = scope.promise {
        sdk.awaitReady()
        api.createTestDate(
            LocalOfficeTestDateRecord(
                localOffice = payload.officeId,
                date = Instant.fromEpochMilliseconds(payload.dateMs.toLong()),
                location = payload.location,
                notes = payload.notes,
                maxParticipants = payload.maxParticipants,
                assistants = payload.assistants.toList(),
            )
        ).toJsTestDate()
    }

    fun updateTestDate(id: String, payload: LocalOfficeTestDateUpdatePayload): Promise<MensaWebTestDate> = scope.promise {
        sdk.awaitReady()
        val patch = buildJsonObject {
            put("date", Instant.fromEpochMilliseconds(payload.dateMs.toLong()).toString())
            put("location", payload.location)
            put("notes", payload.notes)
            put("max_participants", payload.maxParticipants)
            put("assistants", buildJsonArray {
                payload.assistants.forEach { add(JsonPrimitive(it)) }
            })
        }
        api.updateTestDate(id, patch).toJsTestDate()
    }

    fun deleteTestDate(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        api.deleteTestDate(id)
    }
}

@JsExport
data class MensaWebLocalOffice(
    val id: String,
    val slug: String,
    val name: String,
    val kicker: String,
    val bio: String,
    val region: String,
    val coverUrl: String,
)

/**
 * Merged admin/assistant DTO. `role` is `"officer"` for the office head,
 * `"admin"` for the other admins, and `"assistant"` for the assistants —
 * mirrors the iOS member-list rendering rule.
 */
@JsExport
data class MensaWebLocalOfficeMember(
    val id: String,
    val role: String,
    val name: String,
    val email: String,
    val avatarUrl: String,
    val region: String,
    val officeId: String,
    val officeName: String,
)

@JsExport
data class MensaWebLocalOfficeLink(
    val id: String,
    val officeId: String,
    val kind: String,        // "section" | "link"
    val parentId: String,    // "" = root
    val title: String,
    val url: String,
    val icon: String,
    val sortOrder: Int,
)

internal fun LocalOfficeModel.toJs(): MensaWebLocalOffice {
    val base = MensaSdk.apiBaseUrl()
    val cover = if (image.isNotBlank()) "$base/api/files/local_offices/$id/$image" else ""
    return MensaWebLocalOffice(
        id = id,
        slug = slug,
        name = name,
        kicker = "",
        bio = bio,
        region = region,
        coverUrl = cover,
    )
}

internal fun LocalOfficeAdminModel.toJs(): MensaWebLocalOfficeMember {
    val base = MensaSdk.apiBaseUrl()
    val avatar = if (image.isNotBlank()) "$base/api/files/_pb_users_auth_/$user/$image" else ""
    return MensaWebLocalOfficeMember(
        id = id,
        role = if (isTheOfficer) "officer" else "admin",
        name = name,
        email = email,
        avatarUrl = avatar,
        region = region,
        officeId = localOffice,
        officeName = localOfficeName,
    )
}

internal fun LocalOfficeAssistantModel.toJs(): MensaWebLocalOfficeMember {
    val base = MensaSdk.apiBaseUrl()
    val avatar = if (image.isNotBlank()) "$base/api/files/_pb_users_auth_/$user/$image" else ""
    return MensaWebLocalOfficeMember(
        id = id,
        role = "assistant",
        name = name,
        email = email,
        avatarUrl = avatar,
        region = region,
        officeId = localOffice,
        officeName = localOfficeName,
    )
}

internal fun LocalOfficeLinktreeRowModel.toJs(): MensaWebLocalOfficeLink = MensaWebLocalOfficeLink(
    id = id,
    officeId = localOffice,
    kind = kind,
    parentId = parent,
    title = title,
    url = url,
    icon = icon,
    sortOrder = sortOrder,
)

@JsExport
data class MensaWebTestDate(
    val id: String,
    val officeId: String,
    val officeName: String,
    val region: String,
    val dateMs: Double,
    val location: String,
    val notes: String,
    val maxParticipants: Int,
    val assistants: Array<String>,
)

internal fun LocalOfficeTestDateModel.toJs(): MensaWebTestDate = MensaWebTestDate(
    id = id,
    officeId = localOffice,
    officeName = localOfficeName,
    region = region,
    dateMs = date.toEpochMilliseconds().toDouble(),
    location = location,
    notes = notes,
    maxParticipants = maxParticipants,
    assistants = assistants.toTypedArray(),
)

/** Maps a raw `LocalOfficeLinkRecord` (from CRUD responses) back to the public shape. */
internal fun LocalOfficeLinkRecord.toJsLink(): MensaWebLocalOfficeLink = MensaWebLocalOfficeLink(
    id = id,
    officeId = localOffice,
    kind = kind,
    parentId = parent,
    title = title,
    url = url,
    icon = icon,
    sortOrder = sortOrder,
)

/** Maps a raw `LocalOfficeTestDateRecord` (from CRUD responses) back to the public shape. */
internal fun LocalOfficeTestDateRecord.toJsTestDate(): MensaWebTestDate = MensaWebTestDate(
    id = id,
    officeId = localOffice,
    officeName = "",  // raw record has no join — office name unavailable here
    region = "",
    dateMs = date.toEpochMilliseconds().toDouble(),
    location = location,
    notes = notes,
    maxParticipants = maxParticipants,
    assistants = assistants.toTypedArray(),
)
