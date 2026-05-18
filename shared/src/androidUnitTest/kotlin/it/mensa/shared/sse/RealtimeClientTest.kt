package it.mensa.shared.sse

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RealtimeClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    // -----------------------------------------------------------------------
    // Smoke test: RealtimeClient can be instantiated and observe() returns Flow
    // -----------------------------------------------------------------------
    @Test
    fun `observe returns a non-null Flow without starting collection`() {
        val engine = MockEngine { _ ->
            respond(
                content = "",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString()
                )
            )
        }
        val client = HttpClient(engine) {
            install(SSE)
            install(ContentNegotiation) { json(json) }
        }
        val realtimeClient = RealtimeClient(client, json)

        val flow: Flow<RealtimeEvent> = realtimeClient.observe(setOf("user_notifications/*"))
        assertNotNull(flow)
    }

    // -----------------------------------------------------------------------
    // Unit test: RealtimeMessage parsing
    // -----------------------------------------------------------------------
    @Test
    fun `RealtimeMessage deserialization from JSON`() {
        val raw = """
            {
              "action": "create",
              "record": {
                "id": "abc",
                "collectionId": "x",
                "collectionName": "user_notifications",
                "seen": null,
                "tr": "notif.test",
                "tr_named_params": {},
                "created": "2024-01-01T00:00:00Z",
                "updated": "2024-01-01T00:00:00Z"
              }
            }
        """.trimIndent()

        val msg = json.decodeFromString(RealtimeMessage.serializer(), raw)

        assertEquals("create", msg.action)
        assertNotNull(msg.record)
    }

    // -----------------------------------------------------------------------
    // Unit test: RealtimeEvent construction
    // -----------------------------------------------------------------------
    @Test
    fun `RealtimeEvent holds correct topic and message`() {
        val record = buildJsonObject { put("id", "abc") }
        val msg = RealtimeMessage(action = "delete", record = record)
        val event = RealtimeEvent(topic = "user_notifications/abc", message = msg)

        assertEquals("user_notifications/abc", event.topic)
        assertEquals("delete", event.message.action)
    }
}
