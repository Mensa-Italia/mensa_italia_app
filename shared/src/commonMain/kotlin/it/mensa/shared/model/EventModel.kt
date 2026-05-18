@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * PocketBase records carry relations as ID strings in their original field,
 * and the expanded objects appear under `expand`. We keep the raw `positionId`
 * and `ownerId`, and expose a computed `position` / `eventOwner` that resolves
 * to the expanded object — so Swift consumers can keep using `event.position?.name`.
 */
@Serializable
data class EventModel(
    val id: String = "",
    val name: String = "",
    val image: String = "",
    val description: String = "",
    @SerialName("info_link")
    val infoLink: String = "",
    @SerialName("booking_link")
    val bookingLink: String = "",
    @SerialName("when_start")
    val whenStart: Instant = Instant.fromEpochMilliseconds(0),
    @SerialName("when_end")
    val whenEnd: Instant = Instant.fromEpochMilliseconds(0),
    val contact: String = "",
    @SerialName("is_national")
    val isNational: Boolean = false,
    @SerialName("is_spot")
    val isSpot: Boolean = false,
    @SerialName("is_public")
    val isPublic: Boolean = false,
    @SerialName("owner")
    val ownerId: String = "",
    @SerialName("position")
    val positionId: String? = null,
    val expand: EventExpand? = null,
) {
    val position: LocationModel? get() = expand?.position
    val eventOwner: EventOwnerModel? get() = expand?.owner
    val owner: String get() = ownerId
}

@Serializable
data class EventExpand(
    val position: LocationModel? = null,
    val owner: EventOwnerModel? = null,
)
