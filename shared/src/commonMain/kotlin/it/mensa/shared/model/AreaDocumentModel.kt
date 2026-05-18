package it.mensa.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class AreaDocumentModel(
    val description: String = "",
    val image: String = "",
    val dimension: String = "",
    val link: String = "",
)
