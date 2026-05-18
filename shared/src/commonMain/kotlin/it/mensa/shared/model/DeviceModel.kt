@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class DeviceModel(
    val id: String = "",
    val user: String = "",
    @SerialName("device_name")
    val deviceName: String = "",
    @SerialName("firebase_id")
    val firebaseId: String = "",
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
)
