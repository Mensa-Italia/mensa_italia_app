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
     *
     * Compatibile sia con HttpClient configurato con expectSuccess=true
     * (default, lancia ClientRequestException per 4xx) sia con expectSuccess=false
     * (gestione manuale dello status).
     */
    open suspend fun search(request: SearchRequest, maxRetries: Int = 5): SearchResponse {
        var attempt = 0
        while (true) {
            var statusCode = 0
            try {
                // /api/search expects JSON (verified with curl + real token).
                // Only the legacy /api/cs/* endpoints want form-urlencoded.
                val response = client.post("/api/search") {
                    contentType(ContentType.Application.Json)
                    setBody(request)
                }
                statusCode = response.status.value
                if (statusCode == 429) {
                    // expectSuccess=false path: got a 429 response without exception
                    if (attempt < maxRetries) {
                        val backoff = minOf(30_000L, 1_000L shl attempt)
                        delay(backoff)
                        attempt++
                        continue
                    } else {
                        throw ClientRequestException(response, "429 Too Many Requests — max retries exceeded")
                    }
                }
                return response.body()
            } catch (e: ClientRequestException) {
                // expectSuccess=true path: 4xx throws before we see the response
                val code = e.response.status.value
                if (code == 429 && attempt < maxRetries) {
                    val backoff = minOf(30_000L, 1_000L shl attempt)
                    delay(backoff)
                    attempt++
                } else {
                    throw e
                }
            }
        }
    }

    /** Shorthand */
    open suspend fun search(
        q: String,
        types: List<String>? = null,
        region: String? = null,
        limitPerType: Int = 10,
        hydrate: Boolean = true,
    ): SearchResponse = search(SearchRequest(q, types, region, limitPerType, hydrate))
}
