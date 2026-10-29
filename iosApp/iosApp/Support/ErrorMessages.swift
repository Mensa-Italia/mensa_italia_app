import Foundation
import Shared

/// Cosa dire all'utente di un errore.
///
/// Le schermate mettevano in un alert `(error as NSError).localizedDescription`,
/// che per un'eccezione Kotlin è il dump intero ("Exception in http request:
/// Error Domain=NSURLErrorDomain Code=-1009…"). Offline lo vedeva chiunque
/// aprisse una lista: l'aggiornamento falliva, come è normale senza rete, e
/// l'alert copriva i dati che l'app aveva già.
extension Error {
    /// Nessuna connessione, server irraggiungibile o timeout — non un guasto.
    var isOffline: Bool {
        let ns = self as NSError
        if ns.domain == NSURLErrorDomain, Self.offlineCodes.contains(ns.code) { return true }
        // Le eccezioni Kotlin arrivano come NSError con l'originale qui dentro.
        if let kotlin = ns.userInfo["KotlinException"] as? KotlinThrowable {
            return NetworkErrors.shared.isUnavailable(t: kotlin)
        }
        return false
    }

    /// Il messaggio da mostrare: leggibile se si è offline, quello dell'errore
    /// altrimenti.
    var userMessage: String {
        if isOffline {
            return tr("app.error.offline", fallback: "Sei offline. Riprova quando torna la connessione.")
        }
        // Errori del livello di accesso (:shared, `IntegrityException`).
        if let kotlin = (self as NSError).userInfo["KotlinException"] as? KotlinThrowable {
            if kotlin is ExportRequestGoneException {
                return tr("ex_app.export.error.gone",
                          fallback: "La richiesta è scaduta o è già stata gestita. Chiedi di nuovo all'app esterna.")
            }
            if kotlin is AccountBlockedException {
                return tr("app.account.suspended.title", fallback: "Account sospeso")
            }
            // Il tag (`attest_key/2`) porta con sé la causa: uno screenshot di un
            // socio o di un revisore la mostra anche se il report al server non
            // fosse mai arrivato.
            if kotlin is IntegrityUnsupportedException || kotlin is IntegrityFailedException {
                return tr("app.error.integrity",
                          fallback: "Questo dispositivo non ha superato la verifica di sicurezza dell'app. Usa l'app ufficiale su un dispositivo non modificato.")
                    + Self.integrityTagSuffix(kotlin)
            }
            if kotlin is IntegrityException {
                return tr("app.error.integrity_retry", fallback: "Verifica di sicurezza non riuscita. Riprova fra poco.")
                    + Self.integrityTagSuffix(kotlin)
            }
        }
        return (self as NSError).localizedDescription
    }

    /// Per l'aggiornamento di dati già a schermo (liste, dettagli dalla cache):
    /// offline non c'è niente da segnalare, restano quelli salvati.
    var refreshMessage: String? { isOffline ? nil : userMessage }

    /// " (attest_key/2)" se l'errore di integrità conosce la sua causa, altrimenti
    /// niente. Non scrive nel log: una vista rilegge `userMessage` a ogni
    /// ridisegno. La riga di log la scrive una volta sola `IntegrityGuard`, dove
    /// il fallimento nasce.
    private static func integrityTagSuffix(_ kotlin: KotlinThrowable) -> String {
        let failure = (kotlin as? IntegrityUnavailableException)?.failure
            ?? (kotlin as? IntegrityUnsupportedException)?.failure
        guard let tag = failure?.tag else { return "" }
        return " (\(tag))"
    }

    private static var offlineCodes: Set<Int> {
        [
            NSURLErrorNotConnectedToInternet,
            NSURLErrorNetworkConnectionLost,
            NSURLErrorCannotFindHost,
            NSURLErrorCannotConnectToHost,
            NSURLErrorTimedOut,
            NSURLErrorDNSLookupFailed,
            NSURLErrorDataNotAllowed,
            NSURLErrorInternationalRoamingOff,
        ]
    }
}
