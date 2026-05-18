@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class BoutiqueModel(
    val id: String = "",
    val uid: String = "",
    val name: String = "",
    val description: String = "",
    val image: List<String> = emptyList(),
    val amount: Int = 0,
    @SerialName("alternative_of")
    val alternativeOf: String = "",
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
)
