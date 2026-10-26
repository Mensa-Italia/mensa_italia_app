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
    /**
     * Titolo e testo gia' tradotti dal server. Le UI partono da [tr] (la
     * chiave di traduzione, nella lingua dell'app); questi servono quando
     * [tr] e' vuoto o la chiave non ha traduzione, come per
     * `developer_message`.
     */
    val title: String = "",
    val description: String = "",
    @SerialName("tr_named_params")
    val trNamedParams: Map<String, String> = emptyMap(),
    val data: JsonObject? = null,
    val seen: Instant? = null,
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
)
