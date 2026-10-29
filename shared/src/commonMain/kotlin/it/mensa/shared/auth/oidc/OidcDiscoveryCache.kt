package it.mensa.shared.auth.oidc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import it.mensa.shared.api.SkipAuthAttribute
import it.mensa.shared.api.endpoints.LoginUnavailableException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fetches and memoises `.well-known/openid-configuration` per issuer.
 * Discovery is normally stable for the lifetime of the app; we keep it in
 * memory only and re-fetch on cold start. The HTTP call strips Authorization
 * because the issuer doesn't accept (and may reject) a bearer of another
 * audience.
 */
class OidcDiscoveryCache(private val client: HttpClient) {
    private val cache = mutableMapOf<String, OidcDiscovery>()
    private val mutex = Mutex()

    /**
     * Una risposta non 2xx o senza `token_endpoint` solleva
     * [LoginUnavailableException] e non entra in cache: un errore momentaneo
     * dell'issuer, tenuto fino al riavvio, faceva fallire ogni login successivo.
     */
    @Throws(Exception::class)
    suspend fun get(issuer: String): OidcDiscovery = mutex.withLock {
        cache[issuer]?.let { return@withLock it }
        val url = issuer.trimEnd('/') + "/.well-known/openid-configuration"
        val response = client.get(url) {
            // Bypass the auth plugin: discovery is an unauthenticated request
            // against the issuer (auth.mensa.it), not against svc.mensa.it.
            attributes.put(SkipAuthAttribute, true)
            header(HttpHeaders.Authorization, null)
            // Lo stato lo traduce qui sotto, nell'errore che il login sa mostrare.
            expectSuccess = false
        }
        if (!response.status.isSuccess()) {
            throw LoginUnavailableException(response.status.value, "OIDC discovery for $issuer")
        }
        val discovery: OidcDiscovery = response.body()
        if (discovery.token_endpoint.isBlank()) {
            throw LoginUnavailableException(detail = "OIDC discovery for $issuer returned empty token_endpoint")
        }
        cache[issuer] = discovery
        discovery
    }

    fun seed(issuer: String, discovery: OidcDiscovery) {
        cache[issuer] = discovery
    }
}
