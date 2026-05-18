@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.api.endpoints.DevicesApi
import it.mensa.shared.model.DeviceModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
data class MensaWebDevice(
    val id: String,
    val userId: String,
    val deviceName: String,
    val firebaseId: String,
    val createdMs: Double,
    val updatedMs: Double,
)

internal fun DeviceModel.toJs(): MensaWebDevice = MensaWebDevice(
    id = id,
    userId = user,
    deviceName = deviceName,
    firebaseId = firebaseId,
    createdMs = created.toEpochMilliseconds().toDouble(),
    updatedMs = updated.toEpochMilliseconds().toDouble(),
)

@JsExport
class MensaWebDevices internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val api: DevicesApi get() = KoinPlatform.getKoin().get()

    fun list(): Promise<Array<MensaWebDevice>> = scope.promise {
        sdk.awaitReady()
        api.list().map { it.toJs() }.toTypedArray()
    }

    fun delete(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        api.delete(id)
    }
}
