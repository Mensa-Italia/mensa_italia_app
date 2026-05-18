package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.LocationModel
import kotlinx.serialization.Serializable

@Serializable
data class StateResponse(
    val state: String = "",
)

@Serializable
data class CreateLocationBody(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double,
    val saved: Boolean = true,
    val created_by: String = "",
)

@Serializable
data class DeleteLocationBody(
    val saved: Boolean = false,
)

class LocationsApi(
    private val pb: PocketBaseClient,
    private val client: HttpClient,
) {
    /** Returns all saved positions for the current user. */
    suspend fun list(): List<LocationModel> =
        pb.fullList("positions")

    /** Creates a new position entry. */
    suspend fun create(
        name: String,
        address: String,
        lat: Double,
        lon: Double,
        createdBy: String = "",
    ): LocationModel = pb.create(
        "positions",
        CreateLocationBody(
            name = name,
            address = address,
            lat = lat,
            lon = lon,
            saved = true,
            created_by = createdBy,
        )
    )

    /**
     * Soft-deletes a location by setting saved=false (as per the Flutter reference).
     */
    suspend fun delete(id: String): LocationModel =
        pb.update("positions", id, DeleteLocationBody(saved = false))

    /**
     * Calls GET /api/position/state?lat=&lon= to determine the geographic state
     * for the given coordinates.
     */
    suspend fun locateState(lat: Double, lon: Double): StateResponse =
        client.get("/api/position/state") {
            parameter("lat", lat)
            parameter("lon", lon)
        }.body()
}
