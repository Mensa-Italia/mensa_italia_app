package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.endpoints.DealWriteBody
import it.mensa.shared.api.endpoints.DealsApi
import it.mensa.shared.api.endpoints.DealsContactWriteBody
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.DealsContactModel
import it.mensa.shared.model.LocationModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class DealsRepository(
    private val api: DealsApi,
    private val db: MensaDatabase,
    private val json: Json
) {
    fun observeAll(): Flow<List<DealModel>> =
        db.dealQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel(json) } }

    suspend fun refresh(filter: String? = null, sort: String = "created") {
        val items = api.list(filter = filter, sort = sort)
        db.transaction {
            db.dealQueries.deleteAll()
            items.forEach { d ->
                db.dealQueries.insertOrReplace(
                    id = d.id,
                    name = d.name,
                    commercialSector = d.commercialSector,
                    positionJson = d.position?.let {
                        json.encodeToString(LocationModel.serializer(), it)
                    },
                    isLocal = if (d.isLocal) 1L else 0L,
                    details = d.details,
                    who = d.who,
                    starting = d.starting?.toEpochMilliseconds(),
                    ending = d.ending?.toEpochMilliseconds(),
                    howToGet = d.howToGet,
                    link = d.link,
                    owner = d.owner,
                    attachment = d.attachment,
                    isActive = if (d.isActive) 1L else 0L,
                    vatNumber = d.vatNumber,
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                )
            }
        }
    }

    suspend fun getById(id: String): DealModel? {
        val row = db.dealQueries.selectById(id).awaitAsOneOrNull() ?: return null
        return row.toModel(json)
    }

    /**
     * Fetches the contacts associated with a deal directly from the API.
     * Network-only: contacts are rarely viewed, so no local cache for now.
     */
    suspend fun contacts(dealId: String): List<DealsContactModel> =
        api.contacts(dealId)

    /**
     * Creates a new deal on the backend and triggers a `refresh()` so the
     * local cache picks it up. Returns the server-confirmed model.
     */
    suspend fun create(deal: DealModel): DealModel {
        val created = api.create(deal)
        runCatching { refresh() }
        return created
    }

    /**
     * Form draft mirroring Flutter's `addDeal` / `updateDeal` parameters.
     * `positionId` is the LocationModel id ("position" relation), `null` if
     * not set. Dates are wire-encoded as ISO-8601 strings.
     */
    data class DealDraft(
        val name: String,
        val commercialSector: String,
        val details: String?,
        val who: String?,
        val howToGet: String?,
        val link: String?,
        val vatNumber: String?,
        val positionId: String?,
        val starting: Instant?,
        val ending: Instant?,
    )

    /**
     * Primary contact attached to a deal. `id == null` means create a new
     * `deals_contacts` record on save; non-null means PATCH it.
     */
    data class ContactDraft(
        val id: String?,
        val name: String,
        val email: String,
        val phoneNumber: String?,
        val note: String?,
    )

    /**
     * Mirrors Flutter's `addDeal`: POST `/deals`, then if a contact is
     * provided POST `/deals_contacts` linked to the new deal id.
     * Refreshes the local cache so observers see the new row.
     */
    suspend fun create(draft: DealDraft, contact: ContactDraft?): DealModel {
        val created = api.createDeal(draft.toBody())
        if (contact != null) {
            api.createContact(contact.toBody(dealId = created.id))
        }
        runCatching { refresh() }
        return created
    }

    /**
     * Mirrors Flutter's `updateDeal`: PATCH `/deals/{id}` with the same
     * field set as create (plus `is_active=true`). Then for the contact:
     * if `contact.id == null` → CREATE; else → PATCH at that contact id.
     */
    suspend fun update(id: String, draft: DealDraft, contact: ContactDraft?): DealModel {
        val updated = api.updateDeal(id, draft.toBody())
        if (contact != null) {
            if (contact.id.isNullOrEmpty()) {
                api.createContact(contact.toBody(dealId = updated.id))
            } else {
                api.updateContact(contact.id, contact.toBody(dealId = updated.id))
            }
        }
        runCatching { refresh() }
        return updated
    }

    /**
     * Deletes a deal both on the backend and from the local cache.
     */
    suspend fun delete(id: String) {
        api.delete(id)
        runCatching {
            db.dealQueries.deleteById(id)
        }
    }

    private fun DealDraft.toBody(): DealWriteBody = DealWriteBody(
        name = name,
        commercialSector = commercialSector,
        details = details,
        who = who,
        howToGet = howToGet,
        link = link,
        vatNumber = vatNumber,
        position = positionId,
        isActive = true,
        starting = starting?.toString(),
        ending = ending?.toString(),
    )

    private fun ContactDraft.toBody(dealId: String): DealsContactWriteBody =
        DealsContactWriteBody(
            name = name,
            email = email,
            phoneNumber = phoneNumber,
            note = note,
            deal = dealId,
            isActive = true,
        )
}
