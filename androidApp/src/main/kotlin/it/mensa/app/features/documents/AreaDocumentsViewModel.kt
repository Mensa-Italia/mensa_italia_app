package it.mensa.app.features.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.DocumentModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AreaDocumentsUiState(
    val documents: List<DocumentModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
)

class AreaDocumentsViewModel : ViewModel() {

    private val repo = koinAccess().documents

    private val _uiState = MutableStateFlow(AreaDocumentsUiState())
    val uiState: StateFlow<AreaDocumentsUiState> = _uiState.asStateFlow()

    init {
        repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(documents = list, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

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

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun onCategoryChange(category: String?) = _uiState.update { it.copy(selectedCategory = category) }

    fun categories(): List<String> =
        _uiState.value.documents
            .map { it.category }
            .filter { it.isNotEmpty() }
            .toSet()
            .sorted()

    fun filtered(): List<DocumentModel> {
        val s = _uiState.value
        return s.documents.filter { d ->
            (s.selectedCategory == null || d.category == s.selectedCategory) &&
                (s.searchQuery.isEmpty() ||
                    d.name.contains(s.searchQuery, ignoreCase = true) ||
                    (d.description?.contains(s.searchQuery, ignoreCase = true) == true))
        }
    }

    fun localizedCategory(raw: String): String =
        raw.replace("_", " ").replace("-", " ")
            .replaceFirstChar { it.uppercase() }
}
