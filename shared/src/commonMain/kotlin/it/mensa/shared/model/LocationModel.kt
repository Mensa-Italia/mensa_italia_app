package it.mensa.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationModel(
    val id: String = "",
    val name: String = "",
    val lat: Double = 0.0,
    val lon: Double = 0.0,
    val address: String = "",
    val state: String = "",
)
