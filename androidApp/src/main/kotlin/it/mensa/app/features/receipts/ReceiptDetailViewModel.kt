package it.mensa.app.features.receipts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.ReceiptModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptDetailUiState(
    val receipt: ReceiptModel? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val downloadingPdf: Boolean = false,
    val pdfUrl: String? = null,
)

class ReceiptDetailViewModel(private val receiptId: String) : ViewModel() {

    private val repo = koinAccess().receipts

    private val _uiState = MutableStateFlow(ReceiptDetailUiState())
    val uiState: StateFlow<ReceiptDetailUiState> = _uiState.asStateFlow()

    init {
        // Observe single receipt from DB — updates on SSE write-through
        repo.observeOne(receiptId)
            .onEach { receipt ->
                _uiState.update { it.copy(receipt = receipt, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        // SSE realtime subscription
        repo.observeRealtime(viewModelScope)
    }

    fun downloadPdf() {
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingPdf = true, pdfUrl = null) }
            runCatching { repo.getReceiptUrl(receiptId) }
                .onSuccess { url ->
                    _uiState.update { it.copy(downloadingPdf = false, pdfUrl = url) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(downloadingPdf = false, error = e.message)
                    }
                }
        }
    }

    /** Call after the URL has been consumed (opened in browser) to reset state. */
    fun onPdfUrlConsumed() {
        _uiState.update { it.copy(pdfUrl = null) }
    }
}
