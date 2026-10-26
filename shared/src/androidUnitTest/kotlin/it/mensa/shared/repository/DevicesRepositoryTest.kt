package it.mensa.shared.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.endpoints.DevicesApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Un telefono, due account: chi esce deve togliere la sua riga, e chi entra
 * deve togliere quelle rimaste degli altri. Altrimenti il server manda ogni
 * notifica una volta per account, sullo stesso token.
 */
class DevicesRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

    private val token = "fcm-token-of-this-phone"

    /** Cosa ha chiesto l'app al server: metodo, path, query e body. */
    private data class Call(val method: HttpMethod, val path: String, val query: String, val body: String)

    private suspend fun createInMemoryDb(): MensaDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MensaDatabase.Schema.create(driver).await()
        return MensaDatabase(driver)
    }

    private fun row(id: String, user: String) =
        """{ "id": "$id", "user": "$user", "firebase_id": "$token", "device_name": "iPhone" }"""

    private fun list(vararg rows: String) =
        """{ "page": 1, "perPage": 50, "totalItems": ${rows.size}, "totalPages": 1, "items": [${rows.joinToString()}] }"""

    private fun repo(
        db: MensaDatabase,
        calls: MutableList<Call>,
        handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = DevicesRepository(
        DevicesApi(
            PocketBaseClient(
                HttpClient(MockEngine { req ->
                    calls += Call(
                        req.method,
                        req.url.encodedPath,
                        req.url.parameters.names().filter { it == "firebase_id" }.joinToString(),
                        (req.body as? TextContent)?.text.orEmpty(),
                    )
                    handler(req)
                }) {
                    install(ContentNegotiation) { json(json) }
                },
            ),
        ),
        db,
    )

    private fun MockRequestHandleScope.ok(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))

    @Test
    fun loginDeletesRowsLeftByOtherAccountsOnThisPhone() = runTest {
        val calls = mutableListOf<Call>()
        val repo = repo(createInMemoryDb(), calls) { req ->
            when {
                req.method == HttpMethod.Get && req.url.parameters.contains("firebase_id") ->
                    ok(list(row("old", "montanari"), row("mine", "sipione")))
                req.method == HttpMethod.Get -> ok(list(row("mine", "sipione")))
                else -> ok("{}")
            }
        }

        val device = repo.ensureRegistered("sipione", token, "iPhone")

        assertEquals("mine", device?.id)
        val deletes = calls.filter { it.method == HttpMethod.Delete }
        assertEquals(listOf("/api/collections/users_devices/records/old"), deletes.map { it.path })
        // La delete rule, per una riga non propria, vuole id e token nel body.
        val body = deletes.single().body
        assertTrue("\"id\":\"old\"" in body && "\"firebase_id\":\"$token\"" in body, body)
        // E la ricerca deve passare il token come query, o la list rule non
        // mostra le righe degli altri.
        assertTrue(calls.any { it.method == HttpMethod.Get && it.query == "firebase_id" })
    }

    @Test
    fun logoutDeletesOnlyThisUsersRowForThisPhone() = runTest {
        val db = createInMemoryDb()
        val calls = mutableListOf<Call>()
        val repo = repo(db, calls) { req ->
            when (req.method) {
                HttpMethod.Get -> ok(list(row("mine", "sipione")))
                else -> ok("{}")
            }
        }
        repo.ensureRegistered("sipione", token, "iPhone")
        calls.clear()

        repo.unregisterCurrent("sipione")

        assertEquals(
            listOf("/api/collections/users_devices/records/mine"),
            calls.filter { it.method == HttpMethod.Delete }.map { it.path },
        )
    }

    @Test
    fun logoutWithoutAKnownTokenTouchesNothing() = runTest {
        val calls = mutableListOf<Call>()
        val repo = repo(createInMemoryDb(), calls) { ok("{}") }

        repo.unregisterCurrent("sipione")

        assertTrue(calls.isEmpty())
    }
}
