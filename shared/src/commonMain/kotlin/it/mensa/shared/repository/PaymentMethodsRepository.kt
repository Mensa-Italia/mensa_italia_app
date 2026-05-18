package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.DonateResponse
import it.mensa.shared.api.endpoints.PaymentMethodsApi
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.api.endpoints.StripeCustomerObject
import it.mensa.shared.api.endpoints.StripeCustomerResponse
import it.mensa.shared.model.PaymentMethodModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Payment methods are not persisted to disk for security reasons.
 * The repository keeps an in-memory cache mirrored to a StateFlow so the
 * UI can react to additions / refreshes without re-fetching.
 */
class PaymentMethodsRepository(
    private val api: PaymentMethodsApi,
    private val settings: SettingsApi,
) {
    /**
     * Stripe publishable key, sourced from the PocketBase `configs` collection
     * (key: `stripe_key`). Mirrors Flutter's startup_viewmodel.dart.
     */
    suspend fun stripePublishableKey(): String =
        settings.configs()["stripe_key"].orEmpty()

    private val _methods = MutableStateFlow<List<PaymentMethodModel>>(emptyList())
    val methods: StateFlow<List<PaymentMethodModel>> = _methods.asStateFlow()

    suspend fun refresh(): List<PaymentMethodModel> {
        val items = api.list()
        _methods.value = items
        return items
    }

    suspend fun addMethod(): StripeCustomerResponse = api.addMethod()

    suspend fun setDefault(paymentMethodId: String) {
        api.setDefault(paymentMethodId)
        refresh()
    }

    suspend fun customer(): StripeCustomerObject = api.customer()

    suspend fun donate(amountCents: Int): DonateResponse = api.donate(amountCents)
}
