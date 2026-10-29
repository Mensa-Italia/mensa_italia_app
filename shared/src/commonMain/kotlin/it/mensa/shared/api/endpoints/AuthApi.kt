package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import it.mensa.shared.api.SkipAuthAttribute
import it.mensa.shared.auth.oidc.OidcTokenResponse
import it.mensa.shared.model.UserModel

class AuthApi(private val client: HttpClient) {
    /**
     * `POST /api/cs/auth-with-zitadel` — form-urlencoded body. Returns OIDC
     * tokens (access / refresh / id) and optionally a PocketBase user record.
     * No Authorization header on the request: a stale Bearer would either
     * be ignored or, worse, cause a 401 from the auth proxy.
     */
    @Throws(Exception::class)
    suspend fun loginWithZitadel(email: String, password: String): OidcTokenResponse {
        val response = client.post("/api/cs/auth-with-zitadel") {
            attributes.put(SkipAuthAttribute, true)
            header(HttpHeaders.Authorization, null)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("email", email)
                        append("password", password)
                    }
                )
            )
        }
        // Il client non usa `expectSuccess`: senza questi controlli il JSON
        // d'errore veniva letto come una risposta di token vuota, e chi
        // sbagliava password (o trovava il server giu') si vedeva
        // "Empty access_token…".
        if (response.status == HttpStatusCode.Unauthorized) throw InvalidCredentialsException()
        if (!response.status.isSuccess()) throw LoginUnavailableException(response.status.value)
        return response.body()
    }

    /**
     * `GET /api/cs/me` — returns the current [UserModel] for the bearer
     * access token. Used after token refresh (the OIDC refresh response
     * carries only tokens) and on cold start to keep the cached profile
     * fresh (membership state, powers, addons can change server-side).
     */
    @Throws(Exception::class)
    suspend fun me(): UserModel = client.get("/api/cs/me").body()

    /**
     * Una lettura leggera su una route che i `blocked` NON possono usare
     * (`IntegrityGuard` la conosce come fuori da `BlockedAllowlist`): se il
     * server risponde 2xx l'account non e' piu' sospeso e lo stato globale si
     * spegne da solo; se risponde 403 `account_blocked` solleva
     * `AccountBlockedException`. `/api/cs/me` non va bene a questo scopo, perche'
     * il server potrebbe servirlo anche a un account sospeso.
     */
    @Throws(Exception::class)
    suspend fun probeAccess() {
        client.get("/api/collections/events/records") {
            parameter("perPage", 1)
            parameter("skipTotal", 1)
        }
    }
}

/** `/auth-with-zitadel` ha rifiutato email e password (401). */
class InvalidCredentialsException : Exception("Email o password non corrette")

/**
 * Il login non si puo' completare per un motivo che non dipende dalla password:
 * troppi tentativi (429), area soci o Zitadel irraggiungibili (5xx), oppure una
 * risposta riuscita ma inservibile (senza token o senza record utente).
 *
 * Il messaggio e' gia' quello da mostrare, come per [InvalidCredentialsException];
 * cosa mancava di preciso sta in [detail], per i log.
 */
class LoginUnavailableException(
    /** Lo stato HTTP; null se il server ha risposto 2xx senza i dati del login. */
    val status: Int? = null,
    val detail: String? = null,
) : Exception(
    when (status) {
        HttpStatusCode.TooManyRequests.value -> "Troppi tentativi di accesso. Riprova fra qualche minuto."
        null -> "Accesso non disponibile in questo momento. Riprova fra poco."
        else -> "Accesso non disponibile in questo momento (HTTP $status). Riprova fra poco."
    },
)
