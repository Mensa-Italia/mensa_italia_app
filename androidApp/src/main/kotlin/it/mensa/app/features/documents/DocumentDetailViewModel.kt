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
    val loading: Boolean = true,
    val error: String? = null,
)

/**
 * Risolve `docId` nel file da aprire. Non c'e' piu' una schermata di dettaglio:
 * toccare un documento apre direttamente il PDF, e il riassunto AI non viene
 * nemmeno richiesto al backend.
 */
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
        }
    }

}
