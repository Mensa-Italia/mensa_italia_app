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
 * When the issuer rejects the refresh_token, [onSessionLost] decides: a new
 * session (a fresh login) makes the refresh succeed with it, null closes the
 * session and the refresh fails with [SessionLostException]. Any other failure
 * (5xx, network) leaves the session as it is.
 */
class TokenRefresher(
    private val client: HttpClient,
    private val tokenStore: ITokenStore,
    private val json: Json,
) {
    private val mutex = Mutex()

    /**
     * Chiamata, sotto il lock del rinnovo, quando l'issuer rifiuta il
     * refresh_token. Chi possiede la sessione ([AuthRepository]) puo' rifarne
     * una con un login vero e restituirla: il rinnovo riesce con quella, e le
     * richieste in coda sul lock la trovano gia' pronta. Null vuol dire che la
     * sessione e' chiusa.
     */
    @Volatile
    var onSessionLost: (suspend () -> OidcSession?)? = null

    /** Rinnova se il token scade entro [REFRESH_SKEW_SECONDS]. */
    @Throws(Exception::class)
    suspend fun refresh(): OidcSession =
        refreshUnless { it.expiresAtEpochSeconds - Clock.System.now().epochSeconds > REFRESH_SKEW_SECONDS }

    /** Bypass the expiry check — used when /api/cs/me failed and we suspect
     *  the access token is stale even though its `exp` is in the future. */
    @Throws(Exception::class)
    suspend fun forceRefresh(): OidcSession = refreshUnless { false }

    /**
     * Il server ha rifiutato [rejectedAccessToken] con un 401: rinnova, a meno
     * che un'altra richiesta non l'abbia gia' sostituito mentre questa
     * aspettava il lock.
     *
     * [refresh] qui non basta, perche' decide dalla scadenza scritta nel token:
     * un token rifiutato prima di quella (orologio del telefono indietro,
     * sessione chiusa sul server) non veniva mai rinnovato, e il retry del 401
     * ripartiva con lo stesso token.
     */
    @Throws(Exception::class)
    suspend fun refreshRejected(rejectedAccessToken: String): OidcSession =
        refreshUnless { it.accessToken != rejectedAccessToken }

    /** [stillGood] decide, sotto lock, se la sessione corrente va tenuta com'e'. */
    private suspend fun refreshUnless(stillGood: (OidcSession) -> Boolean): OidcSession = mutex.withLock {
        val current = AuthHolder.session ?: throw SessionLostException()

        // Another coroutine may have refreshed while we were waiting on the lock.
        if (stillGood(current)) return@withLock current

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
            // Distinguish "auth server says this grant is dead" (400/401/403 —
            // RFC 6749 invalid_grant / invalid_client) from "auth server is
            // having a bad day" (5xx, 429). Only the first ends the session;
            // the rest is treated like a transport failure and bubbles up
            // without clearing local state.
            val s = e.response.status
            val grantDead = s == HttpStatusCode.BadRequest ||
                s == HttpStatusCode.Unauthorized ||
                s == HttpStatusCode.Forbidden
            if (!grantDead) throw e
            // Rifiutato il refresh_token di una sessione che nel frattempo e'
            // stata sostituita (un login) o chiusa: non e' piu' affare nostro.
            replacementOf(current, e)?.let { return@withLock it }
            AuthHolder.session = null
            runCatching { tokenStore.clear() }
            onSessionLost?.invoke()?.let { return@withLock it }
            throw SessionLostException(e)
        }
        // Network / transport failures propagate as they are and leave the
        // session intact, so a later retry (when connectivity returns) can succeed.

        // Un 200 senza access_token non e' un rinnovo: salvarlo lasciava la
        // sessione con un token vuoto fino al login successivo.
        check(response.access_token.isNotBlank()) { "Token endpoint returned no access_token" }

        // Un rinnovo arrivato dopo un login o un logout non deve sovrascriverli.
        replacementOf(current)?.let { return@withLock it }

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

    /**
     * La sessione che ha preso il posto di [current] mentre il rinnovo era in
     * volo (un login), o null se [current] e' ancora quella attiva. Se nel
     * frattempo la sessione e' stata chiusa (un logout) solleva
     * [SessionLostException].
     *
     * Il confronto e' sul refresh_token e non sull'istanza: l'avvio rilegge la
     * stessa sessione dal disco, e scambiarla per un'altra vorrebbe dire
     * buttare un rinnovo riuscito, con un refresh_token ormai ruotato.
     */
    private fun replacementOf(current: OidcSession, cause: Throwable? = null): OidcSession? {
        val now = AuthHolder.session ?: throw SessionLostException(cause)
        return now.takeIf { it.refreshToken != current.refreshToken }
    }

    companion object {
        private const val REFRESH_SKEW_SECONDS = 60L
        private const val EXPIRES_SAFETY_MARGIN_SECONDS = 30L
    }
}

/**
 * L'issuer ha rifiutato il refresh_token (400 invalid_grant, 401, 403): la
 * sessione e' finita, e quando arriva a chi chiama e' gia' chiusa (vedi
 * [TokenRefresher.onSessionLost]). Un tipo a parte perche' chi chiama non
 * debba ricavarlo dallo stato HTTP: un 5xx dell'issuer non e' questo.
 */
class SessionLostException(cause: Throwable? = null) : Exception("Session lost: refresh_token rejected", cause)
