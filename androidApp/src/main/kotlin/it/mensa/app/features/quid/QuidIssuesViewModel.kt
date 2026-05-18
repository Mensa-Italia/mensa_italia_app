package it.mensa.app.features.quid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.QuidIssue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuidIssuesState(
    val issues: List<QuidIssue> = emptyList(),
    val refreshing: Boolean = false,
    val error: String? = null,
)

/**
 * QuidIssuesViewModel — entry-point for the Quid magazine issues list.
 *
 * Mirrors iOS QuidIssuesView data layer: observes QuidRepository.observeIssues()
 * and triggers refreshIssues() on init.
 */
class QuidIssuesViewModel : ViewModel() {

    private val repo = koinAccess().quid

    private val _state = MutableStateFlow(QuidIssuesState())
    val state: StateFlow<QuidIssuesState> = _state.asStateFlow()

    init {
        repo.observeIssues()
            .onEach { issues -> _state.update { it.copy(issues = issues) } }
            .launchIn(viewModelScope)

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            try {
                repo.refreshIssues()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(refreshing = false) }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
