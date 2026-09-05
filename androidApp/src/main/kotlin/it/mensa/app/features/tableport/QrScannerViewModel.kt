package it.mensa.app.features.tableport

import androidx.lifecycle.ViewModel
import it.mensa.shared.tableport.StampQRParser
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
     * Legge il contenuto del QR e ne ricava id e codice.
     *
     * Delega a [StampQRParser], che e' lo stesso parser usato da iOS. Qui c'era
     * una seconda copia della stessa logica, scritta a mano: le due si sono
     * divise il destino, e il QR con l'URL intero le rompeva entrambe allo
     * stesso modo senza che si potesse correggerle in un posto solo.
     */
    fun parseQr(raw: String): ScanResult? {
        val payload = StampQRParser.parse(raw) ?: return null
        _uiState.update { it.copy(scanning = false, lastScanned = raw) }
        return ScanResult(payload.stampId, payload.verificationCode)
    }

    /** Re-enable scanning after a dismiss so the user can try again. */
    fun resetScanning() {
        _uiState.update { it.copy(scanning = true, lastScanned = null) }
    }
}
