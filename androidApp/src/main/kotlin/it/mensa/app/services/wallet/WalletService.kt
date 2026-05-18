package it.mensa.app.services.wallet

import android.content.Context
import it.mensa.app.support.Logger

/**
 * WalletService — Google Wallet API for Passes integration skeleton.
 *
 * Enables adding the Mensa membership card (tessera) to Google Wallet.
 * Uses `com.google.android.gms:play-services-pay`.
 *
 * Required setup:
 * - Google Wallet Issuer Account + Issuer ID from Google Pay & Wallet Console
 * - Pass class created via REST API or Console (Loyalty or Generic pass class)
 * - Service account with Wallet Object Issuer role for JWT signing
 *
 * TODO:
 *  1. Create Pass class on Google Wallet Console for "it.mensa.app.tessera"
 *  2. Implement JWT generation for "Add to Google Wallet" button
 *     (backend API endpoint preferred — never embed signing key on client)
 *  3. Call [PayClient.savePasses] with the signed JWT
 *  4. Render the "Add to Google Wallet" button using the official asset
 *  5. Handle [PayClient.isReadyToPay] gating
 *
 * Reference: https://developers.google.com/wallet/reference/rest
 */
class WalletService(private val context: Context) {

    // TODO: inject via Koin once implementation is complete
    // private val payClient: PayClient = Wallet.getPaymentsClient(context, WalletConstants)

    private var isAvailable: Boolean = false

    /**
     * Check if the Google Wallet API is available on this device.
     * Must be called before attempting to add passes.
     *
     * @param onResult callback with availability result
     */
    fun checkAvailability(onResult: (Boolean) -> Unit) {
        // TODO: implement using PayClient.isReadyToPay
        Logger.d("Wallet", "checkAvailability", "Not yet implemented")
        onResult(false)
    }

    /**
     * Launch the "Add to Google Wallet" flow using a signed JWT.
     *
     * @param signedJwt JWT signed by the backend containing the Pass object
     * @param onSuccess called when the pass was successfully saved
     * @param onFailure called with error message on failure
     */
    fun addMembershipCard(
        signedJwt: String,
        onSuccess: () -> Unit = {},
        onFailure: (String) -> Unit = {},
    ) {
        // TODO: call PayClient.savePasses(signedJwt, activity, requestCode)
        Logger.w("Wallet", "addMembershipCard", "Not yet implemented")
        onFailure("WalletService not yet implemented")
    }
}
