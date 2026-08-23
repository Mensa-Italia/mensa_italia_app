import Foundation
import Security
import Shared

/// Recupera le credenziali lasciate dalla vecchia app Flutter.
///
/// Controparte iOS di `FlutterCredentialMigration` in `shared/androidMain`.
/// Flutter non persisteva la sessione — costruiva `PocketBase(getBaseUrl())`
/// con l'authStore in memoria — e rifaceva il login vero a ogni apertura
/// usando email e password tenute in `flutter_secure_storage`. Questa app le
/// cerca in Keychain sotto un servizio diverso, quindi per chi arriva da
/// Flutter erano sul dispositivo ma irraggiungibili: l'avvio ripiegava sulla
/// sessione salvata invece di rifare il login.
///
/// Gli attributi non sono indovinati: vengono dal sorgente del plugin alla
/// versione che l'app Flutter dichiarava (`flutter_secure_storage`
/// 10.0.0-beta.4). `kSecAttrService` e' il `defaultAccountName` delle
/// `AppleOptions`, e `kSecAttrAccount` e' la chiave cosi' com'e' — a differenza
/// di Android, su Apple il plugin non antepone nessun prefisso.
enum FlutterCredentialMigration {

    /// `AppleOptions.defaultAccountName` nel plugin.
    private static let service = "flutter_secure_storage_service"
    private static let emailKey = "email"
    private static let passwordKey = "password"

    /// Copia le credenziali nel `CredentialStore` dell'app e poi rimuove
    /// l'originale dal Keychain.
    ///
    /// La rimozione non e' pulizia: e' cio' che rende definitivo il logout.
    /// `AuthRepository.logout()` svuota il nostro store ma non conosce quello
    /// di Flutter — senza cancellare l'originale, la riapertura successiva
    /// rimigrerebbe le stesse credenziali e rifarebbe il login da solo, contro
    /// la volonta' di chi era appena uscito.
    ///
    /// Va chiamata prima di `auth.doInit()`: e' quello a decidere se rifare il
    /// login vero o ripiegare sulla sessione salvata, e lo decide leggendo le
    /// credenziali.
    static func run() async {
        guard let email = read(account: emailKey), !email.isEmpty,
              let password = read(account: passwordKey), !password.isEmpty
        else { return }

        let adopted = (try? await koin.auth.adoptLegacyCredentials(
            email: email, password: password
        ))?.boolValue ?? false

        guard adopted else { return }
        delete(account: emailKey)
        delete(account: passwordKey)
    }

    private static func read(account: String) -> String? {
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: service,
            kSecAttrAccount: account,
            kSecReturnData: true,
            kSecMatchLimit: kSecMatchLimitOne,
        ]
        var item: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &item) == errSecSuccess,
              let data = item as? Data
        else { return nil }
        return String(data: data, encoding: .utf8)
    }

    private static func delete(account: String) {
        let query: [CFString: Any] = [
            kSecClass: kSecClassGenericPassword,
            kSecAttrService: service,
            kSecAttrAccount: account,
        ]
        SecItemDelete(query as CFDictionary)
    }
}
