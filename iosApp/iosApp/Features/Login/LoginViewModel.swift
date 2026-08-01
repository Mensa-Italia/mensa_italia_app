import SwiftUI
import Shared

@MainActor @Observable
final class LoginViewModel {
    var email: String = ""
    var password: String = ""
    var loading: Bool = false
    var error: String?

    /// In corso un accesso con passkey (bottone separato, spinner separato).
    var passkeyLoading: Bool = false

    /// Messaggio **neutro**, non un errore: il percorso passkey non deve mai
    /// colorare di rosso la schermata. Quando fallisce, si dice all'utente che
    /// puo' usare la password e si va avanti.
    var passkeyNotice: String?

    private let passkeyAuthenticator = PasskeyAuthenticator()

    init() {
        // Debug seed for visual iteration only.
        if ProcessInfo.processInfo.arguments.contains("--preview-prefill") {
            email = "socio@mensa.it"
            password = "••••••••••"
        }
        // Dev convenience: prefill from env vars MENSA_EMAIL / MENSA_PASSWORD
        // when present (set via SIMCTL_CHILD_* on simulator launch).
        let env = ProcessInfo.processInfo.environment
        if let e = env["MENSA_EMAIL"], !e.isEmpty { email = e }
        if let p = env["MENSA_PASSWORD"], !p.isEmpty { password = p }
    }

    /// Auto-submit if the env asked for it. Called from the view's .task so
    /// we are off the init phase and Task { await ... } is safe.
    func autoLoginIfRequested() async {
        let env = ProcessInfo.processInfo.environment
        Log.auth.info("autoLogin check: MENSA_AUTOLOGIN=\(env["MENSA_AUTOLOGIN"] ?? "<nil>") email=\(self.email.isEmpty ? "empty" : "set") pwd=\(self.password.isEmpty ? "empty" : "set")")
        guard env["MENSA_AUTOLOGIN"] == "1" else { return }
        guard !email.isEmpty, !password.isEmpty else { return }
        Log.auth.info("autoLogin: triggering login")
        _ = await login()
    }

    /// Returns true on success. On failure sets `error`.
    /// AuthRepository.login(email:password:) returns Result<UserModel> in Kotlin.
    /// In Swift/ObjC bridging it throws on failure and returns UserModel on success.
    /// Accesso con passkey. Ritorna true solo in caso di successo.
    ///
    /// Ogni esito negativo riporta l'utente alla password senza errori rossi:
    /// e' l'unico comportamento sensato, perche' chi non ha una passkey su questo
    /// dispositivo non ha sbagliato nulla.
    func loginWithPasskey() async -> Bool {
        let address = email.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !address.isEmpty, !passkeyLoading, !loading else { return false }

        passkeyLoading = true
        passkeyNotice = nil
        error = nil
        defer { passkeyLoading = false }

        // Un solo ritentativo, e solo per challenge scaduta: significa che
        // l'utente ha esitato troppo davanti al prompt, non che qualcosa e' rotto.
        for attempt in 0..<2 {
            // `try?`: le suspend fun Kotlin arrivano in Swift come `async throws`
            // e possono lanciare su cancellazione. Il repository non propaga
            // errori applicativi — quelli sono gia' status — quindi un throw qui
            // e' eccezionale e va trattato come fallimento generico.
            guard let start = try? await koin.passkeys.beginLogin(email: address) else {
                passkeyNotice = notice(forStart: PasskeyStartStatus.networkError)
                return false
            }
            guard start.status == PasskeyStartStatus.ready, let challenge = start.challenge else {
                passkeyNotice = notice(forStart: start.status)
                return false
            }

            let credentialJSON: String
            do {
                credentialJSON = try await passkeyAuthenticator.assert(
                    requestOptionsJSON: challenge.requestOptionsJson
                )
            } catch PasskeyAuthenticator.Failure.cancelledOrNoCredential {
                // Annullato dall'utente oppure nessuna passkey locale: iOS non
                // li distingue, e per noi la conseguenza e' la stessa.
                passkeyNotice = tr(
                    "app.login.passkey.use_password",
                    fallback: "Accedi con la password su questo dispositivo."
                )
                return false
            } catch {
                Log.auth.error("Passkey assertion failed: \(error.localizedDescription)")
                passkeyNotice = tr(
                    "app.login.passkey.failed",
                    fallback: "Non riesco a usare la passkey. Prova con la password."
                )
                return false
            }

            guard let result = try? await koin.passkeys.finishLogin(
                loginId: challenge.loginId,
                credentialJson: credentialJSON
            ) else {
                passkeyNotice = notice(forFinish: PasskeyLoginStatus.networkError)
                return false
            }

            if result.status == PasskeyLoginStatus.success {
                Log.auth.info("Passkey login successful")
                return true
            }
            if result.status == PasskeyLoginStatus.challengeExpired, attempt == 0 {
                continue
            }

            passkeyNotice = notice(forFinish: result.status)
            return false
        }

        passkeyNotice = tr(
            "app.login.passkey.failed",
            fallback: "Non riesco a usare la passkey. Prova con la password."
        )
        return false
    }

    private func notice(forStart status: PasskeyStartStatus) -> String {
        if status == PasskeyStartStatus.networkError {
            return tr(
                "app.login.passkey.offline",
                fallback: "Non riesco a contattare il server. Prova con la password."
            )
        }
        return tr(
            "app.login.passkey.none",
            fallback: "Nessuna passkey attiva per questa email. Accedi con la password."
        )
    }

    private func notice(forFinish status: PasskeyLoginStatus) -> String {
        if status == PasskeyLoginStatus.networkError {
            return tr(
                "app.login.passkey.offline",
                fallback: "Non riesco a contattare il server. Prova con la password."
            )
        }
        return tr(
            "app.login.passkey.failed",
            fallback: "Non riesco a usare la passkey. Prova con la password."
        )
    }

    func login() async -> Bool {
        guard !email.isEmpty, !password.isEmpty else { return false }
        loading = true
        error = nil
        passkeyNotice = nil
        defer { loading = false }
        do {
            // Swift bridged name: login(email:password:) — async throws -> UserModel
            _ = try await koin.auth.login(email: email, password: password)
            Log.auth.info("Login successful")
            return true
        } catch {
            Log.auth.error("Login failed: \(error.localizedDescription)")
            self.error = (error as NSError).localizedDescription
            return false
        }
    }
}
