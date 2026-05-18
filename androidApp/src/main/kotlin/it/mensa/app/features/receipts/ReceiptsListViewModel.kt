package it.mensa.app.features.receipts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import java.text.NumberFormat
import java.util.Locale

// ─── Receipt kind ─────────────────────────────────────────────────────────────

enum class ReceiptKind { Donation, Renewal, Purchase, Unknown }

val ReceiptModel.kind: ReceiptKind
    get() {
        val d = (description ?: "").lowercase()
        return when {
            d.contains("donaz") || d.contains("donation") -> ReceiptKind.Donation
            d.contains("rinnov") || d.contains("renewal") || d.contains("membership") -> ReceiptKind.Renewal
            d.contains("acquist") || d.contains("purchase") || d.contains("boutique") -> ReceiptKind.Purchase
            else -> ReceiptKind.Unknown
        }
    }

val ReceiptKind.labelKey: String
    get() = when (this) {
        ReceiptKind.Donation -> "receipts.kind.donation"
        ReceiptKind.Renewal -> "receipts.kind.renewal"
        ReceiptKind.Purchase -> "receipts.kind.purchase"
        ReceiptKind.Unknown -> "receipts.kind.unknown"
    }

val ReceiptKind.fallback: String
    get() = when (this) {
        ReceiptKind.Donation -> "Donazione"
        ReceiptKind.Renewal -> "Rinnovo"
        ReceiptKind.Purchase -> "Acquisto"
        ReceiptKind.Unknown -> "Transazione"
    }

val ReceiptKind.iconVec: ImageVector
    get() = when (this) {
        ReceiptKind.Donation -> Icons.Outlined.Favorite
        ReceiptKind.Renewal -> Icons.Outlined.Autorenew
        ReceiptKind.Purchase -> Icons.Outlined.ShoppingBag
        ReceiptKind.Unknown -> Icons.Outlined.CreditCard
    }

// ─── Status color ─────────────────────────────────────────────────────────────

val ReceiptModel.statusColor: Color
    get() = when (status.lowercase()) {
        "completed", "paid", "success", "succeeded" -> Color(0xFF22C55E)
        "pending", "processing" -> Color(0xFFF59E0B)
        "failed", "error", "canceled", "cancelled" -> Color(0xFFEF4444)
        else -> Color(0xFF9CA3AF)
    }

// ─── Amount formatter ─────────────────────────────────────────────────────────

val ReceiptModel.amountFormatted: String
    get() {
        val euros = amount / 100.0
        val fmt = NumberFormat.getCurrencyInstance(Locale.ITALY)
        return try {
            fmt.format(euros)
        } catch (_: Exception) {
            "€%.2f".format(euros)
        }
    }

// ─── UI State ─────────────────────────────────────────────────────────────────

data class ReceiptsListUiState(
    val receipts: List<ReceiptModel> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class ReceiptsListViewModel : ViewModel() {

    private val repo = koinAccess().receipts

    private val _uiState = MutableStateFlow(ReceiptsListUiState())
    val uiState: StateFlow<ReceiptsListUiState> = _uiState.asStateFlow()

    init {
        repo.observeAll()
            .onEach { list ->
                // Sort desc by created date — mirrors iOS date desc ordering
                val sorted = list.sortedByDescending { it.created.toEpochMilliseconds() }
                _uiState.update { it.copy(receipts = sorted, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        // SSE realtime subscription
        repo.observeRealtime(viewModelScope)

        // Initial network fetch
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
}
