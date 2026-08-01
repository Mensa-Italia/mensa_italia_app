import SwiftUI
import Shared

/// Propone **una volta sola** di attivare una passkey, dopo un accesso con
/// password.
///
/// Tutte le condizioni stanno nel gate condiviso
/// (`PasskeyEnrollmentGate.shouldShow`): accesso avvenuto con password, marker
/// non ancora scritto, nessuna passkey gia' attiva, non in modalita' screenshot.
/// Qui c'e' solo la presentazione.
///
/// Il marker viene scritto **quando il prompt appare**, non quando viene
/// accettato: e' questo che rende vero "una volta sola" anche se l'utente dice
/// no. Da quel momento l'attivazione resta raggiungibile dai settings.
private struct PasskeyEnrollmentPromptModifier: ViewModifier {
    @State private var presented = false
    @State private var working = false
    @State private var failed = false

    private let authenticator = PasskeyAuthenticator()

    func body(content: Content) -> some View {
        content
            .task { await evaluate() }
            .alert(
                // Chiave con suffisso di piattaforma: il nome del sensore
                // biometrico è diverso su iOS e Android, e una chiave sola
                // costringerebbe una delle due a una dicitura generica.
                tr("app.passkey.enroll.title.ios", fallback: "Accedi con Face ID la prossima volta"),
                isPresented: $presented
            ) {
                Button(tr("app.passkey.enroll.confirm", fallback: "Attiva")) {
                    Task { await enroll() }
                }
                Button(tr("app.passkey.enroll.later", fallback: "Non ora"), role: .cancel) {}
            } message: {
                // Proposta di valore, non allarme sicurezza.
                Text(tr(
                    "app.passkey.enroll.body",
                    fallback: "Puoi entrare senza digitare la password. La password continua a funzionare."
                ))
            }
            .alert(
                tr("views.passkeys.add_failed", fallback: "Attivazione non completata. Riprova."),
                isPresented: $failed
            ) {
                Button(tr("common.ok", fallback: "OK"), role: .cancel) {}
            }
    }

    private func evaluate() async {
        guard let user = koin.auth.currentUser.value as? UserModel else { return }

        // Su iOS il supporto passkey c'e' sempre entro i target che spediamo
        // (ASAuthorization esiste da iOS 16), quindi il flag e' costante: su
        // Android invece dipende dalla versione di sistema.
        guard (try? await koin.passkeyEnrollment.shouldShow(
            user: user,
            deviceSupportsPasskeys: true
        )) == true else { return }

        koin.passkeyEnrollment.markShown(userId: user.id)
        presented = true
    }

    private func enroll() async {
        guard !working else { return }
        working = true
        defer { working = false }

        guard let challenge = try? await koin.passkeys.beginRegistration() else {
            failed = true
            return
        }

        let credentialJSON: String
        do {
            credentialJSON = try await authenticator.create(
                creationOptionsJSON: challenge.creationOptionsJson
            )
        } catch PasskeyAuthenticator.Failure.cancelledOrNoCredential {
            // Ha chiuso il prompt di sistema: nessun messaggio, e' una scelta.
            return
        } catch {
            Log.auth.error("Passkey enrollment failed: \(error.localizedDescription)")
            failed = true
            return
        }

        // Boolean e non Result: vedi il commento in PasskeyRepository. Il valore
        // attraversa il bridge come `Any` (NSNumber), da cui il cast.
        let outcome = try? await koin.passkeys.finishRegistration(
            passkeyId: challenge.passkeyId,
            credentialJson: credentialJSON,
            name: UIDevice.current.name
        )
        if (outcome as? Bool) != true { failed = true }
    }
}

extension View {
    /// Da applicare alla schermata principale post-login.
    func passkeyEnrollmentPrompt() -> some View {
        modifier(PasskeyEnrollmentPromptModifier())
    }
}
