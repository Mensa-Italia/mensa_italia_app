package it.mensa.shared.di

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import it.mensa.shared.auth.ITokenStore
import it.mensa.shared.auth.TokenStore
import it.mensa.shared.db.DriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module

/**
 * Android auth-storage stays on `EncryptedSharedPreferences` (AES-256-GCM,
 * master key in Android Keystore / StrongBox) — same prefs file + key name as
 * the previous standalone implementation, so existing sessions migrate
 * transparently when the app upgrades to the multiplatform-settings wrapper.
 */
actual val platformModule: Module = module {
    single<Settings> {
        val ctx = androidContext()
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            ctx,
            "mensa_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        SharedPreferencesSettings(prefs)
    }
    single { TokenStore(get()) } bind ITokenStore::class
    single { DriverFactory(androidContext()) }
}
