package it.mensa.shared.auth.oidc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.forms.FormDataContent
import io.ktor.http.HttpStatusCode
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import it.mensa.shared.api.SkipAuthAttribute
import it.mensa.shared.auth.AuthHolder
import it.mensa.shared.auth.ITokenStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile

/**
 * Refreshes the access token against the OIDC `token_endpoint` discovered
 * for the current session's issuer. Guarded by a [Mutex] so concurrent API
 * calls cannot fire overlapping refresh requests.
 *
 * Public-client / PKCE-style: no client secret, just
 * `grant_type=refresh_token`, `refresh_token`, `client_id`.
 *
 * On failure, the session is wiped and listeners ([onSessionLost]) are
 * notified so [AuthRepository] can drop to anonymous.
 */
class TokenRefresher(
    private val client: HttpClient,
    private val tokenStore: ITokenStore,
    private val json: Json,
) {
    private val mutex = Mutex()

    @Volatile
    var onSessionLost: (() -> Unit)? = null

    suspend fun refresh(): OidcSession = refreshInternal(force = false)

    /** Bypass the expiry check — used when /api/cs/me failed and we suspect
     *  the access token is stale even though its `exp` is in the future. */
    suspend fun forceRefresh(): OidcSession = refreshInternal(force = true)

    private suspend fun refreshInternal(force: Boolean): OidcSession = mutex.withLock {
        val current = AuthHolder.session ?: error("No active session to refresh")

        // Another coroutine may have refreshed while we were waiting on the lock.
        if (!force && current.expiresAtEpochSeconds - Clock.System.now().epochSeconds > REFRESH_SKEW_SECONDS) {
            return@withLock current
        }

        val response = try {
            client.post(current.tokenEndpoint) {
                attributes.put(SkipAuthAttribute, true)
                header(HttpHeaders.Authorization, null)
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("grant_type", "refresh_token")
                            append("refresh_token", current.refreshToken)
                            append("client_id", current.clientId)
                        }
                    )
                )
            }.body<OidcTokenResponse>()
        } catch (e: ResponseException) {
            // Distinguish "auth server says this grant is dead" (400/401 —
            // RFC 6749 invalid_grant / invalid_client) from "auth server is
            // having a bad day" (5xx). Only the first kills the session;
            // 5xx is treated like a transport failure and bubbles up without
            // clearing local state.
            val s = e.response.status
            val grantDead = s == HttpStatusCode.BadRequest ||
                s == HttpStatusCode.Unauthorized ||
                s == HttpStatusCode.Forbidden
            if (grantDead) {
                AuthHolder.session = null
                runCatching { tokenStore.clear() }
                onSessionLost?.invoke()
            }
            throw e
        } catch (t: Throwable) {
            // Network / transport failure — leave the session intact so a
            // later retry (when connectivity returns) can succeed.
            throw t
        }

        val updated = current.copy(
            accessToken = response.access_token,
            refreshToken = response.refresh_token ?: current.refreshToken,
            idToken = response.id_token ?: current.idToken,
            expiresAtEpochSeconds = Clock.System.now().epochSeconds + response.expires_in - EXPIRES_SAFETY_MARGIN_SECONDS,
        )
        AuthHolder.session = updated
        runCatching { tokenStore.save(json.encodeToString(OidcSession.serializer(), updated)) }
        updated
    }

    companion object {
        private const val REFRESH_SKEW_SECONDS = 60L
        private const val EXPIRES_SAFETY_MARGIN_SECONDS = 30L
    }
}
