package it.mensa.shared.model.search

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchRequest(
    val q: String,
    val types: List<String>? = null,
    val region: String? = null,
    @SerialName("limit_per_type") val limitPerType: Int = 10,
    val hydrate: Boolean = true,
)
