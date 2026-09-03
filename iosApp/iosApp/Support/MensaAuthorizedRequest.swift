import Foundation
import Shared

/// Autenticazione per le richieste che non passano dal client Ktor condiviso.
///
/// Le immagini e i file non li scarica Ktor: se ne occupano due `URLSession`
/// nostre (`CachedAsyncImage`, `StampImagePrefetcher`), che quindi non vedono
/// il Bearer che `AuthPlugin` mette sulle chiamate API. Finche' `/api/files`
/// era pubblico non cambiava niente; ora e' l'header a decidere se il server
/// produce il link firmato verso S3, quindi va messo anche qui.
enum MensaAuth {

    /// Stessa regola del client Ktor (`isMensaHost` in `AuthPlugin.kt`): il
    /// Bearer va al nostro backend e a nessun altro. Le stesse view caricano
    /// anche immagini da CDN di terzi, e a un'origine estranea il token
    /// perderebbe la sessione senza motivo — oltre a farsi rifiutare.
    static func isMensaHost(_ host: String?) -> Bool {
        guard let host else { return false }
        return host == "mensa.it" || host.hasSuffix(".mensa.it")
    }

    /// Costruisce la richiesta aggiungendo l'`Authorization` quando l'URL e'
    /// nostro e una sessione c'e'.
    ///
    /// Il token si legge qui, a ogni richiesta, e non si tiene da parte:
    /// `AuthHolder` e' la fotografia sincrona della sessione corrente, ed e'
    /// esattamente cio' per cui esiste.
    static func request(
        url: URL,
        cachePolicy: URLRequest.CachePolicy = .useProtocolCachePolicy,
        timeoutInterval: TimeInterval = 60
    ) -> URLRequest {
        var request = URLRequest(url: url, cachePolicy: cachePolicy, timeoutInterval: timeoutInterval)
        authorize(&request)
        return request
    }

    /// Aggiunge l'header a una richiesta gia' costruita.
    static func authorize(_ request: inout URLRequest) {
        guard isMensaHost(request.url?.host) else { return }
        guard let token = AuthHolder.shared.token, !token.isEmpty else { return }
        request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    }
}

/// Toglie l'`Authorization` quando un redirect porta su un altro host.
///
/// Serve perche' il nostro backend risponde `307` verso S3: senza questo,
/// `URLSession` rigira gli header della richiesta originale al nuovo host e il
/// token della sessione finirebbe in un dominio che non e' nostro — nei suoi
/// log di accesso, e in mano a chiunque li legga. OkHttp lo fa da solo, qui va
/// scritto.
///
/// Usarlo come delegate per-task: `session.data(for: req, delegate:
/// MensaRedirectAuthStripper.shared)`.
final class MensaRedirectAuthStripper: NSObject, URLSessionTaskDelegate, @unchecked Sendable {
    static let shared = MensaRedirectAuthStripper()

    func urlSession(
        _ session: URLSession,
        task: URLSessionTask,
        willPerformHTTPRedirection response: HTTPURLResponse,
        newRequest request: URLRequest,
        completionHandler: @escaping (URLRequest?) -> Void
    ) {
        let originalHost = task.originalRequest?.url?.host
        guard request.url?.host != originalHost else {
            completionHandler(request)
            return
        }
        var sanitized = request
        sanitized.setValue(nil, forHTTPHeaderField: "Authorization")
        completionHandler(sanitized)
    }
}
