@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ReceiptModel(
    val id: String = "",
    val description: String? = null,
    val user: String = "",
    @SerialName("stripe_code")
    val stripeCode: String = "",
    val status: String = "",
    val amount: Int = 0,
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
)
