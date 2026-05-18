package it.mensa.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class CalendarLinkModel(
    val id: String = "",
    val user: String = "",
    val hash: String = "",
    val state: List<String> = emptyList(),
)
