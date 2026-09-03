package it.mensa.shared.di

import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import it.mensa.shared.auth.CredentialStore
import it.mensa.shared.auth.ICredentialStore
import it.mensa.shared.auth.ITokenStore
import it.mensa.shared.auth.TokenStore
import it.mensa.shared.db.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Apple auth-storage uses the Keychain via `multiplatform-settings`'
 * `KeychainSettings`.
 *
 * ATTENZIONE all'accessibilita': `KeychainSettings(service = ...)` costruisce le
 * sue query con `kSecClass` e `kSecAttrService` soltanto — `kSecAttrAccessible`
 * non lo passa mai. Le voci nascono quindi con il default del Keychain,
 * `kSecAttrAccessibleWhenUnlocked`, che **finisce nei backup cifrati** del
 * dispositivo e si ripristina su un telefono diverso. Qui dentro c'e' il
 * refresh token e, via [CredentialStore], la password del socio.
 *
 * A portarle a `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` — leggibili
 * dopo il primo sblocco, cosi' il refresh da BGTask continua a funzionare, ma
 * fuori da ogni backup e dalla sincronizzazione iCloud — ci pensa
 * `KeychainHardening` lato Swift, con una `SecItemUpdate` fuori banda.
 *
 * Non si passa `kSecAttrAccessible` al costruttore di proposito: quegli
 * attributi finiscono anche nelle query di lettura, le voci gia' scritte non
 * verrebbero piu' trovate e ogni utente si ritroverebbe disconnesso dopo
 * l'aggiornamento. Il dettaglio sta nel commento di `KeychainHardening.swift`.
 *
 * TODO(watchOS sharing): for the watchOS companion app to read the same token
 * we need `kSecAttrAccessGroup` set on Keychain entries (and the matching
 * `keychain-access-groups` entitlement on both targets). As of
 * `multiplatform-settings` 1.2.0 the `KeychainSettings` public API does not
 * expose `accessGroup`. Options when the Watch target is added:
 *   1) bump multiplatform-settings to a version that exposes it (≥1.3.0 if
 *      released, otherwise track the upstream issue);
 *   2) implement a small `Settings`-conformant Keychain wrapper in `appleMain`
 *      that goes through `Security.framework` directly (NSMutableDictionary
 *      bridged via Foundation, NOT raw `Map<K,V> as NSObject`);
 *   3) keep iOS and Watch on separate Keychain entries and bootstrap the
 *      Watch session via `WCSession` on first launch.
 */
actual val platformModule: Module = module {
    single<Settings> { KeychainSettings(service = "it.mensa.app.auth") }
    single { TokenStore(get()) } bind ITokenStore::class
    // Stesso store sicuro del token, chiave diversa. Vedi CredentialStore.
    single { CredentialStore(get()) } bind ICredentialStore::class
    single { DriverFactory() }
}
