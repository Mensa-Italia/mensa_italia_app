@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class TicketModel(
    val id: String = "",
    val name: String? = null,
    val description: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val link: String? = null,
    val qr: String? = null,
    @SerialName("internal_ref_id")
    val internalRefId: String? = null,
    @SerialName("customer_data")
    val customerData: String? = null,
    val deadline: Instant? = null,
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
)
