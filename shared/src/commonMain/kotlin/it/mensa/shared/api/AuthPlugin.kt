package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.takeFrom
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.util.AttributeKey
import it.mensa.shared.auth.AuthHolder
import it.mensa.shared.auth.oidc.TokenRefresher
import kotlin.concurrent.Volatile

/**
 * Per-request marker: tells [AuthPlugin] to leave the request alone
 * (no Bearer injection, no proactive refresh, no 401 retry).
 * Used for OIDC discovery (`/.well-known/...`) and the refresh call itself.
 */
val SkipAuthAttribute: AttributeKey<Boolean> = AttributeKey("it.mensa.skipAuth")

private val AuthRetriedAttribute: AttributeKey<Boolean> = AttributeKey("it.mensa.authRetried")

/**
 * Late-binding holder for the [TokenRefresher]. The HTTP client is built
 * eagerly by [HttpClientFactory], but the refresher depends on that same
 * client — Koin breaks the cycle by populating this holder after both
 * have been constructed. Until then the plugin runs in "header-only"
 * mode and falls back to whatever token is in [AuthHolder].
 */
object AuthRefresherHolder {
    @Volatile
    var refresher: TokenRefresher? = null
}

/**
 * Proactive refresh + Bearer injection. If the access token is about to
 * expire and a refresh token is available, refresh synchronously before
 * setting the Authorization header.
 */
val AuthPlugin = createClientPlugin("MensaAuthPlugin") {
    onRequest { request, _ ->
        if (request.attributes.getOrNull(SkipAuthAttribute) == true) return@onRequest

        val refresher = AuthRefresherHolder.refresher
        val session = AuthHolder.session
        if (refresher != null && session != null && session.refreshToken.isNotBlank() && AuthHolder.isExpiringSoon()) {
            runCatching { refresher.refresh() }
        }

        AuthHolder.token?.let { token ->
            request.headers.remove(HttpHeaders.Authorization)
            request.headers.append(HttpHeaders.Authorization, "Bearer $token")
        }
    }
}

/**
 * Reactive 401 → refresh → retry once. Wired by [HttpClientFactory] after
 * the client is built. Lookup of the [TokenRefresher] is lazy (via
 * [AuthRefresherHolder]) for the same DI-cycle reason described above.
 */
fun HttpClient.installAuthRetry() {
    plugin(HttpSend).intercept { request ->
        val call = execute(request)
        if (call.response.status != HttpStatusCode.Unauthorized) return@intercept call
        if (request.attributes.getOrNull(SkipAuthAttribute) == true) return@intercept call
        if (request.attributes.getOrNull(AuthRetriedAttribute) == true) return@intercept call
        val refresher = AuthRefresherHolder.refresher ?: return@intercept call
        val session = AuthHolder.session ?: return@intercept call
        if (session.refreshToken.isBlank()) return@intercept call

        val refreshed = runCatching { refresher.refresh() }.getOrNull() ?: return@intercept call

        val retry = HttpRequestBuilder().apply {
            takeFrom(request)
            attributes.put(AuthRetriedAttribute, true)
            headers.remove(HttpHeaders.Authorization)
            header(HttpHeaders.Authorization, "Bearer ${refreshed.accessToken}")
        }
        execute(retry)
    }
}
