package it.mensa.app.features.tickets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.TicketModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

// ─── Status ───────────────────────────────────────────────────────────────────

enum class TicketStatus { Pending, Completed, Failed, Unknown }

val TicketModel.statusComputed: TicketStatus
    get() {
        if (qr.isNullOrBlank()) {
            val deadlineEpoch = deadline?.toEpochMilliseconds() ?: Long.MAX_VALUE
            return if (deadlineEpoch < Clock.System.now().toEpochMilliseconds()) {
                TicketStatus.Failed
            } else {
                TicketStatus.Pending
            }
        }
        return TicketStatus.Completed
    }

// ─── UI State ─────────────────────────────────────────────────────────────────

data class TicketsListUiState(
    val tickets: List<TicketModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class TicketsListViewModel : ViewModel() {

    private val repo = koinAccess().tickets

    private val _uiState = MutableStateFlow(TicketsListUiState())
    val uiState: StateFlow<TicketsListUiState> = _uiState.asStateFlow()

    init {
        // Observe DB flow (emits on every write-through from SSE or refresh)
        repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(tickets = list, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        // Start SSE realtime subscription — cancelled automatically when VM is cleared
        repo.observeRealtime(viewModelScope)

        // Initial network fetch
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { repo.refresh() }
                .onFailure { e ->
                    _uiState.update { it.copy(loading = false, error = e.message) }
                }
        }
    }
}
