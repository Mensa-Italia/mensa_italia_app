package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.StampModel
import it.mensa.shared.model.StampUserModel
import kotlinx.serialization.Serializable

/** Minimal body for creating a user_stamp record (stamp + code + user id). */
@Serializable
data class AddUserStampBody(
    val stamp: String,
    val code: String,
    val user: String
)

class StampsApi(private val pb: PocketBaseClient) {

    /**
     * Fetch all stamps from the `stamps` collection.
     * Flutter uses pb.collection('stamp').getOne(...) for single fetch.
     */
    suspend fun list(
        filter: String? = null,
        sort: String = "-created"
    ): List<StampModel> =
        pb.fullList("stamps", filter = filter, sort = sort)

    suspend fun get(id: String): StampModel =
        pb.getOne("stamps", id)

    suspend fun create(body: StampModel): StampModel =
        pb.create("stamps", body)

    suspend fun update(id: String, body: StampModel): StampModel =
        pb.update("stamps", id, body)

    suspend fun delete(id: String) =
        pb.delete("stamps", id)

    /**
     * Add a user_stamp record — POST /api/collections/stamp_users/records.
     * Flutter: pb.collection('stamp_users').create(body: { "stamp": id, "code": code, "user": userId })
     */
    suspend fun addUserStamp(stampId: String, code: String, userId: String): StampUserModel =
        pb.create("stamp_users", AddUserStampBody(stamp = stampId, code = code, user = userId))

    /**
     * Fetch user stamps for the authenticated user — expand=stamp, sort=-created.
     */
    suspend fun getUserStamps(
        filter: String? = null
    ): List<StampUserModel> =
        pb.fullList("stamp_users", filter = filter, sort = "-created", expand = "stamp")

    /**
     * Verify a stamp code by fetching the stamp filtered by the supplied code.
     * Used by the QR-scan flow before creating the user_stamp record.
     */
    suspend fun getStamp(id: String, code: String): StampModel? {
        val sId = id.replace("'", "")
        val sCode = code.replace("'", "")
        return pb.list<StampModel>(
            collection = "stamps",
            perPage = 1,
            filter = "id = '$sId' && code = '$sCode'",
        ).items.firstOrNull()
    }

    /**
     * Shorthand for [addUserStamp] — the user id comes from the caller context.
     */
    suspend fun addStamp(stampId: String, code: String, userId: String): StampUserModel =
        addUserStamp(stampId, code, userId)
}
