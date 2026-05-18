import SwiftUI
import Shared

@MainActor @Observable
final class LoginViewModel {
    var email: String = ""
    var password: String = ""
    var loading: Bool = false
    var error: String? = nil

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
    func login() async -> Bool {
        guard !email.isEmpty, !password.isEmpty else { return false }
        loading = true
        error = nil
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
