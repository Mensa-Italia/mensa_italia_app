package it.mensa.shared.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * Recupera le credenziali lasciate dalla vecchia app Flutter.
 *
 * Flutter non persisteva la sessione — costruiva `PocketBase(getBaseUrl())`
 * con l'authStore in memoria — e rifaceva il login a ogni apertura usando
 * email e password tenute in `FlutterSecureStorage`. L'app Kotlin usa un
 * archivio diverso, con chiavi diverse (`auth_email` / `auth_password` in
 * `mensa_secure_prefs`), quindi quelle credenziali erano lì ma irraggiungibili.
 *
 * Senza questo ponte, chi arriva da Flutter non ha credenziali utilizzabili:
 * `AuthRepository.reloginWithStoredCredentials()` torna null, l'avvio cade sul
 * ramo della sessione persistita, e un'eventuale invalidazione dei token lo
 * scaricherebbe sulla schermata di accesso a digitare la password a mano.
 *
 * Non introduce nessuna esposizione nuova: la password si sposta fra due
 * archivi cifrati della stessa app, entrambi con master key nel Keystore.
 */
object FlutterCredentialMigration {

    /** Nome del file di preferenze usato da `flutter_secure_storage`. */
    private const val FLUTTER_PREFS = "FlutterSecureStorage"

    /**
     * Copia le credenziali Flutter nel [ICredentialStore] dell'app, una volta
     * sola, e poi le rimuove dall'archivio di origine.
     *
     * La rimozione non e' pulizia: e' cio' che rende il logout definitivo.
     * `AuthRepository.logout()` svuota il nostro store, ma non conosce quello
     * di Flutter — senza cancellare l'originale, il riavvio successivo
     * rimigrerebbe le stesse credenziali e rifarebbe il login da solo, contro
     * la volonta' di chi era appena uscito.
     */
    suspend fun run(context: Context, credentials: ICredentialStore) {
        // Chi ha gia' credenziali sue non si tocca: le nostre sono per
        // definizione piu' recenti di quelle lasciate da Flutter.
        if (runCatching { credentials.read() }.getOrNull() != null) return

        // Esistenza controllata sul file, non aprendo le preferenze: aprirle
        // ne creerebbe una copia vuota su ogni dispositivo che Flutter non ha
        // mai visto.
        val prefsFile = File(File(context.applicationInfo.dataDir, "shared_prefs"), "$FLUTTER_PREFS.xml")
        if (!prefsFile.exists()) return

        val prefs = runCatching {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                context,
                FLUTTER_PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }.getOrNull() ?: return

        val all = runCatching { prefs.all }.getOrNull() ?: return

        // Le chiavi non si cercano per nome esatto. `flutter_secure_storage`
        // le antepone un prefisso costante suo, e dipendere dal valore letterale
        // di quel prefisso significherebbe che un cambio di versione del
        // plugin fa fallire la migrazione in silenzio. Il suffisso invece e'
        // la chiave che l'app aveva scritto: `email` e `password`.
        val emailKey = all.keys.firstOrNull { it.matchesLegacyKey("email") } ?: return
        val passwordKey = all.keys.firstOrNull { it.matchesLegacyKey("password") } ?: return

        val email = all[emailKey] as? String
        val password = all[passwordKey] as? String
        if (email.isNullOrBlank() || password.isNullOrBlank()) return

        runCatching { credentials.save(email, password) }
            .onSuccess {
                runCatching {
                    prefs.edit().remove(emailKey).remove(passwordKey).apply()
                }
            }
    }

    private fun String.matchesLegacyKey(name: String): Boolean =
        this == name || endsWith("_$name")
}
