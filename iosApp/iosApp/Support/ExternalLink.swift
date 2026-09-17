import Foundation
import Shared
import UIKit

/// L'unico modo in cui l'app apre un indirizzo esterno.
///
/// I campi `link` che arrivano dal backend non sono garantiti assoluti: nel
/// database delle convenzioni ci sono davvero valori come `www.bonavitaly.com`,
/// perche' il tipo `url` di PocketBase accetta anche un indirizzo senza schema.
///
/// Su iOS quel valore non fallisce in modo evidente, ed e' il motivo per cui il
/// bottone sembrava rotto senza dire niente:
///
///  - `URL(string: "www.bonavitaly.com")` **non** restituisce `nil`. Restituisce
///    un URL *relativo*, con `scheme == nil`, quindi tutti i `guard let` passano.
///  - `UIApplication.open` su un URL relativo non fa assolutamente niente.
///  - `SFSafariViewController(url:)` accetta solo `http`/`https`: con un URL
///    privo di schema viola la sua precondizione e fa cadere la UI.
///
/// La normalizzazione vive in `shared` (`ExternalUrl`), non qui, cosi' iOS e
/// Android decidono con la stessa identica regola cosa sia un link apribile.
enum ExternalLink {

    /// L'indirizzo normalizzato, o `nil` se da `raw` non si ricava niente di
    /// apribile. Copre anche `mailto:` e `tel:`.
    static func url(_ raw: String?) -> URL? {
        guard let normalized = ExternalUrl.shared.normalize(raw: raw) else { return nil }
        return URL(string: normalized)
    }

    /// Solo `http`/`https`, cioe' quel che si puo' dare a `SFSafariViewController`
    /// o a una `WKWebView`. Per tutto il resto serve il sistema.
    static func browsable(_ raw: String?) -> URL? {
        guard let normalized = ExternalUrl.shared.normalize(raw: raw),
              ExternalUrl.shared.isWebUrl(url: normalized) else { return nil }
        return URL(string: normalized)
    }

    /// `true` se `raw` porta da qualche parte: serve a non mostrare bottoni morti.
    static func canOpen(_ raw: String?) -> Bool {
        ExternalUrl.shared.normalize(raw: raw) != nil
    }

    /// Passa l'indirizzo al sistema. Non fa niente se non c'e' niente da aprire.
    @MainActor
    static func open(_ raw: String?) {
        guard let url = url(raw) else { return }
        UIApplication.shared.open(url)
    }

    /// `mailto:` costruito a modo, con l'oggetto codificato.
    ///
    /// L'indirizzo passa dalla stessa regola di `shared`. Senza quel
    /// controllo `URLComponents` non fallisce mai — percent-codifica e basta —
    /// e un campo compilato a mano con "chiedere in sede" diventerebbe
    /// `mailto:chiedere%20in%20sede`, cioe' esattamente il bottone morto che
    /// questo helper doveva far sparire.
    static func mailto(_ email: String?, subject: String? = nil) -> URL? {
        var address = (email ?? "").trimmingCharacters(in: .whitespacesAndNewlines)
        if address.hasPrefix("mailto:") { address = String(address.dropFirst("mailto:".count)) }
        guard !address.isEmpty,
              ExternalUrl.shared.normalize(raw: address) == "mailto:\(address)" else { return nil }
        var components = URLComponents()
        components.scheme = "mailto"
        components.path = address
        if let subject, !subject.isEmpty {
            components.queryItems = [URLQueryItem(name: "subject", value: subject)]
        }
        return components.url
    }

    /// `tel:` senza spazi: il dialer non li digerisce.
    static func tel(_ phone: String?) -> URL? {
        let digits = (phone ?? "").filter { !$0.isWhitespace }
        guard !digits.isEmpty else { return nil }
        return URL(string: "tel:\(digits)")
    }
}
