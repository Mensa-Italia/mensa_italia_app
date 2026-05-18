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

data class TicketDetailUiState(
    val ticket: TicketModel? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class TicketDetailViewModel(private val ticketId: String) : ViewModel() {

    private val repo = koinAccess().tickets

    private val _uiState = MutableStateFlow(TicketDetailUiState())
    val uiState: StateFlow<TicketDetailUiState> = _uiState.asStateFlow()

    init {
        // Observe single ticket from DB — updates whenever SSE fires
        repo.observeOne(ticketId)
            .onEach { ticket ->
                _uiState.update { it.copy(ticket = ticket, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        // SSE per-collection subscription (write-through triggers observeOne emission)
        repo.observeRealtime(viewModelScope)
    }
}
