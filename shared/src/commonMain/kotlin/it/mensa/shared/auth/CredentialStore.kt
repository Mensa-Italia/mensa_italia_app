package it.mensa.shared.auth

import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

/** Email e password con cui l'utente e' entrato l'ultima volta. */
data class StoredCredentials(val email: String, val password: String)

interface ICredentialStore {
    suspend fun save(email: String, password: String)
    suspend fun read(): StoredCredentials?
    suspend fun clear()
}

/**
 * Conserva le credenziali dell'ultimo accesso con password, per poter rifare
 * un login vero — non un semplice refresh del token — a ogni avvio e quando
 * l'utente chiede di ricontrollare la propria tessera.
 *
 * Il backend e' lo stesso `Settings` che tiene gia' il refresh_token: Keychain
 * su iOS (`kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly`, fuori dal
 * backup iCloud e non sincronizzato) e `EncryptedSharedPreferences` su Android
 * (AES-256-GCM con master key nel Keystore). Vedi [TokenStore] e i due
 * `PlatformModule`.
 *
 * Resta il fatto che qui c'e' una password e non un token: chi ottiene i
 * segreti dell'app ottiene le credenziali, non una sessione revocabile. La
 * scelta e' consapevole — serve a rifare il login per intero, che e' l'unica
 * cosa che si e' vista aggiornare davvero i dati della tessera — ma il giorno
 * in cui `/api/cs/me` diventa affidabile questo file va via.
 *
 * Chi entra con la passkey non ha niente da salvare: [read] torna null e chi
 * chiama ricade sul refresh normale.
 */
class CredentialStore(
    private val settings: Settings,
) : ICredentialStore {

    override suspend fun save(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) return
        settings[KEY_EMAIL] = email
        settings[KEY_PASSWORD] = password
    }

    override suspend fun read(): StoredCredentials? {
        val email = settings.getStringOrNull(KEY_EMAIL)?.takeIf { it.isNotBlank() } ?: return null
        val password = settings.getStringOrNull(KEY_PASSWORD)?.takeIf { it.isNotBlank() } ?: return null
        return StoredCredentials(email, password)
    }

    override suspend fun clear() {
        settings.remove(KEY_EMAIL)
        settings.remove(KEY_PASSWORD)
    }

    private companion object {
        const val KEY_EMAIL = "auth_email"
        const val KEY_PASSWORD = "auth_password"
    }
}
