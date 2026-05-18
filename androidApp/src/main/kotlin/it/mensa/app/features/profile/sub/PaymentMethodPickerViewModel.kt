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

/**
 * State for the inline payment-method picker bottom sheet used by the
 * donation flow. Mirrors Flutter's `PaymentMethodPickerModel` —
 * `showPicker` toggles between "default method + Pay" and the list of
 * methods to pick from.
 */
data class PaymentMethodPickerUiState(
    val loading: Boolean = true,
    val methods: List<PaymentMethodModel> = emptyList(),
    val defaultId: String? = null,
    val showPicker: Boolean = false,
    val adding: Boolean = false,
    val addPending: AddMethodRequest? = null,
    val errorMessage: String? = null,
)

class PaymentMethodPickerViewModel : ViewModel() {

    private val paymentMethods = koinAccess().paymentMethods
    private val stripe: StripeService = KoinPlatform.getKoin().get()

    private val _uiState = MutableStateFlow(PaymentMethodPickerUiState())
    val uiState: StateFlow<PaymentMethodPickerUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            // Fetch customer + methods in parallel-ish — methods drive the
            // list, customer.invoiceSettings.default_payment_method drives
            // which one is preselected.
            val methodsResult = runCatching { paymentMethods.refresh() }
            val customerResult = runCatching { paymentMethods.customer() }
            val methods = methodsResult.getOrDefault(emptyList())
            val defaultId = customerResult.getOrNull()?.defaultPaymentMethodId
            _uiState.update {
                it.copy(
                    loading = false,
                    methods = methods,
                    defaultId = defaultId,
                )
            }
        }
    }

    /** Returns the currently-default PaymentMethod, or null when there
     *  isn't one. Mirrors `getMyPaymentMethod()` in Flutter. */
    fun defaultMethod(): PaymentMethodModel? {
        val state = _uiState.value
        val id = state.defaultId ?: return null
        return state.methods.firstOrNull { it.id == id }
    }

    fun toggleShowPicker() {
        _uiState.update { it.copy(showPicker = !it.showPicker) }
    }

    fun selectMethod(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { paymentMethods.setDefault(id) }.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(defaultId = id, showPicker = false, loading = false)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(loading = false, errorMessage = e.message)
                    }
                },
            )
        }
    }

    /** Kicks off the add-method flow. The screen observes `addPending`
     *  and presents Stripe's PaymentSheet with the SetupIntent. */
    fun addMethod() {
        if (_uiState.value.adding || _uiState.value.addPending != null) return
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
                            addPending = AddMethodRequest(
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

    fun consumeAddPending() {
        _uiState.update { it.copy(addPending = null) }
    }

    fun onAddMethodResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                // PaymentSheet just attached a new method to the customer
                // and the backend marks it as default. Reload so the new
                // card shows up as the selected default.
                _uiState.update { it.copy(adding = false) }
                load()
            }
            is PaymentSheetResult.Canceled ->
                _uiState.update { it.copy(adding = false) }
            is PaymentSheetResult.Failed ->
                _uiState.update {
                    it.copy(adding = false, errorMessage = result.error.message)
                }
        }
    }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
