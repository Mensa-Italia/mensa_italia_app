@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.api.FilePart
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.endpoints.DealsApi
import it.mensa.shared.api.endpoints.DealsContactWriteBody
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.DealsContactModel
import it.mensa.shared.repository.DealsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlinx.datetime.Instant
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class DealCreatePayload(
    val name: String,
    val commercialSector: String,
    val details: String,
    val who: String,
    val howToGet: String,
    val link: String,
    val vatNumber: String,
    val positionId: String?,
    val validFromMs: Double,  // 0.0 = not set
    val validUntilMs: Double, // 0.0 = not set
)

@JsExport
class DealUpdatePayload(
    val name: String,
    val commercialSector: String,
    val details: String,
    val who: String,
    val howToGet: String,
    val link: String,
    val vatNumber: String,
    val positionId: String?,
    val validFromMs: Double,
    val validUntilMs: Double,
)

@JsExport
class DealContactPayload(
    val name: String,
    val email: String,
    val phone: String,
    val note: String,
    val dealId: String,
)

@JsExport
class MensaWebDeals internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: DealsRepository get() = KoinPlatform.getKoin().get()
    private val api: DealsApi get() = KoinPlatform.getKoin().get()
    private val pb: PocketBaseClient get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (deals: Array<MensaWebDeal>) -> Unit): () -> Unit {
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

    fun getById(id: String): Promise<MensaWebDeal?> = scope.promise {
        sdk.awaitReady()
        repo.getById(id)?.toJs()
    }

    fun contacts(dealId: String): Promise<Array<MensaWebDealContact>> = scope.promise {
        sdk.awaitReady()
        repo.contacts(dealId).map { it.toJs() }.toTypedArray()
    }

    fun create(payload: DealCreatePayload): Promise<MensaWebDeal> = scope.promise {
        sdk.awaitReady()
        val draft = DealsRepository.DealDraft(
            name = payload.name,
            commercialSector = payload.commercialSector,
            details = payload.details.ifBlank { null },
            who = payload.who.ifBlank { null },
            howToGet = payload.howToGet.ifBlank { null },
            link = payload.link.ifBlank { null },
            vatNumber = payload.vatNumber.ifBlank { null },
            positionId = payload.positionId,
            starting = if (payload.validFromMs > 0.0) Instant.fromEpochMilliseconds(payload.validFromMs.toLong()) else null,
            ending = if (payload.validUntilMs > 0.0) Instant.fromEpochMilliseconds(payload.validUntilMs.toLong()) else null,
        )
        repo.create(draft, contact = null).toJs()
    }

    fun update(id: String, payload: DealUpdatePayload): Promise<MensaWebDeal> = scope.promise {
        sdk.awaitReady()
        val draft = DealsRepository.DealDraft(
            name = payload.name,
            commercialSector = payload.commercialSector,
            details = payload.details.ifBlank { null },
            who = payload.who.ifBlank { null },
            howToGet = payload.howToGet.ifBlank { null },
            link = payload.link.ifBlank { null },
            vatNumber = payload.vatNumber.ifBlank { null },
            positionId = payload.positionId,
            starting = if (payload.validFromMs > 0.0) Instant.fromEpochMilliseconds(payload.validFromMs.toLong()) else null,
            ending = if (payload.validUntilMs > 0.0) Instant.fromEpochMilliseconds(payload.validUntilMs.toLong()) else null,
        )
        repo.update(id, draft, contact = null).toJs()
    }

    /**
     * Creates a deal with a browser File as cover image, sent via multipart/form-data.
     * Uses PocketBaseClient.createMultipart directly since DealsRepository.create
     * does not support multipart yet.
     */
    fun createMultipart(payload: DealCreatePayload, coverFile: org.w3c.files.File): Promise<MensaWebDeal> = scope.promise {
        sdk.awaitReady()
        val bytes = jsFileToByteArray(coverFile)
        val fields: Map<String, Any?> = buildDealFields(payload)
        val files = listOf(FilePart("attachment", coverFile.name, coverFile.type.ifBlank { "image/jpeg" }, bytes))
        val created: DealModel = pb.createMultipart("deals", fields, files)
        runCatching { repo.refresh() }
        created.toJs()
    }

    /**
     * Updates a deal with a new cover image via multipart/form-data.
     */
    fun updateMultipart(id: String, payload: DealUpdatePayload, coverFile: org.w3c.files.File): Promise<MensaWebDeal> = scope.promise {
        sdk.awaitReady()
        val bytes = jsFileToByteArray(coverFile)
        val fields: Map<String, Any?> = buildDealFields(payload)
        val files = listOf(FilePart("attachment", coverFile.name, coverFile.type.ifBlank { "image/jpeg" }, bytes))
        val updated: DealModel = pb.updateMultipart("deals", id, fields, files)
        runCatching { repo.refresh() }
        updated.toJs()
    }

    fun delete(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.delete(id)
    }

    fun createContact(payload: DealContactPayload): Promise<MensaWebDealContact> = scope.promise {
        sdk.awaitReady()
        api.createContact(
            DealsContactWriteBody(
                name = payload.name,
                email = payload.email,
                phoneNumber = payload.phone.ifBlank { null },
                note = payload.note.ifBlank { null },
                deal = payload.dealId,
                isActive = true,
            )
        ).toJs()
    }

    fun updateContact(id: String, payload: DealContactPayload): Promise<MensaWebDealContact> = scope.promise {
        sdk.awaitReady()
        api.updateContact(
            id,
            DealsContactWriteBody(
                name = payload.name,
                email = payload.email,
                phoneNumber = payload.phone.ifBlank { null },
                note = payload.note.ifBlank { null },
                deal = payload.dealId,
                isActive = true,
            )
        ).toJs()
    }

    fun deleteContact(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        api.deleteContact(id)
    }
}

@JsExport
data class MensaWebDeal(
    val id: String,
    val name: String,
    val sector: String,
    val description: String,
    val eligibility: String,
    val howToGet: String,
    val validFromMs: Double,
    val validUntilMs: Double,
    val discount: String,
    val link: String,
    /** Full public URL for use in <img> tags. Empty string if no image. */
    val coverUrl: String,
    /** Raw PocketBase filename (no URL) — use this to pre-populate edit forms. */
    val image: String,
    val isActive: Boolean,
    val isLocal: Boolean,
    val region: String,
    val locationName: String,
    val locationAddress: String,
    val vatNumber: String,
)

@JsExport
data class MensaWebDealContact(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val note: String,
)

internal fun DealModel.toJs(): MensaWebDeal {
    val base = MensaSdk.apiBaseUrl()
    val filename = attachment?.takeIf { it.isNotBlank() } ?: ""
    val cover = if (filename.isNotBlank()) "$base/api/files/deals/$id/$filename" else ""
    val pos = position
    // No structured "discount" / "eligibility" fields exist; PB collapses them
    // into the free-text `who` / `details` fields. Surface them under
    // ergonomic JS names so the UI can render without further mapping.
    return MensaWebDeal(
        id = id,
        name = name,
        sector = commercialSector,
        description = details ?: "",
        eligibility = who ?: "",
        howToGet = howToGet ?: "",
        validFromMs = starting?.toEpochMilliseconds()?.toDouble() ?: 0.0,
        validUntilMs = ending?.toEpochMilliseconds()?.toDouble() ?: 0.0,
        discount = "",
        link = link ?: "",
        coverUrl = cover,
        image = filename,
        isActive = isActive,
        isLocal = isLocal,
        region = pos?.state ?: "",
        locationName = pos?.name ?: "",
        locationAddress = pos?.address ?: "",
        vatNumber = vatNumber ?: "",
    )
}

private fun buildDealFields(payload: DealCreatePayload): Map<String, Any?> = linkedMapOf(
    "name" to payload.name,
    "commercial_sector" to payload.commercialSector,
    "details" to payload.details.ifBlank { null },
    "who" to payload.who.ifBlank { null },
    "how_to_get" to payload.howToGet.ifBlank { null },
    "link" to payload.link.ifBlank { null },
    "vat_number" to payload.vatNumber.ifBlank { null },
    "position" to payload.positionId,
    "is_active" to true,
    "starting" to if (payload.validFromMs > 0.0) Instant.fromEpochMilliseconds(payload.validFromMs.toLong()).toString() else null,
    "ending" to if (payload.validUntilMs > 0.0) Instant.fromEpochMilliseconds(payload.validUntilMs.toLong()).toString() else null,
)

private fun buildDealFields(payload: DealUpdatePayload): Map<String, Any?> = linkedMapOf(
    "name" to payload.name,
    "commercial_sector" to payload.commercialSector,
    "details" to payload.details.ifBlank { null },
    "who" to payload.who.ifBlank { null },
    "how_to_get" to payload.howToGet.ifBlank { null },
    "link" to payload.link.ifBlank { null },
    "vat_number" to payload.vatNumber.ifBlank { null },
    "position" to payload.positionId,
    "is_active" to true,
    "starting" to if (payload.validFromMs > 0.0) Instant.fromEpochMilliseconds(payload.validFromMs.toLong()).toString() else null,
    "ending" to if (payload.validUntilMs > 0.0) Instant.fromEpochMilliseconds(payload.validUntilMs.toLong()).toString() else null,
)

// jsFileToByteArray helper lives in MensaWebEvents.kt as `internal` so it is
// shared by both Deals and Events multipart helpers without duplication.

internal fun DealsContactModel.toJs(): MensaWebDealContact = MensaWebDealContact(
    id = id,
    name = name,
    email = email,
    phone = phoneNumber ?: "",
    note = note ?: "",
)
