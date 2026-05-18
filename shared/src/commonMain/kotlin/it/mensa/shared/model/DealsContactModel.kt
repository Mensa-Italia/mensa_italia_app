@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class DealsContactModel(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    val note: String? = null,
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
)
