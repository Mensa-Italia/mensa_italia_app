package it.mensa.shared.di

import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import it.mensa.shared.auth.ITokenStore
import it.mensa.shared.auth.TokenStore
import it.mensa.shared.db.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Apple auth-storage uses the Keychain via `multiplatform-settings`'
 * `KeychainSettings`. Default accessibility class is
 * `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`:
 *
 *  - readable after the first device unlock following a cold boot
 *    (background BGTask refresh still works);
 *  - `ThisDeviceOnly`: an iCloud backup restore on a different device
 *    cannot lift the session;
 *  - excluded from iCloud Keychain sync.
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
    single { DriverFactory() }
}
