package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PbListResponse
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.DeviceModel
import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceBody(
    val user: String = "",
    val firebase_id: String,
    val device_name: String,
    val language: String = "",
)

/** I due campi che la delete rule di `users_devices` chiede a chi non e' il proprietario. */
@Serializable
data class DeleteDeviceByTokenBody(
    val id: String,
    val firebase_id: String,
)

@Serializable
data class UpdateDeviceLanguageBody(
    val language: String,
)

class DevicesApi(private val pb: PocketBaseClient) {

    @Throws(Exception::class)
    suspend fun list(): List<DeviceModel> =
        pb.fullList("users_devices", sort = "-created")

    @Throws(Exception::class)
    suspend fun register(
        userId: String,
        firebaseToken: String,
        deviceName: String,
        language: String = "",
    ): DeviceModel = pb.create(
        "users_devices",
        RegisterDeviceBody(
            user = userId,
            firebase_id = firebaseToken,
            device_name = deviceName,
            language = language,
        )
    )

    @Throws(Exception::class)
    suspend fun updateLanguage(id: String, language: String): DeviceModel =
        pb.update("users_devices", id, UpdateDeviceLanguageBody(language))

    @Throws(Exception::class)
    suspend fun delete(id: String) =
        pb.delete("users_devices", id)

    @Throws(Exception::class)
    suspend fun findByFirebaseId(firebaseId: String): PbListResponse<DeviceModel> =
        pb.list("users_devices", filter = "firebase_id='$firebaseId'")

    /**
     * Tutte le righe con questo token, di qualunque utente. La list rule le
     * mostra a chi passa il token come `?firebase_id=` (chi ha il token e'
     * su quel telefono); il solo filter vedrebbe solo le righe proprie.
     */
    @Throws(Exception::class)
    suspend fun findAnyUserByFirebaseId(firebaseId: String): PbListResponse<DeviceModel> =
        pb.list(
            "users_devices",
            filter = "firebase_id='$firebaseId'",
            query = mapOf("firebase_id" to firebaseId),
        )

    /** Cancella una riga di un altro utente: la delete rule vuole id e token nel body. */
    @Throws(Exception::class)
    suspend fun deleteByToken(id: String, firebaseId: String) =
        pb.deleteWithBody("users_devices", id, DeleteDeviceByTokenBody(id = id, firebase_id = firebaseId))
}
