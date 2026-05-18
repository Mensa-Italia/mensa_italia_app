import Foundation
import UIKit
import StripePaymentSheet
import StripeApplePay
import StripeCore
import StripePayments
import Shared

/// Bridges the Kotlin payment repositories to Stripe iOS.
///
/// Mirrors `lib/ui/views/startup/startup_viewmodel.dart` for SDK
/// initialization and `lib/ui/views/payment_method_manager/*`,
/// `lib/ui/views/make_donation/*`, and `lib/ui/views/receipts/*` for the
/// actual flows (initPaymentSheet / presentPaymentSheet / confirmPayment).
///
/// API shape: **all PaymentSheet presentation uses completion handlers,
/// never `withCheckedContinuation`**. On iOS 26 + Stripe iOS 23+ the
/// combination of an outer `Task { await … }` (e.g. SwiftUI `.task` or
/// async button action) awaiting a continuation that is resumed from
/// inside Stripe's own `async let` graph corrupts the Swift task heap and
/// trips `swift_task_dealloc_specific` → SIGABRT. Keeping the present()
/// path purely callback-based sidesteps that runtime bug.
enum StripeService {
    /// Apple Pay merchant identifier — matches the canonical Mensa Italia
    /// merchant set up under the paid team (dev@mensa.it / 6WA5D3RJBU).
    static let merchantId = "merchant.it.mensa.app"

    /// URL scheme registered in Info.plist (CFBundleURLTypes) for Stripe
    /// 3DS / redirect callbacks.
    static let urlScheme = "mensa"

    /// Apple Pay needs three things to stop PassKit from crashing the
    /// PaymentSheet:
    ///   1. `com.apple.developer.in-app-payments` entitlement listing
    ///      `merchantId` (done in iosApp.entitlements).
    ///   2. The merchant identifier created on developer.apple.com →
    ///      Identifiers → Merchant IDs, and enabled on the provisioning
    ///      profile (done by Xcode capability).
    ///   3. An Apple Pay Payment Processing certificate generated on
    ///      developer.apple.com and uploaded to the Stripe dashboard so
    ///      Stripe can decrypt the payment token. Without this the sheet
    ///      shows the Apple Pay row, the user taps it, and PassKit hard-
    ///      crashes the app.
    /// Flip back to `true` once step 3 is done.
    static let applePayEnabled = false

    private static var didConfigure = false

    /// Holds the PaymentSheet instance for the duration of presentation —
    /// PaymentSheet does not retain itself once `present(from:)` returns,
    /// so without an external strong reference the sheet is deallocated
    /// mid-flight and the result block is never called.
    private static var activeSheet: PaymentSheet?

    /// Fetches the publishable key from the shared SDK (PocketBase `configs`)
    /// and configures Stripe. Safe to call multiple times.
    @MainActor
    static func bootstrap() async throws {
        if didConfigure { return }
        let key = try await koin.paymentMethods.stripePublishableKey()
        guard !key.isEmpty else {
            Log.app.error("Stripe publishable key missing from configs")
            throw NSError(
                domain: "Stripe", code: -100,
                userInfo: [NSLocalizedDescriptionKey:
                    tr("app.payments.error.missing_key",
                       fallback: "Configurazione pagamenti non disponibile.")]
            )
        }
        StripeAPI.defaultPublishableKey = key
        didConfigure = true
        Log.app.info("Stripe configured with publishable key")
    }

    // MARK: - Setup Intent (add new card)

    /// Adds a new payment method via Stripe PaymentSheet in setup-intent mode.
    /// Mirrors `PaymentMethodManagerViewModel.addPaymentMethod()`.
    ///
    /// `completion` is delivered on the main thread.
    @MainActor
    static func addPaymentMethod(
        completion: @escaping (PaymentSheetResult) -> Void
    ) {
        Task { @MainActor in
            do {
                try await bootstrap()
                let seti = try await koin.paymentMethods.addMethod()
                guard !seti.setupIntent.isEmpty else {
                    completion(.failed(error: NSError(
                        domain: "Stripe", code: -101,
                        userInfo: [NSLocalizedDescriptionKey:
                            tr("app.payments.error.no_setup_intent",
                               fallback: "Impossibile inizializzare il pagamento.")]
                    )))
                    return
                }
                let config = makeConfig(customer: seti.customerId,
                                        ephemeralKey: seti.ephemeralKey,
                                        isSetup: true)
                let sheet = PaymentSheet(setupIntentClientSecret: seti.setupIntent,
                                         configuration: config)
                present(sheet: sheet, completion: completion)
            } catch {
                completion(.failed(error: error))
            }
        }
    }

    // MARK: - Payment Intent (donate / one-shot payment)

    /// Confirms a one-off payment (e.g. donation) using PaymentSheet.
    /// Mirrors `MakeDonationViewModel.doTheDonation()`.
    @MainActor
    static func payDonation(
        amountCents: Int32,
        completion: @escaping (PaymentSheetResult) -> Void
    ) {
        Task { @MainActor in
            do {
                try await bootstrap()
                let intent = try await koin.paymentMethods.donate(amountCents: amountCents)
                guard !intent.paymentIntent.isEmpty else {
                    completion(.failed(error: NSError(
                        domain: "Stripe", code: -102,
                        userInfo: [NSLocalizedDescriptionKey:
                            tr("app.payments.error.no_payment_intent",
                               fallback: "Impossibile inizializzare il pagamento.")]
                    )))
                    return
                }
                let config = makeConfig(customer: intent.customerId,
                                        ephemeralKey: intent.ephemeralKey,
                                        isSetup: false)
                let sheet = PaymentSheet(paymentIntentClientSecret: intent.paymentIntent,
                                         configuration: config)
                present(sheet: sheet, completion: completion)
            } catch {
                completion(.failed(error: error))
            }
        }
    }

    // MARK: - Off-session confirmation (donate / pay with default method)

    /// Confirms an existing PaymentIntent using whatever payment method the
    /// backend already attached (Mensa's `/api/payment/donate` charges the
    /// customer's default method off-session, then returns the
    /// `client_secret`). Mirrors Flutter's
    /// `Stripe.instance.confirmPayment(paymentIntentClientSecret:)` — no
    /// PaymentSheet UI, just the SDK's 3DS handler if the bank asks.
    @MainActor
    static func confirmPayment(
        clientSecret: String,
        completion: @escaping (PaymentSheetResult) -> Void
    ) {
        Task { @MainActor in
            do {
                try await bootstrap()
            } catch {
                completion(.failed(error: error))
                return
            }
            guard let presenter = topMostViewController() else {
                completion(.failed(error: NSError(
                    domain: "Stripe", code: -1,
                    userInfo: [NSLocalizedDescriptionKey:
                        "No view controller to present from"]
                )))
                return
            }
            let context = AuthContextHolder(presenter: presenter)
            // Keep the context alive — STPPaymentHandler holds a weak
            // reference to it. Cleared in the completion block below.
            activeAuthContext = context
            let params = STPPaymentIntentParams(clientSecret: clientSecret)
            STPPaymentHandler.shared().confirmPayment(
                params, with: context
            ) { status, _, error in
                activeAuthContext = nil
                switch status {
                case .succeeded:
                    completion(.completed)
                case .canceled:
                    completion(.canceled)
                case .failed:
                    completion(.failed(error: error ?? NSError(
                        domain: "Stripe", code: -110,
                        userInfo: [NSLocalizedDescriptionKey:
                            "Pagamento non riuscito"]
                    )))
                @unknown default:
                    completion(.failed(error: NSError(
                        domain: "Stripe", code: -111,
                        userInfo: [NSLocalizedDescriptionKey:
                            "Stato pagamento sconosciuto"]
                    )))
                }
            }
        }
    }

    private static var activeAuthContext: AuthContextHolder?

    /// `STPPaymentHandler` calls back through this for 3DS / redirect flows.
    private final class AuthContextHolder: NSObject, STPAuthenticationContext {
        let presenter: UIViewController
        init(presenter: UIViewController) { self.presenter = presenter }
        func authenticationPresentingViewController() -> UIViewController { presenter }
    }

    // MARK: - Configuration helpers

    private static func makeConfig(customer: String?,
                                   ephemeralKey: String?,
                                   isSetup: Bool) -> PaymentSheet.Configuration {
        var config = PaymentSheet.Configuration()
        config.merchantDisplayName = "Mensa Italia"
        config.returnURL = "\(urlScheme)://stripe-redirect"
        config.allowsDelayedPaymentMethods = true
        if let c = customer, !c.isEmpty, let k = ephemeralKey, !k.isEmpty {
            config.customer = .init(id: c, ephemeralKeySecret: k)
        }
        if applePayEnabled {
            config.applePay = .init(merchantId: merchantId, merchantCountryCode: "IT")
        }
        return config
    }

    @MainActor
    private static func present(
        sheet: PaymentSheet,
        completion: @escaping (PaymentSheetResult) -> Void
    ) {
        guard let presenter = topMostViewController() else {
            completion(.failed(error: NSError(
                domain: "Stripe", code: -1,
                userInfo: [NSLocalizedDescriptionKey:
                    "No view controller to present from"]
            )))
            return
        }
        activeSheet = sheet
        sheet.present(from: presenter) { result in
            // Drop the strong ref once Stripe is done with the sheet.
            activeSheet = nil
            completion(result)
        }
    }

    @MainActor
    private static func topMostViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
            ?? UIApplication.shared.connectedScenes
                .compactMap { $0 as? UIWindowScene }
                .first
        guard var top = scene?.keyWindow?.rootViewController
                ?? scene?.windows.first?.rootViewController
        else { return nil }
        while let presented = top.presentedViewController { top = presented }
        return top
    }
}
