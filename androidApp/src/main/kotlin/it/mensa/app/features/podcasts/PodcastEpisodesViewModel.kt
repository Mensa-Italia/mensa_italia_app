package it.mensa.app.features.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.services.audio.AudioTrack
import it.mensa.app.services.audio.AudioTrackKind
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.PodcastEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

data class PodcastEpisodesUiState(
    val episodes: List<PodcastEpisode> = emptyList(),
    val podcastTitle: String = "",
    val loading: Boolean = true,
    val error: String? = null,
)

class PodcastEpisodesViewModel(
    private val podcastId: String,
) : ViewModel(), KoinComponent {

    private val repo = koinAccess().podcasts
    private val audioController: AudioPlayerController by inject()

    private val _uiState = MutableStateFlow(PodcastEpisodesUiState())
    val uiState: StateFlow<PodcastEpisodesUiState> = _uiState.asStateFlow()

    init {
        repo.observeEpisodes(podcastId)
            .onEach { list ->
                _uiState.update { it.copy(episodes = list, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        repo.observePodcasts()
            .onEach { list ->
                val title = list.firstOrNull { it.id == podcastId }?.title.orEmpty()
                if (title.isNotEmpty()) {
                    _uiState.update { it.copy(podcastTitle = title) }
                }
            }
            .launchIn(viewModelScope)

        viewModelScope.launch { runCatching { repo.refreshPodcasts() } }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repo.refreshEpisodes(podcastId) }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message) }
                }
        }
    }

    fun playEpisode(episode: PodcastEpisode, podcastTitle: String) {
        makeTrack(episode, podcastTitle)?.let { track ->
            val current = audioController.currentTrack.value
            if (current?.id == track.id) {
                // Toggle pause/play
                audioController.togglePlayPause()
            } else {
                audioController.play(track)
            }
        }
    }

    fun playAll(podcastTitle: String) {
        val tracks = _uiState.value.episodes.mapNotNull { makeTrack(it, podcastTitle) }
        if (tracks.isNotEmpty()) audioController.play(tracks.first())
        // TODO: queue full list when queue API is finalized
    }

    fun shuffleAll(podcastTitle: String) {
        val tracks = _uiState.value.episodes.mapNotNull { makeTrack(it, podcastTitle) }.shuffled()
        if (tracks.isNotEmpty()) audioController.play(tracks.first())
    }

    fun addAllToQueue(podcastTitle: String) {
        // TODO: implement when AudioPlayerController queue is finalized
    }

    fun playFromEpisode(episode: PodcastEpisode, podcastTitle: String) {
        val episodes = _uiState.value.episodes
        val idx = episodes.indexOfFirst { it.id == episode.id }
        if (idx < 0) return
        val firstTrack = makeTrack(episodes[idx], podcastTitle) ?: return
        audioController.play(firstTrack)
    }

    private fun makeTrack(episode: PodcastEpisode, podcastTitle: String): AudioTrack? {
        val url = episode.audioUrl?.takeIf { it.isNotEmpty() } ?: return null
        return AudioTrack(
            id = "podcast-episode-${episode.id}",
            title = episode.title,
            subtitle = podcastTitle,
            artworkUrl = episode.imageUrl?.takeIf { it.isNotEmpty() },
            mediaUrl = url,
            kind = AudioTrackKind.Podcast,
            sourceId = episode.id,
            durationHint = if (episode.durationSeconds > 0) episode.durationSeconds.toLong() * 1000L else null,
        )
    }

    fun trackId(episodeId: String) = "podcast-episode-$episodeId"
}
