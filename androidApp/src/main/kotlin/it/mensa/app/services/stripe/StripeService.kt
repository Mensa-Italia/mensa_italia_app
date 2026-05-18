package it.mensa.app.services.stripe

import android.content.Context
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import it.mensa.app.support.Logger
import it.mensa.shared.api.endpoints.DonateResponse
import it.mensa.shared.api.endpoints.StripeCustomerResponse
import it.mensa.shared.repository.PaymentMethodsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Bridges the shared payment repository to the Stripe Android SDK.
 * Mirrors the iOS counterpart in `iosApp/Support/StripeService.swift` and
 * Flutter's flows in `lib/ui/views/make_donation` and
 * `lib/ui/views/payment_method_manager`.
 *
 * Deep link `mensa://stripe-redirect` is registered in AndroidManifest.xml,
 * which is sufficient for Stripe to round-trip 3DS / bank-auth flows.
 */
class StripeService(
    private val context: Context,
    private val repository: PaymentMethodsRepository,
) {

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** Fetches `stripe_key` from PocketBase `configs` and initializes the
     *  Stripe SDK. Safe to call multiple times. */
    suspend fun bootstrap(): Boolean {
        if (_isInitialized.value) return true
        val key = runCatching { repository.stripePublishableKey() }.getOrElse {
            Logger.w("Stripe", "bootstrap", "Failed to load publishable key: ${it.message}")
            return false
        }
        if (key.isBlank()) {
            Logger.w("Stripe", "bootstrap", "Empty publishable key — Stripe disabled")
            return false
        }
        PaymentConfiguration.init(context, key)
        _isInitialized.value = true
        Logger.i("Stripe", "bootstrap", "Stripe SDK initialized")
        return true
    }

    suspend fun prepareAddMethod(): StripeCustomerResponse {
        bootstrap()
        return repository.addMethod()
    }

    suspend fun prepareDonation(amountCents: Int): DonateResponse {
        bootstrap()
        return repository.donate(amountCents)
    }

    /** Customer block is only attached when both id and ephemeralKey are
     *  present — Stripe asserts on a customer config without an ephemeral
     *  key. Flutter's flows also skip it when the backend doesn't supply
     *  one. */
    fun buildConfiguration(
        customerId: String?,
        ephemeralKey: String?,
    ): PaymentSheet.Configuration {
        val builder = PaymentSheet.Configuration.Builder(MERCHANT_DISPLAY_NAME)
            .allowsDelayedPaymentMethods(true)
        if (!customerId.isNullOrBlank() && !ephemeralKey.isNullOrBlank()) {
            builder.customer(
                PaymentSheet.CustomerConfiguration(
                    id = customerId,
                    ephemeralKeySecret = ephemeralKey,
                )
            )
        }
        return builder.build()
    }

    companion object {
        private const val MERCHANT_DISPLAY_NAME = "Mensa Italia"
    }
}
