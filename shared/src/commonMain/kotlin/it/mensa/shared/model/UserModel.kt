@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class UserModel(
    val id: String = "",
    val username: String = "",
    val name: String = "",
    val avatar: String = "",
    val email: String = "",
    @SerialName("expire_membership")
    val expireMembership: Instant = Instant.fromEpochMilliseconds(0),
    val powers: List<String> = emptyList(),
    val addons: List<String> = emptyList(),
    @SerialName("is_membership_active")
    val isMembershipActive: Boolean = false,
    val created: Instant = Instant.fromEpochMilliseconds(0),
)
