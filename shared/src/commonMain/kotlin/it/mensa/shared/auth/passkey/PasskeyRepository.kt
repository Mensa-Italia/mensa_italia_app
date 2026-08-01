package it.mensa.shared.auth.passkey

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import it.mensa.shared.api.endpoints.PasskeyApi
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.auth.LoginMethod
import kotlinx.serialization.json.Json

/**
 * Orchestrazione dei due giri a due passi delle passkey (login e registrazione).
 *
 * Non apre nessun prompt di sistema: quello vive negli app module, perche' su
 * Android serve un'`Activity` e su iOS una `presentationAnchor`. Qui c'e' solo
 * l'HTTP e la traduzione degli status in esiti che la UI sa gestire.
 *
 * La sessione resta di competenza di [AuthRepository]: il login riuscito viene
 * consegnato a `adoptTokens`, cosi' persistenza, stato e cache utente hanno una
 * sola implementazione condivisa con il login a password.
 */
class PasskeyRepository(
    private val api: PasskeyApi,
    private val auth: AuthRepository,
    private val json: Json,
) {

    /**
     * Apre una challenge di login per [email].
     *
     * Non solleva eccezioni: ogni fallimento diventa uno status, perche' il
     * percorso passkey non deve mai produrre un errore rosso — al massimo
     * riporta l'utente alla password.
     */
    suspend fun beginLogin(email: String): PasskeyStartResult {
        val response = try {
            api.beginLogin(email.trim().lowercase())
        } catch (e: ResponseException) {
            return PasskeyStartResult(passkeyStartStatusFor(e.response.status))
        } catch (_: Throwable) {
            return PasskeyStartResult(PasskeyStartStatus.NETWORK_ERROR)
        }

        val options = response.public_key_credential_request_options
        if (response.login_id.isBlank() || options == null) {
            return PasskeyStartResult(PasskeyStartStatus.UNAVAILABLE)
        }

        return PasskeyStartResult(
            status = PasskeyStartStatus.READY,
            challenge = PasskeyChallenge(
                loginId = response.login_id,
                requestOptionsJson = json.encodeToString(options),
            ),
        )
    }

    /**
     * Verifica l'assertion prodotta dal device e, se valida, rende attiva la
     * sessione.
     *
     * 410 diventa [PasskeyLoginStatus.CHALLENGE_EXPIRED]: il chiamante puo'
     * rifare un giro di [beginLogin] + prompt. Non lo facciamo qui perche'
     * richiederebbe di riaprire il prompt biometrico, che e' responsabilita'
     * dell'app module.
     */
    suspend fun finishLogin(loginId: String, credentialJson: String): PasskeyLoginResult {
        val tokens = try {
            api.finishLogin(loginId, credentialJson)
        } catch (e: ResponseException) {
            return PasskeyLoginResult(passkeyLoginStatusFor(e.response.status))
        } catch (_: Throwable) {
            return PasskeyLoginResult(PasskeyLoginStatus.NETWORK_ERROR)
        }

        val user = try {
            auth.adoptTokens(
                tokens = tokens,
                method = LoginMethod.PASSKEY,
                source = "/auth-with-passkey/finish",
            )
        } catch (_: Throwable) {
            // Token validi ma inutilizzabili (record mancante, sessione non
            // persistibile): trattarlo come rifiuto manda l'utente sulla
            // password, che e' l'unica strada che puo' rimettere le cose a posto.
            return PasskeyLoginResult(PasskeyLoginStatus.REJECTED)
        }

        return PasskeyLoginResult(PasskeyLoginStatus.SUCCESS, user)
    }

    /**
     * Avvia la registrazione di una passkey per l'utente in sessione.
     *
     * Le creation options arrivano con `excludeCredentials` gia' popolato da
     * Zitadel: se su questo device esiste gia' una passkey per l'account,
     * l'authenticator rifiuta e l'errore emerge nell'app module, non qui.
     *
     * Ritorna null se il server non e' in grado di avviare la registrazione.
     */
    suspend fun beginRegistration(): PasskeyRegistrationChallenge? {
        val response = try {
            api.beginRegistration()
        } catch (_: Throwable) {
            return null
        }

        val options = response.public_key_credential_creation_options
        if (response.passkey_id.isBlank() || options == null) return null

        return PasskeyRegistrationChallenge(
            passkeyId = response.passkey_id,
            creationOptionsJson = json.encodeToString(options),
        )
    }

    /**
     * Conferma la registrazione. [name] e' l'etichetta in lista: passare il
     * modello del dispositivo, altrimenti con tre passkey la lista e'
     * indistinguibile.
     *
     * Ritorna un `Boolean` e non un `Result` di proposito: `kotlin.Result` e' una
     * value class e attraversando il bridge ObjC arriva a Swift come oggetto
     * comunque non-nil, anche quando incapsula un fallimento. Un chiamante Swift
     * che controlla solo il nil scambierebbe un errore per un successo — cioe' un
     * fallimento silenzioso proprio nel punto in cui l'utente crede di aver
     * attivato la passkey.
     */
    suspend fun finishRegistration(
        passkeyId: String,
        credentialJson: String,
        name: String,
    ): Boolean = runCatching { api.finishRegistration(passkeyId, credentialJson, name) }.isSuccess

    /** Le passkey dell'utente. Lista vuota anche in caso di errore di rete. */
    suspend fun list(): List<PasskeyModel> = runCatching { api.list() }.getOrDefault(emptyList())

    /**
     * Come [list] ma distingue "nessuna passkey" da "non lo so": serve al gate
     * del prompt, che non deve proporre l'attivazione basandosi su una lista
     * vuota solo perche' la rete era giu'.
     */
    suspend fun listOrNull(): List<PasskeyModel>? = runCatching { api.list() }.getOrNull()

    /** `Boolean` per la stessa ragione di [finishRegistration]. */
    suspend fun remove(passkeyId: String): Boolean = runCatching { api.remove(passkeyId) }.isSuccess
}

/**
 * Traduzione status HTTP -> esito, per l'apertura della challenge.
 *
 * Funzione a parte, e non un `when` inline, perche' questa mappatura *e'* il
 * contratto della matrice di degradazione: e' l'unica cosa che decide se
 * l'utente vede "nessuna passkey per questa email" o "server non raggiungibile".
 * Isolata si verifica senza montare un client HTTP finto.
 */
internal fun passkeyStartStatusFor(status: HttpStatusCode): PasskeyStartStatus =
    when (status) {
        // 409: il server non distingue "email sconosciuta" da "nessuna passkey".
        HttpStatusCode.Conflict -> PasskeyStartStatus.UNAVAILABLE
        else -> PasskeyStartStatus.NETWORK_ERROR
    }

/** Traduzione status HTTP -> esito, per la verifica dell'assertion. */
internal fun passkeyLoginStatusFor(status: HttpStatusCode): PasskeyLoginStatus =
    when (status) {
        // 410: challenge scaduta, l'utente ha esitato. Merita un secondo giro.
        HttpStatusCode.Gone -> PasskeyLoginStatus.CHALLENGE_EXPIRED
        HttpStatusCode.Conflict -> PasskeyLoginStatus.UNAVAILABLE
        HttpStatusCode.Unauthorized, HttpStatusCode.BadRequest -> PasskeyLoginStatus.REJECTED
        else -> PasskeyLoginStatus.NETWORK_ERROR
    }
