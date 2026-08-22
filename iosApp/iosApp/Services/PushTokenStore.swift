import Foundation
import UIKit
import Shared

/// Tiene allineata la riga di questo iPhone nella collection `users_devices`.
///
/// La versione precedente registrava il device una volta e poi si ricordava di
/// averlo fatto in `UserDefaults` (`push.token.lastUploaded = "<userId>|<token>"`).
/// Quel promemoria era il bug: cancellando il dispositivo dal server e
/// rifacendo login, la chiave era ancora lì, la POST non partiva più e il
/// telefono restava fuori dall'elenco — e senza notifiche — fino a una
/// reinstallazione. Peggio: la chiave veniva scritta anche quando la
/// registrazione falliva.
///
/// Adesso la verità su chi è registrato ce l'ha il server e gliela si chiede:
/// `DevicesRepository.ensureRegistered` (:shared) fa una GET filtrata per
/// `firebase_id` e la POST solo se manca davvero. Qui resta un promemoria di
/// sola sessione, così non si ripete la verifica a ogni callback FCM dello
/// stesso avvio, ma ogni avvio a freddo e ogni login ricontrollano.
@MainActor
final class PushTokenStore {
    static let shared = PushTokenStore()
    private init() {}

    /// Ultimo `(userId, token)` verificato **in questo processo**. Non
    /// persistito: è esattamente ciò che rendeva il bug permanente.
    private var verifiedInThisSession: String?

    /// Latest FCM token observed in this process. Set by `AppDelegate`'s
    /// `MessagingDelegate.messaging(_:didReceiveRegistrationToken:)`.
    private(set) var currentToken: String?

    /// Chiamata dal callback `MessagingDelegate`. Mette da parte il token e
    /// prova a registrarlo (no-op se l'utente non è ancora autenticato — ci
    /// riprova la `ensureRegistered()` dopo il login).
    func handle(token: String) {
        if token != currentToken {
            // Token ruotato: la verifica precedente non vale più.
            verifiedInThisSession = nil
        }
        currentToken = token
        Task { await ensureRegistered() }
    }

    /// Da chiamare dopo il login, dall'`onAppear` di `MainTabView` e a ogni
    /// ritorno in primo piano. Idempotente.
    func ensureRegistered() async {
        guard let token = currentToken, !token.isEmpty else { return }
        guard let user = koin.auth.currentUser.value as? UserModel, !user.id.isEmpty else {
            return
        }

        let key = "\(user.id)|\(token)"
        if verifiedInThisSession == key { return }

        let deviceName = UIDevice.current.name.isEmpty
            ? UIDevice.current.model
            : UIDevice.current.name

        let registered = try? await koin.devices.ensureRegistered(
            userId: user.id,
            firebaseToken: token,
            deviceName: deviceName,
            language: Locale.current.identifier
        )
        // Si segna solo ciò che è andato a buon fine: un fallimento di rete
        // deve poter essere ritentato al prossimo foreground.
        if registered != nil {
            verifiedInThisSession = key
        }
    }

    /// Da chiamare al logout: la prossima sessione riparte da zero e
    /// ripubblica il device per il nuovo utente.
    func reset() {
        verifiedInThisSession = nil
    }
}
