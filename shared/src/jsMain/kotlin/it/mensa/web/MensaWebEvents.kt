@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.api.endpoints.EventSchedulesApi
import it.mensa.shared.api.endpoints.EventsApi
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.EventScheduleModel
import it.mensa.shared.repository.EventsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlinx.datetime.Instant
import org.koin.mp.KoinPlatform
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import kotlin.js.Promise

@JsExport
class EventCreatePayload(
    val name: String,
    val description: String,
    val image: String,       // filename already uploaded, or "" — multipart upload is a future wave
    val infoLink: String,
    val bookingLink: String,
    val startsMs: Double,    // epoch ms as JS Number
    val endsMs: Double,
    val isNational: Boolean,
    val isOnline: Boolean,
    val isPublic: Boolean,
    val isSpot: Boolean,
    val contact: String,
    val region: String,
    val positionId: String?, // null if online
    val ownerId: String,
)

@JsExport
class EventUpdatePayload(
    val name: String,
    val description: String,
    val image: String,
    val infoLink: String,
    val bookingLink: String,
    val startsMs: Double,
    val endsMs: Double,
    val isNational: Boolean,
    val isOnline: Boolean,
    val isPublic: Boolean,
    val isSpot: Boolean,
    val contact: String,
    val region: String,
    val positionId: String?,
    val ownerId: String,
)

@JsExport
class EventScheduleCreatePayload(
    val title: String,
    val eventId: String,
    val description: String,
    val startsMs: Double,
    val endsMs: Double,
    val maxExternalGuests: Int,
    val price: Double,
    val infoLink: String,
    val isSubscriptable: Boolean,
)

@JsExport
class EventScheduleUpdatePayload(
    val title: String,
    val description: String,
    val startsMs: Double,
    val endsMs: Double,
    val maxExternalGuests: Int,
    val price: Double,
    val infoLink: String,
    val isSubscriptable: Boolean,
)

@JsExport
class MensaWebEvents internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: EventsRepository get() = KoinPlatform.getKoin().get()
    private val api: EventsApi get() = KoinPlatform.getKoin().get()
    private val schedulesApi: EventSchedulesApi get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (events: Array<MensaWebEvent>) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.observeAll().collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return { job.cancel() }
    }

    fun refresh(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.refresh()
    }

    fun getById(id: String): Promise<MensaWebEvent?> = scope.promise {
        sdk.awaitReady()
        repo.getById(id)?.toJs()
    }

    fun create(payload: EventCreatePayload): Promise<MensaWebEvent> = scope.promise {
        sdk.awaitReady()
        // TODO: multipart cover upload (wave successiva) — image is filename only, not bytes
        val body = EventModel(
            name = payload.name,
            description = payload.description,
            image = payload.image,
            infoLink = payload.infoLink,
            bookingLink = payload.bookingLink,
            whenStart = Instant.fromEpochMilliseconds(payload.startsMs.toLong()),
            whenEnd = Instant.fromEpochMilliseconds(payload.endsMs.toLong()),
            isNational = payload.isNational,
            isSpot = payload.isSpot,
            isPublic = payload.isPublic,
            contact = payload.contact,
            ownerId = payload.ownerId,
            positionId = payload.positionId,
        )
        api.create(body).toJs()
    }

    fun update(id: String, payload: EventUpdatePayload): Promise<MensaWebEvent> = scope.promise {
        sdk.awaitReady()
        // TODO: multipart cover upload (wave successiva) — image is filename only, not bytes
        val body = EventModel(
            id = id,
            name = payload.name,
            description = payload.description,
            image = payload.image,
            infoLink = payload.infoLink,
            bookingLink = payload.bookingLink,
            whenStart = Instant.fromEpochMilliseconds(payload.startsMs.toLong()),
            whenEnd = Instant.fromEpochMilliseconds(payload.endsMs.toLong()),
            isNational = payload.isNational,
            isSpot = payload.isSpot,
            isPublic = payload.isPublic,
            contact = payload.contact,
            ownerId = payload.ownerId,
            positionId = payload.positionId,
        )
        api.update(id, body).toJs()
    }

    /**
     * Creates an event with a browser File as cover image. The File is read to
     * ByteArray via Uint8Array and sent via multipart/form-data.
     * Uses EventsRepository.create(EventDraft) which calls createMultipart internally.
     */
    fun createMultipart(payload: EventCreatePayload, coverFile: org.w3c.files.File): Promise<MensaWebEvent> = scope.promise {
        sdk.awaitReady()
        val bytes = jsFileToByteArray(coverFile)
        val draft = it.mensa.shared.repository.EventDraft(
            name = payload.name,
            description = payload.description,
            infoLink = payload.infoLink,
            whenStart = Instant.fromEpochMilliseconds(payload.startsMs.toLong()),
            whenEnd = Instant.fromEpochMilliseconds(payload.endsMs.toLong()),
            isNational = payload.isNational,
            isSpot = payload.isSpot,
            ownerId = payload.ownerId,
            positionId = payload.positionId,
            imageBytes = bytes,
            imageFilename = coverFile.name,
            imageContentType = coverFile.type.ifBlank { "image/jpeg" },
        )
        repo.create(draft).toJs()
    }

    /**
     * Updates an event with a new cover image via multipart/form-data.
     */
    fun updateMultipart(id: String, payload: EventUpdatePayload, coverFile: org.w3c.files.File): Promise<MensaWebEvent> = scope.promise {
        sdk.awaitReady()
        val bytes = jsFileToByteArray(coverFile)
        val draft = it.mensa.shared.repository.EventDraft(
            name = payload.name,
            description = payload.description,
            infoLink = payload.infoLink,
            whenStart = Instant.fromEpochMilliseconds(payload.startsMs.toLong()),
            whenEnd = Instant.fromEpochMilliseconds(payload.endsMs.toLong()),
            isNational = payload.isNational,
            isSpot = payload.isSpot,
            ownerId = payload.ownerId,
            positionId = payload.positionId,
            imageBytes = bytes,
            imageFilename = coverFile.name,
            imageContentType = coverFile.type.ifBlank { "image/jpeg" },
        )
        repo.update(id, draft).toJs()
    }

    fun delete(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        api.delete(id)
    }

    // ── Schedules sub-namespace ──────────────────────────────────────────────

    fun listSchedules(eventId: String): Promise<Array<MensaWebEventSchedule>> = scope.promise {
        sdk.awaitReady()
        schedulesApi.listForEvent(eventId).map { it.toJs() }.toTypedArray()
    }

    fun createSchedule(payload: EventScheduleCreatePayload): Promise<MensaWebEventSchedule> = scope.promise {
        sdk.awaitReady()
        schedulesApi.create(
            EventScheduleModel(
                title = payload.title,
                event = payload.eventId,
                description = payload.description,
                whenStart = Instant.fromEpochMilliseconds(payload.startsMs.toLong()),
                whenEnd = Instant.fromEpochMilliseconds(payload.endsMs.toLong()),
                maxExternalGuests = payload.maxExternalGuests,
                price = payload.price,
                infoLink = payload.infoLink,
                isSubscriptable = payload.isSubscriptable,
            )
        ).toJs()
    }

    fun updateSchedule(id: String, payload: EventScheduleUpdatePayload): Promise<MensaWebEventSchedule> = scope.promise {
        sdk.awaitReady()
        schedulesApi.update(
            id,
            EventScheduleModel(
                id = id,
                title = payload.title,
                description = payload.description,
                whenStart = Instant.fromEpochMilliseconds(payload.startsMs.toLong()),
                whenEnd = Instant.fromEpochMilliseconds(payload.endsMs.toLong()),
                maxExternalGuests = payload.maxExternalGuests,
                price = payload.price,
                infoLink = payload.infoLink,
                isSubscriptable = payload.isSubscriptable,
            )
        ).toJs()
    }

    fun deleteSchedule(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        schedulesApi.delete(id)
    }
}

@JsExport
data class MensaWebEventSchedule(
    val id: String,
    val title: String,
    val eventId: String,
    val description: String,
    val startsMs: Double,
    val endsMs: Double,
    val maxExternalGuests: Int,
    val price: Double,
    val infoLink: String,
    val isSubscriptable: Boolean,
)

internal fun EventScheduleModel.toJs(): MensaWebEventSchedule = MensaWebEventSchedule(
    id = id ?: "",
    title = title,
    eventId = event ?: "",
    description = description,
    startsMs = whenStart.toEpochMilliseconds().toDouble(),
    endsMs = whenEnd.toEpochMilliseconds().toDouble(),
    maxExternalGuests = maxExternalGuests,
    price = price,
    infoLink = infoLink,
    isSubscriptable = isSubscriptable,
)

/**
 * JS-facing event mirror. Image references on PocketBase are bare filenames; we
 * compose the public URL here via `${apiBaseUrl}/api/files/events/{id}/{file}`
 * so consumers can drop the URL into `<img>` without further plumbing.
 *
 * `online` is `true` whenever the event has no `positionId` AND no resolved
 * `position` expand — mirrors the iOS rule for showing the "Online" label.
 */
@JsExport
data class MensaWebEvent(
    val id: String,
    val title: String,
    val description: String,
    /** Full public URL for use in <img> tags. Empty string if no image. */
    val coverUrl: String,
    /** Raw PocketBase filename (no URL) — use this to pre-populate edit forms. */
    val image: String,
    val infoLink: String,
    val bookingLink: String,
    val startsMs: Double,
    val endsMs: Double,
    val isNational: Boolean,
    val isOnline: Boolean,
    val isPublic: Boolean,
    val isSpot: Boolean,
    val region: String,
    val locationName: String,
    val locationAddress: String,
    /** PocketBase id of the linked position, when the event is bound to a
     *  saved location. Empty string when the event is online or otherwise
     *  has no position attached. Mirrors `EventDraft.positionId`. */
    val locationId: String,
    val ownerName: String,
)

internal fun EventModel.toJs(): MensaWebEvent {
    val base = MensaSdk.apiBaseUrl()
    val cover = if (image.isNotBlank()) "$base/api/files/events/$id/$image" else ""
    val pos = position
    return MensaWebEvent(
        id = id,
        title = name,
        description = description,
        coverUrl = cover,
        image = image,
        infoLink = infoLink,
        bookingLink = bookingLink,
        startsMs = whenStart.toEpochMilliseconds().toDouble(),
        endsMs = whenEnd.toEpochMilliseconds().toDouble(),
        isNational = isNational,
        isOnline = positionId == null && pos == null,
        isPublic = isPublic,
        isSpot = isSpot,
        region = pos?.state ?: "",
        locationName = pos?.name ?: "",
        locationAddress = pos?.address ?: "",
        locationId = positionId ?: pos?.id ?: "",
        ownerName = eventOwner?.name ?: "",
    )
}

/**
 * Reads a browser File object into a ByteArray via Uint8Array.
 * file.arrayBuffer() returns a JS Promise<ArrayBuffer>; we bridge it to a
 * Kotlin suspend function using suspendCoroutine.
 */
internal suspend fun jsFileToByteArray(file: org.w3c.files.File): ByteArray =
    kotlin.coroutines.suspendCoroutine { cont ->
        @Suppress("UNCHECKED_CAST")
        val promise = file.asDynamic().arrayBuffer().unsafeCast<kotlin.js.Promise<org.khronos.webgl.ArrayBuffer>>()
        promise.then(
            { arrayBuffer ->
                val uint8 = Uint8Array(arrayBuffer)
                val bytes = ByteArray(uint8.length) { i -> uint8[i] }
                cont.resumeWith(Result.success(bytes))
                null
            },
            { err ->
                cont.resumeWith(Result.failure(RuntimeException(err.toString())))
                null
            }
        )
    }
