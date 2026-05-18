package it.mensa.app.features.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.features.events.util.EventFilterHelpers
import it.mensa.app.features.events.util.EventFilterState
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.EventModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventListUiState(
    val events: List<EventModel> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val filter: EventFilterState = EventFilterState(),
    val canAddEvent: Boolean = false,
)

class EventListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EventListUiState(loading = true))
    val uiState: StateFlow<EventListUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().events
    private val auth get() = koinAccess().auth
    private var observeJob: Job? = null

    init {
        startObserving()
        observeUser()
        refresh(showSpinner = false)
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(events = list, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeUser() {
        auth.currentUser
            .onEach { user ->
                val powers = user?.powers ?: emptyList()
                val canAdd = powers.contains("super") ||
                    powers.contains("events") ||
                    powers.contains("events_helper") ||
                    powers.contains("canAddEvent")
                _uiState.update { it.copy(canAddEvent = canAdd) }
            }
            .launchIn(viewModelScope)
    }

    fun refresh(showSpinner: Boolean = true) {
        viewModelScope.launch {
            if (showSpinner) _uiState.update { it.copy(refreshing = true) }
            try {
                repo.refresh(sort = "when_end")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(refreshing = false) }
            }
        }
    }

    fun setQuery(q: String) = _uiState.update { it.copy(query = q) }
    fun setFilter(f: EventFilterState) = _uiState.update { it.copy(filter = f) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    // Derived lists

    private fun nowEpochMillis() = System.currentTimeMillis()

    private fun matchesQuery(e: EventModel, query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.lowercase().trim()
        if (e.name.lowercase().contains(q)) return true
        if (e.description.lowercase().contains(q)) return true
        val pos = e.position ?: return false
        return pos.name.lowercase().contains(q) || pos.address.lowercase().contains(q)
    }

    private fun passesAll(e: EventModel, state: EventListUiState): Boolean =
        EventFilterHelpers.matches(e, state.filter) && matchesQuery(e, state.query)

    fun upcoming(state: EventListUiState): List<EventModel> {
        val now = nowEpochMillis()
        return state.events
            .filter { it.whenEnd.toEpochMilliseconds() >= now }
            .filter { passesAll(it, state) }
            .sortedBy { it.whenStart.toEpochMilliseconds() }
    }

    fun past(state: EventListUiState): List<EventModel> {
        val now = nowEpochMillis()
        return state.events
            .filter { it.whenEnd.toEpochMilliseconds() < now }
            .filter { passesAll(it, state) }
            .sortedByDescending { it.whenStart.toEpochMilliseconds() }
    }
}
