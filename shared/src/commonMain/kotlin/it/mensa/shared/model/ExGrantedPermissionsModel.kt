@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ExGrantedPermissionsModel(
    val id: String = "",
    val user: String = "",
    @SerialName("ex_app")
    val exApp: String = "",
    val permissions: List<String> = emptyList(),
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
)
