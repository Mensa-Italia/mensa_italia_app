package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SigModel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val image: String = "",
    val link: String = "",
    @SerialName("group_type")
    val groupType: String = "",
)
