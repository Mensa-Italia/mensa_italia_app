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

@Serializable
data class UpdateDeviceLanguageBody(
    val language: String,
)

class DevicesApi(private val pb: PocketBaseClient) {

    suspend fun list(): List<DeviceModel> =
        pb.fullList("users_devices", sort = "-created")

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

    suspend fun updateLanguage(id: String, language: String): DeviceModel =
        pb.update("users_devices", id, UpdateDeviceLanguageBody(language))

    suspend fun delete(id: String) =
        pb.delete("users_devices", id)

    suspend fun findByFirebaseId(firebaseId: String): PbListResponse<DeviceModel> =
        pb.list("users_devices", filter = "firebase_id='$firebaseId'")
}
