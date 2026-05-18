package it.mensa.shared.model.search

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val query: String = "",
    val total: Int = 0,
    val results: Map<String, List<SearchHit>> = emptyMap(),
) {
    fun hitsFor(type: String): List<SearchHit> = results[type].orEmpty()
    val allHits: List<SearchHit> get() = results.values.flatten()
}
