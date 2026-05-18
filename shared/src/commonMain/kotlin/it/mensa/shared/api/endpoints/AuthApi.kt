package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
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
    suspend fun loginWithZitadel(email: String, password: String): OidcTokenResponse =
        client.post("/api/cs/auth-with-zitadel") {
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
        }.body()

    /**
     * `GET /api/cs/me` — returns the current [UserModel] for the bearer
     * access token. Used after token refresh (the OIDC refresh response
     * carries only tokens) and on cold start to keep the cached profile
     * fresh (membership state, powers, addons can change server-side).
     */
    suspend fun me(): UserModel = client.get("/api/cs/me").body()
}
