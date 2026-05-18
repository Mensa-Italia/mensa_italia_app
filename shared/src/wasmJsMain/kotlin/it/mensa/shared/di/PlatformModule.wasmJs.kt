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
 * Browser auth-storage: `multiplatform-settings`' `StorageSettings` backed
 * by `window.localStorage`. **Plaintext** — any in-page script (third-party
 * widgets, browser extensions, XSS) can read the session. Same threat
 * model as the previous direct localStorage implementation; only meaningful
 * fix is a backend HttpOnly cookie, which requires backend changes.
 */
actual val platformModule: Module = module {
    single<Settings> { StorageSettings(delegate = localStorage) }
    single { TokenStore(get()) } bind ITokenStore::class
    single { DriverFactory() }
}
