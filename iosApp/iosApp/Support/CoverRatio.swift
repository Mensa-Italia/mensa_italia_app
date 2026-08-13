import SwiftUI

/// Il rapporto larghezza/altezza da dare alla copertina di una card.
///
/// Gemello di `CoverRatio.kt` su Android: limiti, riserva e chiave devono
/// restare identici, altrimenti la stessa lista ha due altezze diverse sulle
/// due piattaforme.
///
/// Le liste avevano due difetti opposti: i SIG lasciavano bande vuote
/// (l'immagine entrava per intero in una finestra piu' alta di lei), gli
/// eventi venivano tagliati (altezza massima fissa su locandine piu' alte).
/// La cura e' la stessa: il contenitore prende il rapporto dell'immagine vera
/// e l'immagine resta in `.fill`.
///
/// Il rapporto pero' si conosce solo quando l'immagine e' arrivata, e questo
/// fa riassestare la lista sotto le dita. Due accorgimenti lo riducono a una
/// volta sola per immagine: la mappa vive qui e non dentro la cella — che
/// `LazyVStack` ricicla proprio mentre si scorre — ed e' persistita, quindi
/// dalla seconda apertura la lista e' gia' della misura giusta al primo frame.
@Observable
final class CoverRatioStore {

    static let shared = CoverRatioStore()

    /// Sotto il quadrato la card smetterebbe di essere una riga e diventerebbe
    /// una pagina. Sotto questo limite si torna a tagliare, di proposito.
    static let min: CGFloat = 1.0

    /// Sopra questo rapporto la striscia diventa troppo bassa perche' lo scrim
    /// e i chip in alto restino leggibili. E' tarato appena sopra il 2,53 che
    /// l'app impone quando si carica la copertina di un SIG, cosi' le banner
    /// passano intatte invece di essere tagliate per un pelo.
    static let max: CGFloat = 2.6

    /// Usato finche' non si sa niente, e per le card senza immagine.
    static let fallback: CGFloat = 16.0 / 9.0

    private static let storeKey = "mensa.cover.ratios"

    private var ratios: [String: CGFloat]

    private init() {
        let saved = UserDefaults.standard.dictionary(forKey: Self.storeKey) as? [String: Double] ?? [:]
        ratios = saved.mapValues { CGFloat($0) }
    }

    /// Il rapporto da usare adesso per [key], gia' dentro i limiti.
    ///
    /// La chiave e' l'URL completo, che contiene collezione, record e nome
    /// file: PocketBase rinomina il file a ogni upload, quindi quando un
    /// referente cambia la copertina la chiave cambia da sola e il valore
    /// vecchio non viene mai riusato.
    func ratio(for key: String?) -> CGFloat {
        guard let key, let stored = ratios[key] else { return Self.fallback }
        return Self.clamp(stored)
    }

    /// Registra la misura letta sull'immagine appena decodificata.
    func remember(_ size: CGSize, for key: String?) {
        guard let key, size.height > 0 else { return }
        let ratio = size.width / size.height
        guard ratio.isFinite, ratio > 0, ratios[key] != ratio else { return }
        ratios[key] = ratio
        UserDefaults.standard.set(
            ratios.mapValues { Double($0) },
            forKey: Self.storeKey
        )
    }

    static func clamp(_ ratio: CGFloat) -> CGFloat {
        Swift.min(Swift.max(ratio, min), max)
    }
}
