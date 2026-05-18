package it.mensa.app.features.tableport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.StampUserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PassportUiState(
    val stamps: List<StampUserModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    /** Set after a QR scan — contains stampId:::code payload split */
    val pendingVerification: ScanResult? = null,
)

/** Parsed QR payload: `stampId:::verificationCode` */
data class ScanResult(val stampId: String, val code: String)

class PassportViewModel : ViewModel() {

    private val repo = koinAccess().stamps

    private val _uiState = MutableStateFlow(PassportUiState())
    val uiState: StateFlow<PassportUiState> = _uiState.asStateFlow()

    init {
        repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(stamps = list, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            runCatching { repo.refresh() }
        }
    }

    /** Called by the QR scanner screen with a successfully parsed QR value. */
    fun onQrScanned(stampId: String, code: String) {
        _uiState.update { it.copy(pendingVerification = ScanResult(stampId, code)) }
    }

    /** Called when StampConfirmSheet is dismissed (confirmed or cancelled). */
    fun clearPendingVerification() {
        _uiState.update { it.copy(pendingVerification = null) }
        refresh()
    }
}
