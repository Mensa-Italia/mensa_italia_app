package it.mensa.app.features.auth

import android.content.Context
import android.os.Build
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException

/**
 * Prompt di sistema per creare e usare le passkey, via Credential Manager.
 *
 * Il bridge qui e' sottile perche' `CreatePublicKeyCredentialRequest` e
 * `GetPublicKeyCredentialOption` prendono e restituiscono direttamente JSON
 * WebAuthn: le options arrivano dal backend gia' nella forma giusta e la
 * risposta si rigira al backend cosi' com'e'. (Su iOS invece ASAuthorization
 * vuole i campi decodificati, e il JSON di risposta va ricostruito a mano.)
 *
 * Vive nell'app module e non nello `shared` perche' serve un [Context] di
 * Activity per mostrare il prompt.
 */
class PasskeyAuthenticator(private val activityContext: Context) {

    private val credentialManager = CredentialManager.create(activityContext)

    /**
     * Esegue l'assertion. [requestOptionsJson] e' il JSON che arriva da
     * `/api/cs/auth-with-passkey/begin`.
     */
    suspend fun assert(requestOptionsJson: String): PasskeyPromptResult {
        if (!isSupported) return PasskeyPromptResult.Unsupported

        return try {
            val response = credentialManager.getCredential(
                context = activityContext,
                request = GetCredentialRequest(
                    listOf(GetPublicKeyCredentialOption(requestJson = requestOptionsJson)),
                ),
            )
            val credential = response.credential as? PublicKeyCredential
                ?: return PasskeyPromptResult.Failed("unexpected credential type")
            PasskeyPromptResult.Success(credential.authenticationResponseJson)
        } catch (_: NoCredentialException) {
            // Nessuna passkey su questo dispositivo. Non e' un errore: e' il caso
            // di chi ha la passkey su un altro telefono, o non l'ha mai creata.
            PasskeyPromptResult.NoCredential
        } catch (_: GetCredentialCancellationException) {
            PasskeyPromptResult.Cancelled
        } catch (e: GetCredentialException) {
            PasskeyPromptResult.Failed(e.errorMessage?.toString() ?: e.type)
        }
    }

    /**
     * Crea una passkey. [creationOptionsJson] arriva da `/api/cs/passkeys/begin`
     * e porta gia' `excludeCredentials` popolato da Zitadel: se su questo
     * dispositivo esiste gia' una passkey per l'account, e' l'authenticator a
     * rifiutare, e la cosa emerge qui come [PasskeyPromptResult.AlreadyRegistered].
     */
    suspend fun create(creationOptionsJson: String): PasskeyPromptResult {
        if (!isSupported) return PasskeyPromptResult.Unsupported

        return try {
            val response = credentialManager.createCredential(
                context = activityContext,
                request = CreatePublicKeyCredentialRequest(requestJson = creationOptionsJson),
            )
            val created = response as? CreatePublicKeyCredentialResponse
                ?: return PasskeyPromptResult.Failed("unexpected create response type")
            PasskeyPromptResult.Success(created.registrationResponseJson)
        } catch (_: CreateCredentialCancellationException) {
            PasskeyPromptResult.Cancelled
        } catch (e: CreateCredentialException) {
            if (e.isAlreadyRegistered()) {
                PasskeyPromptResult.AlreadyRegistered
            } else {
                PasskeyPromptResult.Failed(e.errorMessage?.toString() ?: e.type)
            }
        }
    }

    /**
     * Il duplicato si riconosce dal `InvalidStateError` del DOM, che e' quello
     * che WebAuthn usa quando `excludeCredentials` fa match.
     *
     * Il match e' su stringhe e non sulla gerarchia `DomError` di proposito: e'
     * la parte di API di Credential Manager che si e' mossa piu' fra le versioni,
     * mentre `type` ed `errorMessage` sono stabili. Stessa scelta fatta lato
     * server con i codici d'errore di Zitadel (vedi `isPasswordError` in
     * MensaCore/tools/zauth/login.go).
     */
    private fun CreateCredentialException.isAlreadyRegistered(): Boolean {
        val haystack = "$type ${errorMessage ?: ""}".lowercase()
        return "invalidstateerror" in haystack || "already registered" in haystack
    }

    companion object {
        /**
         * Credential Manager esiste da API 21 ma le passkey servono API 28+.
         * Sotto quella soglia il bottone non va nemmeno mostrato: `minSdk` qui
         * e' 24, quindi il caso e' reale.
         */
        val isSupported: Boolean
            get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

        /** Stesso limite applicato dal server (`passkeyNameMaxLen`). */
        private const val MAX_NAME_LEN = 100

        /**
         * Etichetta con cui registrare la passkey: il modello del dispositivo.
         * Senza, una lista con tre voci "Passkey" e' inutilizzabile.
         */
        fun deviceLabel(): String {
            val model = Build.MODEL.orEmpty().trim()
            val manufacturer = Build.MANUFACTURER.orEmpty().trim()
            val composed = when {
                model.isEmpty() && manufacturer.isEmpty() -> "Android"
                model.startsWith(manufacturer, ignoreCase = true) -> model
                manufacturer.isEmpty() -> model
                else -> "$manufacturer $model"
            }
            return composed.take(MAX_NAME_LEN)
        }
    }
}

/** Esito di un prompt di sistema. */
sealed interface PasskeyPromptResult {
    /** [credentialJson] va rigirato al backend cosi' com'e'. */
    data class Success(val credentialJson: String) : PasskeyPromptResult

    /** L'utente ha chiuso il prompt. Nessun messaggio: non e' un errore. */
    data object Cancelled : PasskeyPromptResult

    /** Nessuna passkey utilizzabile su questo dispositivo. */
    data object NoCredential : PasskeyPromptResult

    /** Su questo dispositivo esiste gia' una passkey per l'account. */
    data object AlreadyRegistered : PasskeyPromptResult

    /** Dispositivo troppo vecchio (API < 28). */
    data object Unsupported : PasskeyPromptResult

    data class Failed(val message: String?) : PasskeyPromptResult
}
