package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.*
import io.ktor.http.*
import it.mensa.shared.model.search.SearchRequest
import it.mensa.shared.model.search.SearchResponse
import kotlinx.coroutines.delay

open class SearchApi(private val client: HttpClient) {

    /**
     * Esegue una query. Gestisce automaticamente 429 con backoff esponenziale
     * (1s, 2s, 4s, 8s, 16s, max 30s). Re-lancia ogni altro errore.
     */
    @Throws(Exception::class)
    open suspend fun search(request: SearchRequest, maxRetries: Int = 5): SearchResponse {
        var attempt = 0
        while (true) {
            try {
                // /api/search expects JSON (verified with curl + real token).
                // Only the legacy /api/cs/* endpoints want form-urlencoded.
                return client.post("/api/search") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }.body()
            } catch (e: ClientRequestException) {
                if (e.response.status != HttpStatusCode.TooManyRequests || attempt >= maxRetries) throw e
                delay(minOf(30_000L, 1_000L shl attempt))
                attempt++
            }
        }
    }

    /** Shorthand */
    @Throws(Exception::class)
    open suspend fun search(
        q: String,
        types: List<String>? = null,
        region: String? = null,
        limitPerType: Int = 10,
        hydrate: Boolean = true,
    ): SearchResponse = search(SearchRequest(q, types, region, limitPerType, hydrate))
}
