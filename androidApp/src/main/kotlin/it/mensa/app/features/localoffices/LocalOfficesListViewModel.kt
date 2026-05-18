package it.mensa.app.features.localoffices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.LocalOfficeModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

data class LocalOfficesListUiState(
    val offices: List<LocalOfficeModel> = emptyList(),
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

class LocalOfficesListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LocalOfficesListUiState(loading = true))
    val uiState: StateFlow<LocalOfficesListUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().localOffices
    private var observeJob: Job? = null

    init {
        startObserving()
        refresh()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = repo.observeAllOffices()
            .onEach { list ->
                _uiState.update { it.copy(offices = list, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                repo.refreshAllOffices()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun setQuery(q: String) = _uiState.update { it.copy(query = q) }

    fun filtered(state: LocalOfficesListUiState): List<LocalOfficeModel> {
        val q = state.query.trim()
        if (q.isEmpty()) return state.offices
        val needle = normalize(q)
        return state.offices.filter { o ->
            val hay = normalize("${o.name} ${o.region} ${o.bio}")
            hay.contains(needle)
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }

    private fun normalize(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace(Regex("[^\\p{ASCII}]"), "")
            .lowercase()
}
