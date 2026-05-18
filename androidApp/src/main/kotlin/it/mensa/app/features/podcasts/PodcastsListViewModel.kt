package it.mensa.app.features.podcasts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.Podcast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PodcastsListUiState(
    val podcasts: List<Podcast> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class PodcastsListViewModel : ViewModel() {

    private val repo = koinAccess().podcasts

    private val _uiState = MutableStateFlow(PodcastsListUiState())
    val uiState: StateFlow<PodcastsListUiState> = _uiState.asStateFlow()

    init {
        repo.observePodcasts()
            .onEach { list ->
                _uiState.update { it.copy(podcasts = list, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repo.refreshPodcasts() }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message) }
                }
        }
    }
}
