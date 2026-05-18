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
import it.mensa.shared.api.endpoints.LocationsApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LocationsApiTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

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
    fun locateStateParsesStateField() = runTest {
        val stateJson = """{"state": "Lombardia"}"""

        val client = buildClient(stateJson)
        val pb = PocketBaseClient(client)
        val api = LocationsApi(pb, client)

        val response = api.locateState(45.4642, 9.1900)

        assertEquals("Lombardia", response.state)
    }

    @Test
    fun locateStateHandlesEmptyState() = runTest {
        val stateJson = """{"state": ""}"""

        val client = buildClient(stateJson)
        val pb = PocketBaseClient(client)
        val api = LocationsApi(pb, client)

        val response = api.locateState(0.0, 0.0)

        assertEquals("", response.state)
    }

    @Test
    fun listParsesMultipleLocations() = runTest {
        val locationsJson = """
            {
              "page": 1,
              "perPage": 200,
              "totalItems": 2,
              "totalPages": 1,
              "items": [
                {
                  "id": "loc1",
                  "name": "Palazzo Mensa",
                  "lat": 45.4642,
                  "lon": 9.1900,
                  "address": "Via Roma 1, Milano",
                  "state": "Lombardia"
                },
                {
                  "id": "loc2",
                  "name": "Sala Riunioni",
                  "lat": 41.9028,
                  "lon": 12.4964,
                  "address": "Piazza Navona, Roma",
                  "state": "Lazio"
                }
              ]
            }
        """.trimIndent()

        val client = buildClient(locationsJson)
        val pb = PocketBaseClient(client)
        val api = LocationsApi(pb, client)

        val items = api.list()
        assertEquals(2, items.size)
        assertEquals("loc1", items[0].id)
        assertEquals("Palazzo Mensa", items[0].name)
        assertEquals("Lombardia", items[0].state)
        assertEquals("loc2", items[1].id)
        assertEquals("Lazio", items[1].state)
    }
}
