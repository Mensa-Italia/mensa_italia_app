import Foundation
import Shared
import SwiftUI
import UIKit

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

    /// Scarica un file del backend in un file temporaneo e ne ritorna il path.
    ///
    /// Serve a tutto cio' che non puo' mandare un header per conto suo:
    /// `PDFDocument(url:)` di PDFKit si fa la rete da solo e non accetta header,
    /// e un `ShareLink` su un URL remoto consegna a chi lo riceve un link che
    /// senza autenticazione risponde 404. Scaricando qui, con l'header, si
    /// ottengono entrambe le cose: i byte da dare a PDFKit e un file vero da
    /// condividere.
    ///
    /// Il file finisce in `tmp/mensa-files/`, che iOS non mette nei backup e
    /// svuota per conto suo. Il nome e' quello dell'URL cosi' il foglio di
    /// condivisione e l'anteprima mostrano un titolo sensato.
    ///
    /// Torna nil su qualunque errore: chi chiama mostra il suo stato di errore.
    static func downloadToTemporaryFile(from url: URL) async -> URL? {
        let urlRequest = request(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 60)
        do {
            let (data, response) = try await URLSession.shared.data(
                for: urlRequest,
                delegate: MensaRedirectAuthStripper.shared
            )
            guard let http = response as? HTTPURLResponse, http.statusCode == 200 else {
                let code = (response as? HTTPURLResponse)?.statusCode ?? -1
                Log.net.error("download file: HTTP \(code) per \(url.absoluteString)")
                return nil
            }
            let dir = FileManager.default.temporaryDirectory
                .appendingPathComponent("mensa-files", isDirectory: true)
            try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
            let name = url.lastPathComponent.isEmpty ? "documento" : url.lastPathComponent
            let dest = dir.appendingPathComponent(name)
            try? FileManager.default.removeItem(at: dest)
            try data.write(to: dest, options: .atomic)
            return dest
        } catch {
            Log.net.error("download file fallito: \(error.localizedDescription)")
            return nil
        }
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

/// Un file scaricato, pronto per il foglio di condivisione.
///
/// `Identifiable` perche' `.sheet(item:)` lo richiede: un `URL` da solo non lo e'.
struct SharedFile: Identifiable {
    let url: URL
    var id: String { url.absoluteString }
}

/// Foglio di condivisione di sistema per un file gia' su disco.
///
/// `ShareLink` copre il caso in cui il contenuto si conosce quando si compone
/// la view. Qui il file esiste solo dopo un download autenticato, quindi la
/// condivisione va presentata quando quel download e' finito — e per
/// presentarla a comando serve `UIActivityViewController`.
struct ShareFileSheet: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
