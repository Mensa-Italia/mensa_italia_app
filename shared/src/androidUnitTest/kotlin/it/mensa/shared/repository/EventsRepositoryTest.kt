package it.mensa.shared.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.endpoints.EventsApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class EventsRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private fun createInMemoryDb(): MensaDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MensaDatabase.Schema.create(driver)
        return MensaDatabase(driver)
    }

    private fun buildMockClient(responseBody: String): HttpClient {
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
    fun refreshAndObserveAllReturnsTwoItems() = runTest {
        val mockResponse = """
            {
              "page": 1, "perPage": 200, "totalItems": 2, "totalPages": 1,
              "items": [
                { "id": "e1", "name": "Evento 1", "description": "Desc 1",
                  "when_start": "2024-06-01T10:00:00Z", "when_end": "2024-06-02T18:00:00Z",
                  "owner": "u1", "is_national": true, "is_spot": false,
                  "booking_link": "", "info_link": "" },
                { "id": "e2", "name": "Evento 2", "description": "Desc 2",
                  "when_start": "2024-07-01T10:00:00Z", "when_end": "2024-07-01T18:00:00Z",
                  "owner": "u2", "is_national": false, "is_spot": true,
                  "booking_link": "https://book.me", "info_link": "" }
              ]
            }
        """.trimIndent()

        val httpClient = buildMockClient(mockResponse)
        val pb = PocketBaseClient(httpClient)
        val api = EventsApi(pb)
        val db = createInMemoryDb()
        val repo = EventsRepository(api, db, json)

        repo.refresh()

        val items = repo.observeAll().first()
        assertEquals(2, items.size)
        // DB selects ORDER BY whenStart DESC: e2 (July 2024) before e1 (June 2024)
        assertEquals("e2", items[0].id)
        assertEquals(true, items[0].isSpot)
        assertEquals("e1", items[1].id)
        assertEquals("Evento 1", items[1].name)
        assertEquals(true, items[1].isNational)
    }

    @Test
    fun getByIdReturnsCorrectItem() = runTest {
        val mockResponse = """
            {
              "page": 1, "perPage": 200, "totalItems": 1, "totalPages": 1,
              "items": [
                { "id": "single1", "name": "Single Event", "description": "Only one",
                  "when_start": "2024-08-01T10:00:00Z", "when_end": "2024-08-01T18:00:00Z",
                  "owner": "u3", "is_national": false, "is_spot": false,
                  "booking_link": "", "info_link": "" }
              ]
            }
        """.trimIndent()

        val httpClient = buildMockClient(mockResponse)
        val pb = PocketBaseClient(httpClient)
        val api = EventsApi(pb)
        val db = createInMemoryDb()
        val repo = EventsRepository(api, db, json)

        repo.refresh()

        val found = repo.getById("single1")
        assertEquals("single1", found?.id)
        assertEquals("Single Event", found?.name)

        val notFound = repo.getById("nonexistent")
        assertEquals(null, notFound)
    }
}
