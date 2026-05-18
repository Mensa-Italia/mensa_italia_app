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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PocketBaseClientTest {

    private fun buildMockClient(responseBody: String): HttpClient {
        val engine = MockEngine { _ ->
            respond(
                content = responseBody,
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true })
            }
        }
    }

    @Test
    fun listDeserializesPbListResponse() = runTest {
        val json = """
            {
              "page": 1,
              "perPage": 50,
              "totalItems": 2,
              "totalPages": 1,
              "items": [
                { "id": "evt1", "name": "Raduno 2024", "description": "Desc1",
                  "when_start": "2024-06-01T10:00:00Z", "when_end": "2024-06-02T18:00:00Z",
                  "owner": "usr1", "is_national": true, "is_spot": false,
                  "booking_link": "", "info_link": "" },
                { "id": "evt2", "name": "Meeting locale", "description": "Desc2",
                  "when_start": "2024-07-15T09:00:00Z", "when_end": "2024-07-15T12:00:00Z",
                  "owner": "usr2", "is_national": false, "is_spot": true,
                  "booking_link": "https://example.com", "info_link": "" }
              ]
            }
        """.trimIndent()

        val client = buildMockClient(json)
        val pb = PocketBaseClient(client)

        val resp = pb.list<it.mensa.shared.model.EventModel>("events")

        assertEquals(1, resp.page)
        assertEquals(2, resp.totalItems)
        assertEquals(2, resp.items.size)
        assertEquals("evt1", resp.items[0].id)
        assertEquals("Raduno 2024", resp.items[0].name)
        assertEquals("evt2", resp.items[1].id)
        assertEquals(true, resp.items[1].isSpot)
    }

    @Test
    fun fullListPaginatesUntilDone() = runTest {
        var page = 0
        val engine = MockEngine { _ ->
            page++
            val items = if (page == 1) {
                """[{"id":"s1","name":"Sig A","description":"","image":"","link":"","group_type":"sig"}]"""
            } else {
                """[]"""
            }
            respond(
                content = """{"page":$page,"perPage":200,"totalItems":1,"totalPages":1,"items":$items}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        val httpClient = HttpClient(engine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true })
            }
        }
        val pb = PocketBaseClient(httpClient)

        val result = pb.fullList<it.mensa.shared.model.SigModel>("sigs")

        assertEquals(1, result.size)
        assertEquals("Sig A", result[0].name)
    }
}
