package it.mensa.app.features.boutique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.BoutiqueModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoutiqueListUiState(
    val products: List<BoutiqueModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val query: String = "",
)

class BoutiqueListViewModel : ViewModel() {

    private val repo = koinAccess().boutique

    private val _uiState = MutableStateFlow(BoutiqueListUiState())
    val uiState: StateFlow<BoutiqueListUiState> = _uiState.asStateFlow()

    init {
        repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(products = list, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { repo.refresh() }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
        }
    }

    fun onQueryChange(q: String) {
        _uiState.update { it.copy(query = q) }
    }

    /** Filtered list matching current search query. */
    fun filteredProducts(): List<BoutiqueModel> {
        val q = _uiState.value.query.trim().lowercase()
        if (q.isEmpty()) return _uiState.value.products
        return _uiState.value.products.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }
}
