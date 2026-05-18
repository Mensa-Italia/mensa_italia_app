package it.mensa.shared.model.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchHit(
    val id: String,
    val score: Double = 0.0,
    val title: String = "",
    val subtitle: String = "",
    val image: String = "",
    @SerialName("deep_link") val deepLink: String = "",
)
