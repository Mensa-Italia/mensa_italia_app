@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class ExAppModel(
    @SerialName("collectionId")
    val collectionId: String? = null,
    @SerialName("collectionName")
    val collectionName: String? = null,
    val id: String? = null,
    val name: String? = null,
    val description: String? = null,
    val image: String? = null,
    val created: Instant? = null,
    val updated: Instant? = null,
)
