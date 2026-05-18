package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.PodcastsApi
import it.mensa.shared.model.Podcast
import it.mensa.shared.model.PodcastEpisode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class PodcastsRepository(
    private val api: PodcastsApi,
) {
    private val _podcasts = MutableStateFlow<List<Podcast>>(emptyList())
    private val _episodes = MutableStateFlow<Map<String, List<PodcastEpisode>>>(emptyMap())

    fun observePodcasts(): Flow<List<Podcast>> = _podcasts.asStateFlow()

    suspend fun refreshPodcasts() {
        _podcasts.value = api.listPodcasts()
    }

    fun observeEpisodes(podcastId: String): Flow<List<PodcastEpisode>> =
        _episodes.map { it[podcastId] ?: emptyList() }

    suspend fun refreshEpisodes(podcastId: String) {
        val episodes = api.listEpisodes(podcastId)
        _episodes.value = _episodes.value + (podcastId to episodes)
    }
}
