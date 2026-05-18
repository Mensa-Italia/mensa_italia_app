@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.api.endpoints.LocationsApi
import it.mensa.shared.model.LocationModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class PositionCreatePayload(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val createdBy: String,
)

@JsExport
class MensaWebPositions internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val api: LocationsApi get() = KoinPlatform.getKoin().get()

    fun list(): Promise<Array<MensaWebPosition>> = scope.promise {
        sdk.awaitReady()
        api.list().map { it.toJs() }.toTypedArray()
    }

    fun create(payload: PositionCreatePayload): Promise<MensaWebPosition> = scope.promise {
        sdk.awaitReady()
        api.create(
            name = payload.name,
            address = payload.address,
            lat = payload.latitude,
            lon = payload.longitude,
            createdBy = payload.createdBy,
        ).toJs()
    }

    fun delete(id: String): Promise<MensaWebPosition> = scope.promise {
        sdk.awaitReady()
        api.delete(id).toJs()
    }
}

@JsExport
data class MensaWebPosition(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val state: String,
)

internal fun LocationModel.toJs(): MensaWebPosition = MensaWebPosition(
    id = id,
    name = name,
    address = address,
    latitude = lat,
    longitude = lon,
    state = state,
)
