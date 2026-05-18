@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class StampModel(
    val id: String = "",
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
    val description: String = "",
    val image: String = "",
)
