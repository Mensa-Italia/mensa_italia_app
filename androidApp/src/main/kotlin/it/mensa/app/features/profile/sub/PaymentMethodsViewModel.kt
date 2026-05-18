package it.mensa.app.features.profile.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import it.mensa.app.services.stripe.StripeService
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.PaymentMethodModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

data class AddMethodRequest(
    val clientSecret: String,
    val configuration: PaymentSheet.Configuration,
)

data class PaymentMethodsUiState(
    val methods: List<PaymentMethodModel> = emptyList(),
    val defaultId: String? = null,
    val loading: Boolean = true,
    val adding: Boolean = false,
    val pending: AddMethodRequest? = null,
    val stripeMessage: String? = null,
    val errorMessage: String? = null,
)

class PaymentMethodsViewModel : ViewModel() {

    private val paymentMethods = koinAccess().paymentMethods
    private val stripe: StripeService = KoinPlatform.getKoin().get()

    private val _uiState = MutableStateFlow(PaymentMethodsUiState())
    val uiState: StateFlow<PaymentMethodsUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { paymentMethods.refresh() }.fold(
                onSuccess = { list -> _uiState.update { it.copy(methods = list, loading = false) } },
                onFailure = { e -> _uiState.update { it.copy(loading = false, errorMessage = e.message) } },
            )
        }
    }

    fun setDefault(id: String) {
        viewModelScope.launch {
            runCatching { paymentMethods.setDefault(id) }.fold(
                onSuccess = { _uiState.update { it.copy(defaultId = id) } },
                onFailure = { e -> _uiState.update { it.copy(errorMessage = e.message) } },
            )
        }
    }

    /** Kicks off the "add payment method" flow: backend round-trip for a
     *  SetupIntent, then the Compose layer presents PaymentSheet. */
    fun addMethod() {
        if (_uiState.value.adding || _uiState.value.pending != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(adding = true) }
            runCatching { stripe.prepareAddMethod() }.fold(
                onSuccess = { resp ->
                    if (resp.setupIntent.isBlank()) {
                        _uiState.update {
                            it.copy(
                                adding = false,
                                errorMessage = "Impossibile inizializzare il pagamento.",
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
                            pending = AddMethodRequest(
                                clientSecret = resp.setupIntent,
                                configuration = config,
                            ),
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(adding = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun consumePending() {
        _uiState.update { it.copy(pending = null) }
    }

    fun onAddMethodResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                _uiState.update {
                    it.copy(adding = false, stripeMessage = "Metodo di pagamento aggiunto.")
                }
                refresh()
            }
            is PaymentSheetResult.Canceled ->
                _uiState.update { it.copy(adding = false) }
            is PaymentSheetResult.Failed ->
                _uiState.update {
                    it.copy(adding = false, errorMessage = result.error.message)
                }
        }
    }

    fun dismissStripeMessage() = _uiState.update { it.copy(stripeMessage = null) }
    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
