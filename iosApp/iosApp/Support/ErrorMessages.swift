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
        isOffline
            ? tr("app.error.offline", fallback: "Sei offline. Riprova quando torna la connessione.")
            : (self as NSError).localizedDescription
    }

    /// Per l'aggiornamento di dati già a schermo (liste, dettagli dalla cache):
    /// offline non c'è niente da segnalare, restano quelli salvati.
    var refreshMessage: String? { isOffline ? nil : userMessage }

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
