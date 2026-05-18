package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AddonModel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val icon: String = "",
    val version: String = "",
    val url: String = "",
    @SerialName("required_power")
    val requiredPower: Int = 0,
)
