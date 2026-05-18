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
import it.mensa.shared.api.endpoints.NotificationsApi
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationsApiTest {

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
    fun listDeserializesTwoNotifications() = runTest {
        val responseJson = """
            {
              "page": 1,
              "perPage": 200,
              "totalItems": 2,
              "totalPages": 1,
              "items": [
                {
                  "id": "notif1",
                  "tr": "notification.welcome",
                  "tr_named_params": {"name": "Mario"},
                  "data": null,
                  "seen": null,
                  "created": "2024-01-15T10:00:00Z",
                  "updated": "2024-01-15T10:00:00Z"
                },
                {
                  "id": "notif2",
                  "tr": "notification.event",
                  "tr_named_params": {"event": "Assembly 2024"},
                  "data": {"event_id": "evt1"},
                  "seen": "2024-01-16T08:00:00Z",
                  "created": "2024-01-15T12:00:00Z",
                  "updated": "2024-01-16T08:00:00Z"
                }
              ]
            }
        """.trimIndent()

        val client = buildClient(responseJson)
        val pb = PocketBaseClient(client)
        val api = NotificationsApi(pb, client)

        val items = api.list()

        assertEquals(2, items.size)

        val first = items[0]
        assertEquals("notif1", first.id)
        assertEquals("notification.welcome", first.tr)
        assertEquals(mapOf("name" to "Mario"), first.trNamedParams)
        assertNull(first.seen)

        val second = items[1]
        assertEquals("notif2", second.id)
        assertEquals("notification.event", second.tr)
        assertEquals(mapOf("event" to "Assembly 2024"), second.trNamedParams)
        assertEquals("evt1", second.data?.get("event_id").toString().trim('"'))
    }

    @Test
    fun listEmptyResponseReturnsEmptyList() = runTest {
        val emptyResponse = """
            {
              "page": 1,
              "perPage": 200,
              "totalItems": 0,
              "totalPages": 0,
              "items": []
            }
        """.trimIndent()

        val client = buildClient(emptyResponse)
        val pb = PocketBaseClient(client)
        val api = NotificationsApi(pb, client)

        val items = api.list()
        assertEquals(0, items.size)
    }
}
