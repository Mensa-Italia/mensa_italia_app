package it.mensa.shared.auth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.mensa.shared.api.endpoints.AuthApi
import it.mensa.shared.api.installMensaInterceptors
import it.mensa.shared.api.mensaDefaults
import it.mensa.shared.api.endpoints.InvalidCredentialsException
import it.mensa.shared.api.endpoints.LoginUnavailableException
import it.mensa.shared.auth.oidc.OidcDiscovery
import it.mensa.shared.auth.oidc.OidcDiscoveryCache
import it.mensa.shared.auth.oidc.TokenRefresher
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/**
 * Un login fallito deve arrivare a chi chiama come eccezione: e' l'unica forma
 * che attraversa il bridge verso Swift. Con un `Result` la schermata di login
 * su iOS non mostrava mai l'errore (password sbagliata, nessun messaggio).
 */
class AuthRepositoryLoginTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val issuer = "https://auth.example.org"

    private class MemoryTokenStore : ITokenStore {
        var token: String? = null
        override suspend fun save(token: String) { this.token = token }
        override suspend fun read(): String? = token
        override suspend fun clear() { token = null }
    }

    private class MemoryCredentialStore : ICredentialStore {
        var stored: StoredCredentials? = null
        override suspend fun save(email: String, password: String) { stored = StoredCredentials(email, password) }
        override suspend fun read(): StoredCredentials? = stored
        override suspend fun clear() { stored = null }
    }

    private val tokens = MemoryTokenStore()
    private val credentials = MemoryCredentialStore()

    @AfterTest
    fun resetGlobals() {
        AuthHolder.session = null
        AccountStatus.clear()
    }

    /** Un access_token con i due claim che `buildSession` legge. */
    private fun accessToken(): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = enc.encodeToString("""{"iss":"$issuer","client_id":"app"}""".toByteArray())
        return "$header.$payload.sig"
    }

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun loginOk() =
        """{"access_token":"${accessToken()}","refresh_token":"r","expires_in":3600,"record":{"id":"u1","email":"socio@example.org"}}"""

    /** [status]/[body] per il login; la discovery dell'issuer, se non e' in cache, da [discovery]. */
    private suspend fun repo(
        status: HttpStatusCode,
        body: String,
        discovery: (() -> Pair<HttpStatusCode, String>)? = null,
    ): AuthRepository {
        val client = HttpClient(MockEngine { req ->
            if (req.url.encodedPath.endsWith("/.well-known/openid-configuration")) {
                val (s, b) = checkNotNull(discovery) { "discovery inattesa" }()
                respond(b, s, jsonHeaders)
            } else {
                respond(body, status, jsonHeaders)
            }
        }) { mensaDefaults() }.apply { installMensaInterceptors() }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MensaDatabase.Schema.create(driver).await()
        val cache = OidcDiscoveryCache(client).apply {
            if (discovery == null) seed(issuer, OidcDiscovery(issuer = issuer, token_endpoint = "$issuer/oauth/v2/token"))
        }
        return AuthRepository(
            api = AuthApi(client),
            tokenStore = tokens,
            db = MensaDatabase(driver),
            json = json,
            discovery = cache,
            tokenRefresher = TokenRefresher(client, tokens, json),
            credentials = credentials,
        )
    }

    @Test
    fun wrongPasswordThrowsAndStaysOut() = runTest {
        val auth = repo(HttpStatusCode.Unauthorized, """{"status":401,"message":"Invalid credentials.","data":{}}""")

        assertFailsWith<InvalidCredentialsException> { auth.login("socio@example.org", "sbagliata") }

        assertFalse(auth.authState.value is AuthState.Authenticated)
        assertNull(credentials.stored)
        assertNull(tokens.token)
    }

    @Test
    fun serverDownThrowsAReadableError() = runTest {
        val auth = repo(HttpStatusCode.ServiceUnavailable, """{"status":503,"message":"Unable to connect to area32","data":{}}""")

        val e = assertFailsWith<LoginUnavailableException> { auth.login("socio@example.org", "giusta") }

        assertEquals(503, e.status)
        assertEquals("Accesso non disponibile in questo momento (HTTP 503). Riprova fra poco.", e.message)
        assertFalse(auth.authState.value is AuthState.Authenticated)
    }

    /**
     * Senza record (il server lo omette se l'area soci non risponde al primo
     * accesso) la risposta e' inservibile: un messaggio da mostrare e niente
     * sessione mezza adottata.
     */
    @Test
    fun responseWithoutRecordLeavesNoSession() = runTest {
        val auth = repo(HttpStatusCode.OK, """{"access_token":"${accessToken()}","refresh_token":"r","expires_in":3600}""")

        val e = assertFailsWith<LoginUnavailableException> { auth.login("socio@example.org", "giusta") }

        assertNull(e.status)
        assertEquals("Accesso non disponibile in questo momento. Riprova fra poco.", e.message)
        assertEquals("/auth-with-zitadel response missing user record", e.detail)
        assertFalse(auth.authState.value is AuthState.Authenticated)
        assertNull(tokens.token)
        assertNull(AuthHolder.session)
    }

    /** Un errore momentaneo della discovery non resta in cache fino al riavvio. */
    @Test
    fun failedDiscoveryIsRetriedOnTheNextLogin() = runTest {
        val answers = ArrayDeque(
            listOf(
                HttpStatusCode.ServiceUnavailable to """{"error":"unavailable"}""",
                HttpStatusCode.OK to """{"issuer":"$issuer","token_endpoint":"$issuer/oauth/v2/token"}""",
            ),
        )
        val auth = repo(HttpStatusCode.OK, loginOk(), discovery = { answers.removeFirst() })

        val e = assertFailsWith<LoginUnavailableException> { auth.login("socio@example.org", "giusta") }
        assertEquals(503, e.status)
        assertFalse(auth.authState.value is AuthState.Authenticated)

        assertEquals("u1", auth.login("socio@example.org", "giusta").id)
        assertIs<AuthState.Authenticated>(auth.authState.value)
    }

    @Test
    fun successReturnsTheUserAndSavesCredentials() = runTest {
        val auth = repo(HttpStatusCode.OK, loginOk())

        val user = auth.login("socio@example.org", "giusta")

        assertEquals("u1", user.id)
        assertIs<AuthState.Authenticated>(auth.authState.value)
        assertEquals(StoredCredentials("socio@example.org", "giusta"), credentials.stored)
    }
}
