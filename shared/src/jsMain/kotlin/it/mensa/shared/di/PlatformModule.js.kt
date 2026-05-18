package it.mensa.shared.di

import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import it.mensa.shared.auth.ITokenStore
import it.mensa.shared.auth.TokenStore
import it.mensa.shared.db.DriverFactory
import kotlinx.browser.localStorage
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Browser auth-storage (`js(IR)` target). Same plaintext threat model as
 * wasmJs — see [it.mensa.shared.auth.TokenStore] kdoc.
 *
 * Key naming: aligned with the iOS Keychain convention
 * `KeychainSettings(service = "it.mensa.app.auth")` which produces a Keychain
 * entry labelled `it.mensa.app.auth_token`. Sharing the same logical key
 * across platforms makes cross-device debugging and any future SSO bridging
 * (e.g. a service-worker that reads the session from a sibling subdomain)
 * trivial.
 */
actual val platformModule: Module = module {
    single<Settings> { StorageSettings(delegate = localStorage) }
    single { TokenStore(get(), key = "it.mensa.app.auth_token") } bind ITokenStore::class
    single { DriverFactory() }
}
