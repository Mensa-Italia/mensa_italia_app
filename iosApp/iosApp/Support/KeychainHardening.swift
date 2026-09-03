import Foundation
import Security

/// Tiene le voci del Keychain dell'app fuori dai backup del dispositivo.
///
/// ## Il problema
///
/// La sessione (refresh token) e le credenziali dell'ultimo accesso — email e
/// password in chiaro, vedi `CredentialStore` lato KMP — stanno nel Keychain,
/// scritte da `KeychainSettings` di `multiplatform-settings`. Quella classe
/// costruisce le sue query con `kSecClass` e `kSecAttrService` e basta: non
/// passa mai `kSecAttrAccessible`, quindi le voci nascono con il default del
/// Keychain, `kSecAttrAccessibleWhenUnlocked`.
///
/// Le voci `WhenUnlocked` **finiscono nei backup cifrati** di iTunes/Finder e
/// si ripristinano su un dispositivo diverso. In pratica: chi ha un backup
/// cifrato del telefono di un socio ha la sua password di Mensa. Con
/// `…ThisDeviceOnly` il Keychain non le mette in nessun backup, cifrato o no.
///
/// ## Perche' non basta cambiare il costruttore
///
/// `KeychainSettings` accetta attributi di default aggiuntivi, ma li aggiunge a
/// *ogni* operazione, letture comprese. Una `SecItemCopyMatching` con
/// `kSecAttrAccessible` filtra su quell'attributo: le voci gia' scritte come
/// `WhenUnlocked` non verrebbero piu' trovate e ogni utente si ritroverebbe
/// disconnesso dopo l'aggiornamento. Peggio: `SecItemAdd` fallirebbe con
/// `errSecDuplicateItem` (l'unicita' di una generic password e' account +
/// service, `kSecAttrAccessible` non ne fa parte) e il successivo
/// `SecItemUpdate` non troverebbe nulla, sollevando un errore.
///
/// ## Cosa fa invece questo file
///
/// Una `SecItemUpdate` fuori banda che cambia solo `kSecAttrAccessible` di
/// tutte le voci del service. Non tocca ne' i dati ne' il modo in cui
/// `KeychainSettings` legge e scrive: le sue query continuano a non filtrare
/// su quell'attributo, quindi trovano le voci esattamente come prima.
///
/// Una volta indurita, la voce resta tale: gli aggiornamenti successivi del
/// token passano da `SecItemUpdate` con il solo `kSecValueData`, e una
/// `SecItemUpdate` cambia solo gli attributi che le passi. Serve ripassare solo
/// quando nasce una voce nuova — dopo un logout e un nuovo accesso — ed e' per
/// questo che [harden] viene chiamata all'avvio e a ogni cambio di stato di
/// autenticazione.
enum KeychainHardening {

    /// Lo stesso `service` con cui `PlatformModule.apple.kt` costruisce
    /// `KeychainSettings`. Se cambia li', va cambiato qui.
    private static let service = "it.mensa.app.auth"

    /// Porta tutte le voci del service a `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`.
    ///
    /// `AfterFirstUnlock` e non `WhenUnlocked` perche' il refresh del token gira
    /// anche da BGTask con lo schermo bloccato; `ThisDeviceOnly` e' la meta' che
    /// conta qui, ed e' cio' che esclude la voce dai backup.
    ///
    /// Idempotente e senza effetti se non c'e' niente da aggiornare.
    /// `errSecItemNotFound` e' il caso normale del primo avvio, prima di
    /// qualunque accesso: non e' un errore.
    @discardableResult
    static func harden() -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
        ]
        let attributes: [String: Any] = [
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]

        let status = SecItemUpdate(query as CFDictionary, attributes as CFDictionary)

        switch status {
        case errSecSuccess:
            Log.auth.info("Keychain: voci portate a ThisDeviceOnly (fuori dai backup)")
            return true
        case errSecItemNotFound:
            // Nessuna sessione salvata: niente da indurire.
            return true
        default:
            // Non e' un motivo per fermare l'avvio: l'app funziona lo stesso,
            // semplicemente le voci restano con l'accessibilita' di prima.
            Log.auth.error("Keychain: SecItemUpdate fallita, status \(status)")
            return false
        }
    }
}
