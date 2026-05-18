package it.mensa.app.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.NotificationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationDetailUiState(
    val notification: NotificationModel? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class NotificationDetailViewModel(private val notificationId: String) : ViewModel() {

    private val repo = koinAccess().notifications

    private val _uiState = MutableStateFlow(NotificationDetailUiState())
    val uiState: StateFlow<NotificationDetailUiState> = _uiState.asStateFlow()

    init {
        // Observe the single notification from the DB flow by filtering
        repo.observeAll()
            .map { list -> list.firstOrNull { it.id == notificationId } }
            .onEach { notification ->
                _uiState.update { it.copy(notification = notification, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        // Mark as seen on open if not already
        viewModelScope.launch {
            runCatching { repo.markSeen(notificationId) }
        }
    }
}
