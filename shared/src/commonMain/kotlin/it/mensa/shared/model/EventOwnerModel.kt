package it.mensa.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class EventOwnerModel(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatar: String = "",
)
