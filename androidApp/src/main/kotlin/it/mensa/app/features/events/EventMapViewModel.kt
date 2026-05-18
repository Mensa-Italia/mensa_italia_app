package it.mensa.app.features.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.EventModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventMapUiState(
    val events: List<EventModel> = emptyList(),
    val selectedEventId: String? = null,
)

class EventMapViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventMapUiState())
    val uiState: StateFlow<EventMapUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().events

    init {
        repo.observeAll()
            .onEach { list -> _uiState.update { it.copy(events = list) } }
            .catch { /* non-fatal */ }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            try { repo.refresh(sort = "when_end") } catch (_: Exception) {}
        }
    }

    /** Only upcoming events with a geo position */
    fun geoEvents(): List<EventModel> {
        val now = System.currentTimeMillis()
        return _uiState.value.events.filter { e -> e.position != null && e.whenEnd.toEpochMilliseconds() >= now }
    }

    fun selectEvent(id: String?) = _uiState.update { it.copy(selectedEventId = id) }

    fun selectedEvent(): EventModel? = _uiState.value.selectedEventId?.let { id ->
        _uiState.value.events.firstOrNull { it.id == id }
    }
}
