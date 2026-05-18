@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/**
 * stamp_users row. The `stamp` field is the stamp record id (string); when the
 * request uses `expand=stamp`, the expanded record appears under `expand.stamp`.
 * Swift consumers can read [stampRecord] to access the embedded stamp metadata.
 */
@Serializable
data class StampUserModel(
    val id: String = "",
    val created: Instant = Instant.fromEpochMilliseconds(0),
    val updated: Instant = Instant.fromEpochMilliseconds(0),
    @SerialName("stamp")
    val stampId: String = "",
    val user: String = "",
    val expand: StampUserExpand? = null,
) {
    val stampRecord: StampModel? get() = expand?.stamp
}

@Serializable
data class StampUserExpand(
    val stamp: StampModel? = null,
)
