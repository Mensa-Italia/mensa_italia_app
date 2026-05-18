package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.DealsContactModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DealWriteBody(
    val name: String,
    @SerialName("commercial_sector") val commercialSector: String,
    val details: String? = null,
    val who: String? = null,
    @SerialName("how_to_get") val howToGet: String? = null,
    val link: String? = null,
    @SerialName("vat_number") val vatNumber: String? = null,
    val position: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val starting: String? = null,
    val ending: String? = null,
)

@Serializable
internal data class DealsContactWriteBody(
    val name: String,
    val email: String,
    @SerialName("phone_number") val phoneNumber: String? = null,
    val note: String? = null,
    val deal: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

class DealsApi(private val pb: PocketBaseClient) {

    /**
     * Contacts associated with a deal. Mirrors the Flutter
     * `getDealsContacts(dealId)` call on `deals_contacts` collection.
     */
    suspend fun contacts(dealId: String): List<DealsContactModel> =
        pb.fullList(
            "deals_contacts",
            filter = "deal='$dealId'",
            sort = "created"
        )

    /**
     * Flutter default: sort=created, filter=ending>=now, expand=position
     */
    suspend fun list(
        filter: String? = null,
        sort: String = "created"
    ): List<DealModel> =
        pb.fullList("deals", filter = filter, sort = sort, expand = "position")

    suspend fun get(id: String): DealModel =
        pb.getOne("deals", id, expand = "position")

    suspend fun create(body: DealModel): DealModel =
        pb.create("deals", body)

    suspend fun update(id: String, body: DealModel): DealModel =
        pb.update("deals", id, body)

    suspend fun delete(id: String) =
        pb.delete("deals", id)

    /**
     * Internal write helpers — used by [DealsRepository] when posting
     * the form-shaped body that mirrors Flutter's `addDeal` / `updateDeal`.
     */
    internal suspend fun createDeal(body: DealWriteBody): DealModel =
        pb.create("deals", body)

    internal suspend fun updateDeal(id: String, body: DealWriteBody): DealModel =
        pb.update("deals", id, body)

    internal suspend fun createContact(body: DealsContactWriteBody): DealsContactModel =
        pb.create("deals_contacts", body)

    internal suspend fun updateContact(id: String, body: DealsContactWriteBody): DealsContactModel =
        pb.update("deals_contacts", id, body)

    internal suspend fun deleteContact(id: String) =
        pb.delete("deals_contacts", id)
}
