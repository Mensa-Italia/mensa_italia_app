package it.mensa.shared.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.mensaDefaults
import it.mensa.shared.api.endpoints.DocumentsApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DocumentsRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private suspend fun createInMemoryDb(): MensaDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MensaDatabase.Schema.create(driver).await()
        return MensaDatabase(driver)
    }

    private fun repo(db: MensaDatabase, status: HttpStatusCode, body: String) = DocumentsRepository(
        DocumentsApi(
            PocketBaseClient(
                HttpClient(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) }) {
                    mensaDefaults()
                },
            ),
        ),
        db,
    )

    /**
     * La notifica di un documento appena pubblicato: la lista non l'ha mai
     * scaricato, quindi in database non c'e'. Prima tornava null ("Documento
     * non disponibile"); adesso arriva dal server e resta in database.
     */
    @Test
    fun documentNotYetListedComesFromTheServer() = runTest {
        val db = createInMemoryDb()
        val repo = repo(
            db,
            HttpStatusCode.OK,
            """{ "id": "doc1", "name": "Verbale", "file": "verbale_abc123.pdf", "created": "2026-10-24 10:00:00.000Z" }""",
        )
        assertEquals("verbale_abc123.pdf", repo.getById("doc1")?.file)
        assertEquals("doc1", repo.firstSnapshot().single().id)
    }

    @Test
    fun missingOnTheServerToo() = runTest {
        val repo = repo(createInMemoryDb(), HttpStatusCode.NotFound, """{"status":404,"message":"The requested resource wasn't found.","data":{}}""")
        assertNull(repo.getById("gone"))
    }
}
