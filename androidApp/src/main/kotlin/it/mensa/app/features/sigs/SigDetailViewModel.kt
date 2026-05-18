package it.mensa.app.features.sigs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.SigModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SigDetailUiState(
    val sig: SigModel? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class SigDetailViewModel(private val sigId: String) : ViewModel() {

    private val _uiState = MutableStateFlow(SigDetailUiState())
    val uiState: StateFlow<SigDetailUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().sigs

    init {
        // Subscribe to the full list, filter locally for our id
        repo.observeAll()
            .onEach { list ->
                val match = list.firstOrNull { it.id == sigId }
                if (match != null) _uiState.update { it.copy(sig = match, loading = false) }
            }
            .catch { }
            .launchIn(viewModelScope)

        load()
    }

    fun load() {
        viewModelScope.launch {
            if (_uiState.value.sig == null) {
                _uiState.update { it.copy(loading = true) }
            }
            try {
                val fetched = repo.getById(sigId)
                if (fetched != null) _uiState.update { it.copy(sig = fetched, loading = false) }
                else _uiState.update { it.copy(loading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
