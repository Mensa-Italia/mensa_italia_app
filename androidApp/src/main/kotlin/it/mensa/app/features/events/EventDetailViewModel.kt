package it.mensa.app.features.events

import it.mensa.app.support.refreshMessage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.EventScheduleModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EventDetailUiState(
    val event: EventModel? = null,
    val schedules: List<EventScheduleModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val calendarSuccess: Boolean = false,
    val calendarError: String? = null,
    val canEdit: Boolean = false,
)

@OptIn(ExperimentalCoroutinesApi::class)
class EventDetailViewModel(private val eventId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(EventDetailUiState())
    val uiState: StateFlow<EventDetailUiState> = _uiState.asStateFlow()

    private val eventsRepo get() = koinAccess().events
    private val schedulesRepo get() = koinAccess().eventSchedules
    private val auth get() = koinAccess().auth
    private val localOffices get() = koinAccess().localOffices

    init {
        loadCached()
        observeSchedules()
        observeCanEdit()
        refresh()
    }

    private fun loadCached() {
        viewModelScope.launch {
            val cached = eventsRepo.getById(eventId)
            if (cached != null) _uiState.update { it.copy(event = cached, loading = false) }
        }
    }

    private fun observeSchedules() {
        schedulesRepo.observeForEvent(eventId)
            .onEach { list ->
                _uiState.update { it.copy(schedules = list.sortedBy { s -> s.whenStart.toEpochMilliseconds() }) }
            }
            .catch { /* non-fatal */ }
            .launchIn(viewModelScope)
    }

    /**
     * "Modifica" segue la `updateRule` del server (owner, super, admin del
     * gruppo locale): il power `events`/`events_helper` non basta, serve
     * l'evento caricato.
     */
    private fun observeCanEdit() {
        combine(auth.currentUser, _uiState.map { it.event }.distinctUntilChanged()) { user, event -> user to event }
            .mapLatest { (user, event) ->
                event != null && runCatching { localOffices.canEditEvent(user, event) }.getOrDefault(false)
            }
            .onEach { canEdit -> _uiState.update { it.copy(canEdit = canEdit) } }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                eventsRepo.refresh(sort = "when_end")
                schedulesRepo.refresh(eventId)
                val updated = eventsRepo.getById(eventId)
                if (updated != null) _uiState.update { it.copy(event = updated, loading = false) }
                else _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.refreshMessage, loading = false) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearCalendarSuccess() = _uiState.update { it.copy(calendarSuccess = false) }
    fun clearCalendarError() = _uiState.update { it.copy(calendarError = null) }
    fun onCalendarSuccess() = _uiState.update { it.copy(calendarSuccess = true) }
    fun onCalendarError(msg: String) = _uiState.update { it.copy(calendarError = msg) }
}
