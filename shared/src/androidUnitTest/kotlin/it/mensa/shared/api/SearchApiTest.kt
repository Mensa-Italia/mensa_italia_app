package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.api.endpoints.SearchApi
import it.mensa.shared.model.search.SearchRequest
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SearchApiTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val successResponse = """
        {
          "query": "carbonara",
          "total": 6,
          "results": {
            "event": [
              {"id":"evt1","score":0.9,"title":"Cena Carbonara","subtitle":"Roma","image":"https://img/1.jpg","deep_link":"mensa://event/evt1"},
              {"id":"evt2","score":0.7,"title":"Serata Romana","subtitle":"Milano","image":"","deep_link":"mensa://event/evt2"}
            ],
            "deal": [
              {"id":"deal1","score":0.8,"title":"Sconto Carbonara","subtitle":"Napoli","image":"","deep_link":"mensa://deal/deal1"}
            ],
            "user": [
              {"id":"usr1","score":0.6,"title":"Mario Rossi","subtitle":"Socio","image":"","deep_link":"mensa://user/usr1"},
              {"id":"usr2","score":0.5,"title":"Luigi Verdi","subtitle":"Socio","image":"","deep_link":"mensa://user/usr2"},
              {"id":"usr3","score":0.4,"title":"Anna Bianchi","subtitle":"Socio","image":"","deep_link":"mensa://user/usr3"}
            ]
          }
        }
    """.trimIndent()

    private fun buildClient(responseBody: String): HttpClient {
        val engine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
        }
    }

    @Test
    fun searchDeserializesMultipleTypes() = runTest {
        val api = SearchApi(buildClient(successResponse))

        val response = api.search("carbonara")

        assertEquals("carbonara", response.query)
        assertEquals(6, response.total)
        assertEquals(2, response.hitsFor("event").size)
        assertEquals(1, response.hitsFor("deal").size)
        assertEquals(3, response.hitsFor("user").size)
        assertEquals(6, response.allHits.size)
    }

    @Test
    fun searchDeserializesDeepLinkMapping() = runTest {
        val api = SearchApi(buildClient(successResponse))

        val response = api.search("carbonara")

        val firstEvent = response.hitsFor("event").first()
        assertEquals("evt1", firstEvent.id)
        assertEquals(0.9, firstEvent.score)
        assertEquals("Cena Carbonara", firstEvent.title)
        // Verify snake_case deep_link maps to camelCase deepLink
        assertEquals("mensa://event/evt1", firstEvent.deepLink)
    }

    @Test
    fun searchRequestSerializesSnakeCaseFields() {
        // Verify JSON serialization produces snake_case (no HTTP call needed)
        val request = SearchRequest(q = "test", types = listOf("event"), limitPerType = 5, hydrate = false)
        val serialized = json.encodeToString(SearchRequest.serializer(), request)

        assertTrue(serialized.contains("limit_per_type"),
            "Expected 'limit_per_type' in JSON but got: $serialized")
        assertTrue(serialized.contains("5"),
            "Expected limitPerType=5 in JSON but got: $serialized")
        // hydrate=false should also be present
        assertTrue(serialized.contains("hydrate"),
            "Expected 'hydrate' field in JSON but got: $serialized")
    }

    @Test
    fun searchRetries429WithBackoff() = runTest {
        var callCount = 0
        val engine = MockEngine { _ ->
            callCount++
            if (callCount == 1) {
                respond(
                    content = """{"error":"rate limit"}""",
                    status = HttpStatusCode.TooManyRequests,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            } else {
                respond(
                    content = successResponse,
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
                )
            }
        }
        // Use expectSuccess=false so MockEngine delivers 429 as a response (not exception).
        // SearchApi handles both modes (with or without expectSuccess).
        val client = HttpClient(engine) {
            install(ContentNegotiation) { json(json) }
            expectSuccess = false
        }
        // Use maxRetries=1 so we only retry once; runTest skips the delay()
        val api = SearchApi(client)

        val response = api.search(SearchRequest(q = "test"), maxRetries = 1)

        assertEquals(2, callCount, "Expected exactly 2 calls (1 retry after 429)")
        assertEquals("carbonara", response.query)
    }
}
