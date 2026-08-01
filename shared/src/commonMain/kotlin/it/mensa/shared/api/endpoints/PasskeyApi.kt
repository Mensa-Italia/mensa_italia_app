package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import it.mensa.shared.api.SkipAuthAttribute
import it.mensa.shared.auth.oidc.OidcTokenResponse
import it.mensa.shared.auth.passkey.PasskeyListResponse
import it.mensa.shared.auth.passkey.PasskeyLoginChallengeResponse
import it.mensa.shared.auth.passkey.PasskeyModel
import it.mensa.shared.auth.passkey.PasskeyRegistrationChallengeResponse

/**
 * Endpoint passkey. Le due rotte di login girano senza `Authorization` per la
 * stessa ragione di [AuthApi.loginWithZitadel]: un Bearer scaduto sarebbe nel
 * migliore dei casi ignorato, nel peggiore causerebbe un 401 dal proxy.
 * Le quattro rotte di gestione richiedono invece la sessione corrente.
 */
class PasskeyApi(private val client: HttpClient) {

    /** `POST /api/cs/auth-with-passkey/begin` — apre la challenge per `email`. */
    suspend fun beginLogin(email: String): PasskeyLoginChallengeResponse =
        client.post("/api/cs/auth-with-passkey/begin") {
            attributes.put(SkipAuthAttribute, true)
            header(HttpHeaders.Authorization, null)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("email", email)
                    }
                )
            )
        }.body()

    /**
     * `POST /api/cs/auth-with-passkey/finish` — verifica l'assertion.
     * Risponde con la stessa forma di `/auth-with-zitadel`, quindi riusiamo
     * [OidcTokenResponse] senza adattamenti.
     */
    suspend fun finishLogin(loginId: String, credentialJson: String): OidcTokenResponse =
        client.post("/api/cs/auth-with-passkey/finish") {
            attributes.put(SkipAuthAttribute, true)
            header(HttpHeaders.Authorization, null)
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("login_id", loginId)
                        append("credential", credentialJson)
                    }
                )
            )
        }.body()

    /** `POST /api/cs/passkeys/begin` — avvia la registrazione per l'utente in sessione. */
    suspend fun beginRegistration(): PasskeyRegistrationChallengeResponse =
        client.post("/api/cs/passkeys/begin").body()

    /** `POST /api/cs/passkeys/finish` — conferma la registrazione. */
    suspend fun finishRegistration(passkeyId: String, credentialJson: String, name: String) {
        client.post("/api/cs/passkeys/finish") {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("passkey_id", passkeyId)
                        append("credential", credentialJson)
                        append("name", name)
                    }
                )
            )
        }
    }

    /** `GET /api/cs/passkeys` */
    suspend fun list(): List<PasskeyModel> =
        client.get("/api/cs/passkeys").body<PasskeyListResponse>().passkeys

    /** `DELETE /api/cs/passkeys/{id}` */
    suspend fun remove(passkeyId: String) {
        client.delete("/api/cs/passkeys/$passkeyId")
    }
}
