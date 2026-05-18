package it.mensa.app.features.profile.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.PaymentMethodModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform
import it.mensa.app.services.stripe.StripeService

/**
 * Snapshot of everything needed to present Stripe PaymentSheet for a
 * donation. The Compose layer reads it via [MakeDonationUiState.pending]
 * and calls `paymentSheet.presentWithPaymentIntent(...)` exactly once per
 * value — the VM clears it on result.
 */
data class DonationPaymentRequest(
    val clientSecret: String,
    val configuration: PaymentSheet.Configuration,
)

data class MakeDonationUiState(
    val amount: Int = 10,
    val customAmountText: String = "",
    val usingCustom: Boolean = false,
    val methods: List<PaymentMethodModel> = emptyList(),
    val selectedMethodId: String? = null,
    val showPicker: Boolean = false,
    val submitting: Boolean = false,
    val pending: DonationPaymentRequest? = null,
    val showResult: Boolean = false,
    val resultMessage: String = "",
    val errorMessage: String? = null,
)

class MakeDonationViewModel : ViewModel() {

    private val paymentMethods = koinAccess().paymentMethods
    private val stripe: StripeService = KoinPlatform.getKoin().get()

    private val _uiState = MutableStateFlow(MakeDonationUiState())
    val uiState: StateFlow<MakeDonationUiState> = _uiState.asStateFlow()

    val presets = listOf(5, 10, 25, 50, 100)

    init {
        loadMethods()
    }

    private fun loadMethods() {
        viewModelScope.launch {
            runCatching { paymentMethods.refresh() }.onSuccess { list ->
                _uiState.update { it.copy(methods = list, selectedMethodId = list.firstOrNull()?.id) }
            }
        }
    }

    fun selectPreset(value: Int) {
        _uiState.update { it.copy(amount = value, usingCustom = false, customAmountText = "") }
    }

    fun onCustomAmountChange(text: String) {
        val digits = text.filter { it.isDigit() }
        val amount = digits.toIntOrNull()
        _uiState.update {
            it.copy(
                customAmountText = digits,
                amount = if (amount != null && amount > 0) amount else if (digits.isEmpty()) 10 else it.amount,
                usingCustom = amount != null && amount > 0,
            )
        }
    }

    fun selectMethod(id: String) {
        _uiState.update { it.copy(selectedMethodId = id) }
    }

    /** Requests a PaymentIntent from the backend and hands the secret +
     *  config to the Compose layer, which presents the Stripe sheet. */
    /** Step 1 — tap on "Dona" opens the picker sheet. The sheet handles
     *  add-method / select-default on its own and, on confirm, calls
     *  [runDonation] with the chosen payment method id. */
    fun submitDonation() {
        val state = _uiState.value
        if (state.amount <= 0 || state.submitting || state.pending != null) return
        _uiState.update { it.copy(showPicker = true) }
    }

    fun dismissPicker() {
        _uiState.update { it.copy(showPicker = false) }
    }

    /** Step 2 — picker confirmed a payment method. Create the PaymentIntent
     *  and hand it to the screen for PaymentSheet presentation. */
    fun runDonation(paymentMethodId: String) {
        val state = _uiState.value
        if (state.amount <= 0 || state.submitting || state.pending != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(showPicker = false, submitting = true) }
            runCatching {
                stripe.prepareDonation(state.amount * 100)
            }.fold(
                onSuccess = { resp ->
                    if (resp.paymentIntent.isBlank()) {
                        _uiState.update {
                            it.copy(
                                submitting = false,
                                showResult = true,
                                resultMessage = "Impossibile inizializzare il pagamento.",
                            )
                        }
                        return@fold
                    }
                    val config = stripe.buildConfiguration(
                        customerId = resp.customerId,
                        ephemeralKey = resp.ephemeralKey,
                    )
                    _uiState.update {
                        it.copy(
                            pending = DonationPaymentRequest(
                                clientSecret = resp.paymentIntent,
                                configuration = config,
                            ),
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            submitting = false,
                            showResult = true,
                            resultMessage = e.message ?: "Errore durante la donazione",
                        )
                    }
                },
            )
        }
    }

    /** Called by the screen once `presentWithPaymentIntent` has been
     *  invoked so we don't re-present on recomposition. */
    fun consumePending() {
        _uiState.update { it.copy(pending = null) }
    }

    fun onPaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> _uiState.update {
                it.copy(
                    submitting = false,
                    showResult = true,
                    resultMessage = "Grazie per il tuo supporto!",
                )
            }
            is PaymentSheetResult.Canceled -> _uiState.update {
                it.copy(submitting = false)
            }
            is PaymentSheetResult.Failed -> _uiState.update {
                it.copy(
                    submitting = false,
                    showResult = true,
                    resultMessage = result.error.message ?: "Pagamento non riuscito",
                )
            }
        }
    }

    fun dismissResult() = _uiState.update { it.copy(showResult = false) }
    fun amountString() = "€${_uiState.value.amount}"
}
