@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.UseSerializers

@Serializable
data class NotificationModel(
    val id: String = "",
    val tr: String = "",
    @SerialName("tr_named_params")
    val trNamedParams: Map<String, String> = emptyMap(),
    val data: JsonObject? = null,
    val seen: Instant? = null,
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
)
