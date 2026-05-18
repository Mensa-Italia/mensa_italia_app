import Foundation
import PassKit
import UIKit
import Shared

/// Apple Wallet (PassKit) integration for the membership card.
///
/// NOTE: The Flutter reference app does NOT have a working backend endpoint
/// for generating `.pkpass` files for the membership card. In Flutter the
/// only Wallet code path is in `bottom_sheet_ticket_model.dart`, which uses
/// the `flutter_wallet_card` package to build cards client-side, and the
/// button itself is gated behind `if (false)` (i.e. disabled). The
/// `pass.app.mensa.it` pass-type-identifier IS configured in the Flutter
/// entitlements, but no `/api/wallet/*` endpoint exists yet on the backend.
///
/// This service is therefore scaffolded against the *expected* contract:
/// `GET https://svc.mensa.it/api/wallet/membership` returns a binary
/// `application/vnd.apple.pkpass` blob, authenticated via the PocketBase
/// auth token. If the endpoint returns 404 / non-pkpass content the caller
/// receives `WalletError.notAvailable` and should fall back to the
/// "coming soon" UX.
@MainActor
enum WalletService {

    enum WalletError: Error, LocalizedError {
        case notSupported
        case notAvailable
        case fetchFailed(Int)
        case invalidPass
        case noPresenter

        var errorDescription: String? {
            switch self {
            case .notSupported: return tr("wallet.error.not_supported", fallback: "Apple Wallet non è disponibile su questo dispositivo.")
            case .notAvailable: return tr("wallet.error.not_available", fallback: "La tessera Wallet non è ancora disponibile.")
            case .fetchFailed(let code): return tr("wallet.error.network", fallback: "Errore di rete ({code}).", ["code": "\(code)"])
            case .invalidPass: return tr("wallet.error.invalid_pass", fallback: "Il file della tessera non è valido.")
            case .noPresenter: return tr("wallet.error.no_presenter", fallback: "Impossibile presentare la tessera.")
            }
        }
    }

    /// True if the device supports adding passes to Wallet.
    static var canAddPasses: Bool {
        PKPassLibrary.isPassLibraryAvailable()
    }

    /// Fetch the `.pkpass` blob for the current user and present
    /// `PKAddPassesViewController`. Throws `WalletError` on any failure.
    static func presentAddMembershipPass() async throws {
        guard canAddPasses else { throw WalletError.notSupported }

        guard let url = URL(string: "https://svc.mensa.it/api/wallet/membership") else {
            throw WalletError.notAvailable
        }

        var req = URLRequest(url: url)
        req.setValue("application/vnd.apple.pkpass", forHTTPHeaderField: "Accept")

        // Reuse the existing PocketBase auth token. AuthRepository exposes
        // the current user/session via Kotlin flows; the raw bearer token
        // is forwarded by the shared HTTP client automatically when calling
        // through the SDK, but for a direct URLSession fetch we need to
        // attach it explicitly. If a typed accessor is added later
        // (e.g. `koin.auth.currentToken()`), swap this in.
        if let token = currentAuthToken() {
            req.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
        }

        let (data, response): (Data, URLResponse)
        do {
            (data, response) = try await URLSession.shared.data(for: req)
        } catch {
            throw WalletError.fetchFailed(-1)
        }

        guard let http = response as? HTTPURLResponse else {
            throw WalletError.fetchFailed(-1)
        }
        guard http.statusCode == 200 else {
            throw WalletError.fetchFailed(http.statusCode)
        }
        // Quick sanity check: PKPass throws on garbage / HTML 404 pages.
        let pass: PKPass
        do {
            pass = try PKPass(data: data)
        } catch {
            throw WalletError.invalidPass
        }

        guard let controller = PKAddPassesViewController(pass: pass) else {
            throw WalletError.invalidPass
        }

        guard let presenter = topMostViewController() else {
            throw WalletError.noPresenter
        }
        presenter.present(controller, animated: true)
    }

    // MARK: - Helpers

    /// Best-effort retrieval of the current PocketBase auth token from
    /// `UserDefaults` (where the shared SDK persists it via `Settings`).
    /// Returns nil if no token is found — the request will then be sent
    /// unauthenticated and the backend should respond 401.
    private static func currentAuthToken() -> String? {
        let candidateKeys = ["pb_auth_token", "auth_token", "authToken", "token"]
        for key in candidateKeys {
            if let s = UserDefaults.standard.string(forKey: key), !s.isEmpty {
                return s
            }
        }
        return nil
    }

    private static func topMostViewController() -> UIViewController? {
        guard let window = UIApplication.shared.activeKeyWindow,
              let root = window.rootViewController else { return nil }
        return root.topMost
    }
}

private extension UIApplication {
    var activeKeyWindow: UIWindow? {
        connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)
    }
}

private extension UIViewController {
    var topMost: UIViewController {
        if let presented = presentedViewController { return presented.topMost }
        if let nav = self as? UINavigationController,
           let top = nav.topViewController { return top.topMost }
        if let tab = self as? UITabBarController,
           let sel = tab.selectedViewController { return sel.topMost }
        return self
    }
}
