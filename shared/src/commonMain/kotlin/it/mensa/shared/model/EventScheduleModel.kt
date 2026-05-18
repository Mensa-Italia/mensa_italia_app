@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class EventScheduleModel(
    val id: String? = null,
    val title: String = "",
    val event: String? = null,
    val description: String = "",
    val image: String? = null,
    @SerialName("when_start")
    val whenStart: Instant = Instant.fromEpochMilliseconds(0),
    @SerialName("when_end")
    val whenEnd: Instant = Instant.fromEpochMilliseconds(0),
    @SerialName("max_external_guests")
    val maxExternalGuests: Int = 0,
    val price: Double = 0.0,
    @SerialName("info_link")
    val infoLink: String = "",
    @SerialName("is_subscriptable")
    val isSubscriptable: Boolean = false,
)
