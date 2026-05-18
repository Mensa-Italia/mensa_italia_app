package it.mensa.app.features.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.DocumentModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DocumentDetailUiState(
    val document: DocumentModel? = null,
    val summary: String? = null,
    val loading: Boolean = true,
    val summaryLoading: Boolean = false,
    val summaryFailed: Boolean = false,
    val error: String? = null,
)

class DocumentDetailViewModel(private val docId: String) : ViewModel() {

    private val repo = koinAccess().documents

    private val _uiState = MutableStateFlow(DocumentDetailUiState())
    val uiState: StateFlow<DocumentDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val doc = runCatching { repo.getById(docId) }.getOrNull()
            _uiState.update { it.copy(document = doc, loading = false) }
            if (doc != null) loadSummary(doc.elaborated)
        }
    }

    private fun loadSummary(elaboratedId: String) {
        if (elaboratedId.isEmpty()) {
            _uiState.update { it.copy(summaryFailed = true) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(summaryLoading = true, summaryFailed = false) }
            val elab = runCatching { repo.getElaborated(elaboratedId) }.getOrNull()
            if (elab != null) {
                _uiState.update { it.copy(summary = elab.iaResume, summaryLoading = false) }
            } else {
                _uiState.update { it.copy(summaryFailed = true, summaryLoading = false) }
            }
        }
    }
}
