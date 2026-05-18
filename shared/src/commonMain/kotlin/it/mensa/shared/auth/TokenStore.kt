package it.mensa.shared.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/**
 * Cross-platform persistent store for the serialised OIDC session JSON.
 *
 * Backed by `com.russhwolf:multiplatform-settings` so each platform plugs in
 * its native secure-storage primitive:
 *
 *  - **iOS** → `KeychainSettings` (Apple Keychain via `Security.framework`,
 *    `kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly` by default — i.e. the
 *    token survives reboots but cannot leak via iCloud backup restore to a
 *    different device).
 *  - **Android** → `SharedPreferencesSettings` wrapping `EncryptedSharedPreferences`
 *    (AES-256-GCM, master key in the Android Keystore / StrongBox when
 *    available).
 *  - **Web (js + wasmJs)** → `StorageSettings` on `localStorage`. NOT
 *    encrypted; same threat model as before (any in-page script can read it).
 *    A backend-set HttpOnly cookie is the only meaningful upgrade and is out
 *    of scope.
 *
 * `Settings` itself is synchronous (in-memory cached, flushed lazily on
 * platforms that need it). We wrap the calls in `suspend` to keep the
 * existing [ITokenStore] contract — historically [read] used to do disk I/O
 * on Android via `EncryptedSharedPreferences.getString` and that was the
 * justification for the `withContext(Dispatchers.IO)` hop. The Settings
 * wrapper retains the same characteristic, but the overhead is negligible
 * (microseconds) and we don't want to bring in a coroutines dispatcher just
 * for that.
 */
class TokenStore(
    private val settings: Settings,
    /**
     * Key under which the serialised OidcSession JSON is stored. Defaults to
     * the historical `auth_token` so the Android `EncryptedSharedPreferences`
     * upgrade migrates transparently (same prefs file + key). The web target
     * overrides this in `PlatformModule.js.kt` to `it.mensa.app.auth_token`
     * so the localStorage key matches the iOS Keychain naming convention
     * (`KeychainSettings(service = "it.mensa.app.auth")` produces an entry
     * displayed as `it.mensa.app.auth_token` in Keychain viewers).
     */
    private val key: String = "auth_token",
) : ITokenStore {
    override suspend fun save(token: String) {
        settings[key] = token
    }

    override suspend fun read(): String? = settings.getStringOrNull(key)

    override suspend fun clear() {
        settings.remove(key)
    }
}
