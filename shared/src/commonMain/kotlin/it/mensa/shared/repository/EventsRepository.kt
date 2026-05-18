package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.FilePart
import it.mensa.shared.api.endpoints.EventSchedulesApi
import it.mensa.shared.api.endpoints.EventsApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.EventScheduleModel
import it.mensa.shared.model.LocationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

/**
 * Draft used to create or update an Event together with its nested schedules.
 * Mirrors the Flutter `createEvent` / `updateEvent` shape so iOS callers can
 * round-trip identical data through the Kotlin shared module.
 */
data class EventDraft(
    val name: String,
    val description: String,
    val infoLink: String,
    val whenStart: Instant,
    val whenEnd: Instant,
    val isNational: Boolean,
    val isSpot: Boolean,
    val ownerId: String,
    val positionId: String?,        // null if online
    val imageBytes: ByteArray?,     // null = keep existing image (update) / no image (create)
    val imageFilename: String?,     // e.g. "cover.png"
    val imageContentType: String? = "image/png",
    val schedules: List<ScheduleDraft> = emptyList(),
)

/**
 * Draft for a nested events_schedule row. Diff semantics on update:
 *  - id == null               → create
 *  - id startsWith "DELETE:"  → delete the id after the colon
 *  - else                     → update events_schedule/{id}
 */
data class ScheduleDraft(
    val id: String?,
    val title: String,
    val description: String,
    val infoLink: String,
    val whenStart: Instant,
    val whenEnd: Instant,
    val maxExternalGuests: Int,
    val price: Double,
    val isSubscriptable: Boolean,
)

class EventsRepository(
    private val api: EventsApi,
    private val db: MensaDatabase,
    private val json: Json,
    private val schedulesApi: EventSchedulesApi? = null,
) {
    fun observeAll(): Flow<List<EventModel>> =
        db.eventQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel(json) } }

    suspend fun refresh(filter: String? = null, sort: String = "when_end") {
        val items = api.list(filter = filter, sort = sort)
        db.transaction {
            db.eventQueries.deleteAll()
            items.forEach { e -> upsertRow(e) }
        }
    }

    suspend fun firstSnapshot(): List<EventModel> = observeAll().first()

    suspend fun fetchPublicEvents(): List<EventModel> = api.listPublic()

    suspend fun getById(id: String): EventModel? {
        val row = db.eventQueries.selectById(id).awaitAsOneOrNull() ?: return null
        return row.toModel(json)
    }

    /** Create an event with optional image cover and nested schedules. */
    suspend fun create(draft: EventDraft): EventModel {
        val fields = baseFields(draft, includeOwner = true)
        val files = imageFiles(draft)
        val created: EventModel = api.pb.createMultipart("events", fields, files)

        schedulesApi?.let { sApi ->
            draft.schedules.forEach { s ->
                sApi.create(s.toModel(eventId = created.id))
            }
        }

        upsertRow(created)
        return created
    }

    /** Update an event. Image is sent only if [EventDraft.imageBytes] is non-null. */
    suspend fun update(id: String, draft: EventDraft): EventModel {
        val fields = baseFields(draft, includeOwner = false)
        val files = imageFiles(draft)
        val updated: EventModel = api.pb.updateMultipart("events", id, fields, files)

        schedulesApi?.let { sApi ->
            draft.schedules.forEach { s ->
                when {
                    s.id == null -> sApi.create(s.toModel(eventId = id))
                    s.id.startsWith("DELETE:") -> {
                        try {
                            sApi.delete(s.id.substringAfter("DELETE:"))
                        } catch (_: Throwable) {
                            // mirror Flutter try { } catch (_) {}
                        }
                    }
                    else -> sApi.update(s.id, s.toModel(eventId = id, idForBody = s.id))
                }
            }
        }

        upsertRow(updated)
        return updated
    }

    suspend fun delete(id: String) {
        api.delete(id)
        db.eventQueries.deleteById(id)
    }

    // --- helpers -------------------------------------------------------------

    private fun baseFields(draft: EventDraft, includeOwner: Boolean): Map<String, Any?> {
        val map = linkedMapOf<String, Any?>(
            "name" to draft.name,
            "description" to draft.description,
            "info_link" to draft.infoLink,
            "when_start" to draft.whenStart.toString(),
            "when_end" to draft.whenEnd.toString(),
            "is_national" to draft.isNational,
            "is_spot" to draft.isSpot,
        )
        if (includeOwner) map["owner"] = draft.ownerId
        if (draft.positionId != null) map["position"] = draft.positionId
        return map
    }

    private fun imageFiles(draft: EventDraft): List<FilePart> {
        val bytes = draft.imageBytes ?: return emptyList()
        val filename = draft.imageFilename ?: "image.png"
        val contentType = draft.imageContentType ?: "image/png"
        return listOf(FilePart("image", filename, contentType, bytes))
    }

    private suspend fun upsertRow(e: EventModel) {
        db.eventQueries.insertOrReplace(
            id = e.id,
            name = e.name,
            description = e.description,
            image = e.image,
            infoLink = e.infoLink,
            contact = e.contact,
            whenStart = e.whenStart.toEpochMilliseconds(),
            whenEnd = e.whenEnd.toEpochMilliseconds(),
            owner = e.owner,
            isNational = if (e.isNational) 1L else 0L,
            isSpot = if (e.isSpot) 1L else 0L,
            isPublic = if (e.isPublic) 1L else 0L,
            bookingLink = e.bookingLink.ifBlank { null },
            positionJson = e.position?.let {
                json.encodeToString(LocationModel.serializer(), it)
            },
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
    }
}

private fun ScheduleDraft.toModel(eventId: String, idForBody: String? = null): EventScheduleModel =
    EventScheduleModel(
        id = idForBody,
        title = title,
        event = eventId,
        description = description,
        whenStart = whenStart,
        whenEnd = whenEnd,
        maxExternalGuests = maxExternalGuests,
        price = price,
        infoLink = infoLink,
        isSubscriptable = isSubscriptable,
    )
