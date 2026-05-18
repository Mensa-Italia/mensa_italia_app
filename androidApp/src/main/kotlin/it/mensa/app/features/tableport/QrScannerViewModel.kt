package it.mensa.app.features.tableport

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class QrScannerUiState(
    val scanning: Boolean = true,
    val lastScanned: String? = null,
)

class QrScannerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(QrScannerUiState())
    val uiState: StateFlow<QrScannerUiState> = _uiState.asStateFlow()

    /**
     * Parse a raw QR string in format `stampId:::verificationCode`.
     * Returns a [ScanResult] or null if the format does not match.
     */
    fun parseQr(raw: String): ScanResult? {
        val parts = raw.split(":::")
        if (parts.size < 2) return null
        val id = parts[0].trim()
        val code = parts[1].trim()
        if (id.isEmpty() || code.isEmpty()) return null
        _uiState.update { it.copy(scanning = false, lastScanned = raw) }
        return ScanResult(id, code)
    }

    /** Re-enable scanning after a dismiss so the user can try again. */
    fun resetScanning() {
        _uiState.update { it.copy(scanning = true, lastScanned = null) }
    }
}
