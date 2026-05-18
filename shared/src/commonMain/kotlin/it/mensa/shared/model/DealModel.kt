@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class DealModel(
    val id: String = "",
    val name: String = "",
    @SerialName("commercial_sector")
    val commercialSector: String = "",
    @SerialName("position")
    val positionId: String? = null,
    val expand: DealExpand? = null,
    @SerialName("is_local")
    val isLocal: Boolean = false,
    val details: String? = null,
    val who: String? = null,
    val starting: Instant? = null,
    val ending: Instant? = null,
    @SerialName("how_to_get")
    val howToGet: String? = null,
    val link: String? = null,
    val owner: String? = null,
    val attachment: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = false,
    @SerialName("vat_number")
    val vatNumber: String? = null,
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
) {
    val position: LocationModel? get() = expand?.position
}

@Serializable
data class DealExpand(
    val position: LocationModel? = null,
)
