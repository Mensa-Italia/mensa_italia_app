package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import it.mensa.shared.model.PaymentMethodModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response shape of `POST /api/payment/method`.
 *
 * Matches Flutter's contract (see `payment_method_manager_viewmodel.dart`):
 * - `client_secret` → SetupIntent client secret
 * - `customer.id`   → Stripe customer id
 * - `ephemeral_key` → optional ephemeral key for the customer
 * - `publishable_key` → optional override of the publishable key
 *
 * Field names are aliased so we expose the same simple flat properties the
 * Swift layer already consumes.
 */
@Serializable
data class StripeCustomerResponse(
    @SerialName("client_secret")
    val setupIntent: String = "",
    val customer: StripeCustomerObject = StripeCustomerObject(),
    @SerialName("ephemeral_key")
    val ephemeralKey: String = "",
    @SerialName("publishable_key")
    val publishableKey: String = "",
) {
    /** Convenience accessor matching the old flat shape. */
    val customerId: String get() = customer.id
}

@Serializable
data class StripeCustomerObject(
    val id: String = "",
    @SerialName("invoice_settings")
    val invoiceSettings: StripeInvoiceSettings? = null,
) {
    /** Default payment method id from `invoice_settings.default_payment_method`.
     *  The Stripe API serializes this as either an inline object (when expanded)
     *  or just the id string. The /api/payment/customer endpoint returns the
     *  expanded object, hence `StripePaymentMethodRef.id`. */
    val defaultPaymentMethodId: String?
        get() = invoiceSettings?.defaultPaymentMethod?.id?.takeIf { it.isNotBlank() }
}

@Serializable
data class StripeInvoiceSettings(
    @SerialName("default_payment_method")
    val defaultPaymentMethod: StripePaymentMethodRef? = null,
)

@Serializable
data class StripePaymentMethodRef(
    val id: String = "",
)

@Serializable
data class DonateResponse(
    @SerialName("client_secret")
    val paymentIntent: String = "",
    val customer: StripeCustomerObject = StripeCustomerObject(),
    @SerialName("ephemeral_key")
    val ephemeralKey: String = "",
    @SerialName("publishable_key")
    val publishableKey: String = "",
) {
    /** Convenience accessor matching the old flat shape. */
    val customerId: String get() = customer.id
}

/**
 * Payment customer / payment-method endpoints. All /api/payment legacy
 * endpoints expect form-urlencoded bodies (see AuthApi for the rationale).
 */
class PaymentMethodsApi(private val client: HttpClient) {

    /** GET /api/payment/method — list saved payment methods for the user. */
    suspend fun list(): List<PaymentMethodModel> =
        client.get("/api/payment/method").body()

    /** POST /api/payment/method — start the "add new method" flow. */
    suspend fun addMethod(): StripeCustomerResponse =
        client.post("/api/payment/method") {
            setBody(FormDataContent(Parameters.Empty))
        }.body()

    /** POST /api/payment/default — set the default payment method.
     *  Form field name `payment_method_id` matches Flutter's contract. */
    suspend fun setDefault(paymentMethodId: String) {
        client.post("/api/payment/default") {
            setBody(
                FormDataContent(
                    Parameters.build { append("payment_method_id", paymentMethodId) }
                )
            )
        }
    }

    /** GET /api/payment/customer — bootstrap Stripe customer info.
     *  Returns the bare Stripe customer object (with `invoice_settings`),
     *  unlike `addMethod()` which wraps it in a SetupIntent response. */
    suspend fun customer(): StripeCustomerObject =
        client.get("/api/payment/customer").body()

    /** POST /api/payment/donate — initiates a donation PaymentIntent. */
    suspend fun donate(amountCents: Int): DonateResponse =
        client.post("/api/payment/donate") {
            setBody(
                FormDataContent(
                    Parameters.build { append("amount", amountCents.toString()) }
                )
            )
        }.body()
}
