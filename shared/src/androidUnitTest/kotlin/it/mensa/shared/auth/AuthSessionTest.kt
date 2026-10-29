package it.mensa.shared.auth

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.mensa.shared.api.AuthRefresherHolder
import it.mensa.shared.api.endpoints.AuthApi
import it.mensa.shared.api.endpoints.PasskeyApi
import it.mensa.shared.api.installMensaInterceptors
import it.mensa.shared.api.mensaDefaults
import it.mensa.shared.auth.oidc.OidcDiscovery
import it.mensa.shared.auth.oidc.OidcDiscoveryCache
import it.mensa.shared.auth.oidc.OidcSession
import it.mensa.shared.auth.oidc.TokenRefresher
import it.mensa.shared.auth.passkey.PasskeyLoginStatus
import it.mensa.shared.auth.passkey.PasskeyRepository
import it.mensa.shared.auth.passkey.PasskeyStartStatus
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * La sessione quando il server dice di no, sul client di produzione
 * ([mensaDefaults] e i suoi interceptor).
 *
 * Con `expectSuccess` spento nessuno di questi casi arrivava dove doveva: un
 * refresh_token revocato diventava una sessione con il token vuoto, un `/me`
 * rifiutato o in errore un socio senza dati pubblicato e messo in cache.
 */
class AuthSessionTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val issuer = "https://auth.example.org"
    private val tokenEndpoint = "$issuer/oauth/v2/token"
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

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
    private val requests = mutableListOf<HttpRequestData>()
    private lateinit var refresher: TokenRefresher
    private lateinit var client: HttpClient

    @AfterTest
    fun resetGlobals() {
        AuthHolder.session = null
        AuthRefresherHolder.refresher = null
        AccountStatus.clear()
    }

    private fun session(accessToken: String, expiresInSeconds: Long) = OidcSession(
        accessToken = accessToken,
        refreshToken = "refresh-$accessToken",
        idToken = null,
        expiresAtEpochSeconds = Clock.System.now().epochSeconds + expiresInSeconds,
        issuer = issuer,
        clientId = "app",
        tokenEndpoint = tokenEndpoint,
    )

    private fun storeSession(s: OidcSession) {
        tokens.token = json.encodeToString(OidcSession.serializer(), s)
    }

    private fun MockRequestHandleScope.ok(body: String) = respond(body, HttpStatusCode.OK, jsonHeaders)
    private fun MockRequestHandleScope.status(status: HttpStatusCode) =
        respond("""{"status":${status.value},"message":"no","data":{}}""", status, jsonHeaders)

    private fun HttpRequestData.bearer() = headers[HttpHeaders.Authorization]?.removePrefix("Bearer ")
    private fun HttpRequestData.isRefresh() = url.toString() == tokenEndpoint
    private fun refreshes() = requests.count { it.isRefresh() }

    /** Un access_token con i due claim che `buildSession` legge. */
    private fun jwt(): String {
        val enc = Base64.getUrlEncoder().withoutPadding()
        val header = enc.encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = enc.encodeToString("""{"iss":"$issuer","client_id":"app"}""".toByteArray())
        return "$header.$payload.sig"
    }

    private fun HttpRequestData.isLogin() = url.encodedPath == "/api/cs/auth-with-zitadel"
    private fun loginOk() =
        """{"access_token":"${jwt()}","refresh_token":"r2","expires_in":3600,"record":$user}"""

    private fun newToken(access: String) = """{"access_token":"$access","refresh_token":"refresh-$access","expires_in":3600}"""
    private val user = """{"id":"u1","email":"socio@example.org"}"""

    private suspend fun repo(route: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): AuthRepository {
        client = HttpClient(MockEngine { req -> requests += req; route(req) }) { mensaDefaults() }
            .apply { installMensaInterceptors() }
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        MensaDatabase.Schema.create(driver).await()
        refresher = TokenRefresher(client, tokens, json).also { AuthRefresherHolder.refresher = it }
        return AuthRepository(
            api = AuthApi(client),
            tokenStore = tokens,
            db = MensaDatabase(driver),
            json = json,
            discovery = OidcDiscoveryCache(client).apply {
                seed(issuer, OidcDiscovery(issuer = issuer, token_endpoint = tokenEndpoint))
            },
            tokenRefresher = refresher,
            credentials = credentials,
        )
    }

    /** Il caso dell'avvio con il refresh_token revocato: si torna al login. */
    @Test
    fun aRevokedRefreshTokenAtStartupEndsTheSession() = runTest {
        storeSession(session("old", expiresInSeconds = -10))
        val auth = repo { req ->
            if (req.isRefresh()) respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest, jsonHeaders)
            else error("unexpected ${req.url}")
        }

        auth.init()

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertNull(auth.currentUser.value)
        assertNull(tokens.token)
        assertNull(AuthHolder.session)
    }

    /** L'issuer giu' non e' un refresh_token morto: la sessione resta. */
    @Test
    fun anAuthServerOutageKeepsTheSession() = runTest {
        storeSession(session("old", expiresInSeconds = -10))
        val auth = repo { req ->
            if (req.isRefresh()) status(HttpStatusCode.ServiceUnavailable) else ok(user)
        }

        auth.init()

        assertEquals(AuthState.Authenticated("old"), auth.authState.value)
        assertEquals("u1", auth.currentUser.value?.id)
        assertNotNull(tokens.token)
    }

    /**
     * Un 401 prima della scadenza scritta nel token (orologio indietro, sessione
     * chiusa sul server): il retry rinnova davvero, invece di ripartire con lo
     * stesso token. Su una route qualunque, non su `/me`, che ha un suo secondo
     * tentativo e lo nasconderebbe.
     */
    @Test
    fun a401BeforeExpiryRefreshesAndRetries() = runTest {
        repo { req ->
            when {
                req.isRefresh() -> ok(newToken("new"))
                req.bearer() == "new" -> ok("""{"items":[]}""")
                else -> status(HttpStatusCode.Unauthorized)
            }
        }
        AuthHolder.session = session("old", expiresInSeconds = 3600)

        AuthApi(client).probeAccess()

        assertEquals("new", AuthHolder.session?.accessToken)
        assertEquals(1, refreshes())
    }

    /** `/me` che rifiuta anche il token appena rinnovato: l'identita' non vale piu'. */
    @Test
    fun meRejectedEvenAfterARefreshEndsTheSession() = runTest {
        storeSession(session("old", expiresInSeconds = 3600))
        var minted = 0
        val auth = repo { req ->
            if (req.isRefresh()) ok(newToken("new${++minted}")) else status(HttpStatusCode.Unauthorized)
        }

        auth.init()

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertNull(auth.currentUser.value)
        assertNull(tokens.token)
    }

    /** `/me` in errore non pubblica un socio vuoto: resta quello che c'era. */
    @Test
    fun meServerErrorPublishesNoEmptyUser() = runTest {
        storeSession(session("old", expiresInSeconds = 3600))
        val auth = repo { status(HttpStatusCode.InternalServerError) }

        auth.init()

        assertIs<AuthState.Authenticated>(auth.authState.value)
        assertNull(auth.currentUser.value)
        assertNotNull(tokens.token)
        assertEquals(0, refreshes())
    }

    /**
     * Il refresh_token rifiutato a meta' sessione (dal retry di un 401): si esce
     * del tutto, come per un `/me` rifiutato, e chi chiama riceve l'errore.
     */
    @Test
    fun aRevokedRefreshTokenMidSessionLogsOut() = runTest {
        storeSession(session("old", expiresInSeconds = 3600))
        var revoked = false
        val auth = repo { req ->
            when {
                req.isRefresh() -> respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest, jsonHeaders)
                revoked -> status(HttpStatusCode.Unauthorized)
                else -> ok(user)
            }
        }
        auth.init()
        assertEquals("u1", auth.currentUser.value?.id)

        revoked = true
        assertFailsWith<ClientRequestException> { AuthApi(client).probeAccess() }

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertNull(auth.currentUser.value)
        assertNull(tokens.token)
        assertNull(AuthHolder.session)
    }

    /**
     * Il refresh_token rifiutato non dice che la password sia sbagliata: con le
     * credenziali salvate si rifa' il login, e la richiesta riesce con la
     * sessione nuova.
     */
    @Test
    fun aLostSessionIsRecoveredWithTheStoredPassword() = runTest {
        credentials.stored = StoredCredentials("socio@example.org", "giusta")
        repo { req ->
            when {
                req.isRefresh() -> respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest, jsonHeaders)
                req.isLogin() -> ok(loginOk())
                req.bearer() == jwt() -> ok("""{"items":[]}""")
                else -> status(HttpStatusCode.Unauthorized)
            }
        }.also { AuthHolder.session = session("old", expiresInSeconds = 3600) }

        AuthApi(client).probeAccess()

        assertEquals(jwt(), AuthHolder.session?.accessToken)
        assertNotNull(credentials.stored)
        assertNotNull(tokens.token)
    }

    /** La password rifiutata anche lei: si esce del tutto, credenziali comprese. */
    @Test
    fun aLostSessionWithAWrongPasswordLogsOutCompletely() = runTest {
        credentials.stored = StoredCredentials("socio@example.org", "cambiata")
        val auth = repo { req ->
            when {
                req.isRefresh() -> respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest, jsonHeaders)
                req.isLogin() -> status(HttpStatusCode.Unauthorized)
                else -> status(HttpStatusCode.Unauthorized)
            }
        }
        AuthHolder.session = session("old", expiresInSeconds = 3600)

        assertFailsWith<ClientRequestException> { AuthApi(client).probeAccess() }

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertNull(credentials.stored)
        assertNull(AuthHolder.session)
    }

    /** Il login che non risponde non dice niente sulla password: si esce, ma resta. */
    @Test
    fun aLostSessionWithTheLoginDownKeepsThePassword() = runTest {
        credentials.stored = StoredCredentials("socio@example.org", "giusta")
        val auth = repo { req ->
            when {
                req.isRefresh() -> respond("""{"error":"invalid_grant"}""", HttpStatusCode.BadRequest, jsonHeaders)
                req.isLogin() -> status(HttpStatusCode.ServiceUnavailable)
                else -> status(HttpStatusCode.Unauthorized)
            }
        }
        AuthHolder.session = session("old", expiresInSeconds = 3600)

        assertFailsWith<ClientRequestException> { AuthApi(client).probeAccess() }

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertNotNull(credentials.stored)
        assertNull(AuthHolder.session)
    }

    /**
     * Il 401 con l'issuer giu' non e' una sessione morta: chi chiama riceve
     * l'errore dell'issuer, non il 401 che la UI legge come "sessione non valida".
     */
    @Test
    fun a401WithTheIssuerDownReportsTheIssuer() = runTest {
        repo { req -> if (req.isRefresh()) status(HttpStatusCode.ServiceUnavailable) else status(HttpStatusCode.Unauthorized) }
        AuthHolder.session = session("old", expiresInSeconds = 3600)

        val e = assertFailsWith<ServerResponseException> { AuthApi(client).probeAccess() }

        assertEquals(HttpStatusCode.ServiceUnavailable, e.response.status)
        assertEquals("old", AuthHolder.session?.accessToken)
    }

    /** `/me` rifiutato con l'issuer giu': non e' un'identita' morta, si resta dentro. */
    @Test
    fun meRejectedWithTheIssuerDownStaysSignedIn() = runTest {
        storeSession(session("old", expiresInSeconds = 3600))
        val auth = repo { req ->
            if (req.isRefresh()) status(HttpStatusCode.ServiceUnavailable) else status(HttpStatusCode.Unauthorized)
        }

        auth.init()

        assertEquals(AuthState.Authenticated("old"), auth.authState.value)
        assertNotNull(tokens.token)
        assertEquals("old", AuthHolder.session?.accessToken)
    }

    /** Un 200 senza access_token non si salva come sessione. */
    @Test
    fun aRefreshWithoutAccessTokenIsNotAdopted() = runTest {
        val before = session("old", expiresInSeconds = -10)
        repo { ok("""{"token_type":"Bearer"}""") }
        AuthHolder.session = before

        assertFailsWith<IllegalStateException> { refresher.refresh() }

        assertEquals(before, AuthHolder.session)
        assertNull(tokens.token)
    }

    /** Due 401 in fila sullo stesso token: chi arriva secondo usa il rinnovo del primo. */
    @Test
    fun aTokenAlreadyReplacedIsNotRefreshedAgain() = runTest {
        repo { req -> if (req.isRefresh()) ok(newToken("new")) else ok(user) }
        AuthHolder.session = session("old", expiresInSeconds = 3600)

        assertEquals("new", refresher.refreshRejected("old").accessToken)
        assertEquals("new", refresher.refreshRejected("old").accessToken)
        assertEquals(1, refreshes())
    }

    /**
     * Gli esiti passkey che dipendono dallo stato: challenge scaduta, rifiuto, e
     * una rimozione fallita che prima risultava riuscita.
     */
    @Test
    fun passkeyStatusesReachTheirOutcome() = runTest {
        var finishStatus = HttpStatusCode.Gone
        val auth = repo { req ->
            when (req.url.encodedPath) {
                "/api/cs/auth-with-passkey/begin" -> status(HttpStatusCode.Conflict)
                "/api/cs/auth-with-passkey/finish" -> status(finishStatus)
                else -> status(HttpStatusCode.InternalServerError)
            }
        }
        val passkeys = PasskeyRepository(PasskeyApi(client), auth, json)

        assertEquals(PasskeyStartStatus.UNAVAILABLE, passkeys.beginLogin("socio@example.org").status)
        assertEquals(PasskeyLoginStatus.CHALLENGE_EXPIRED, passkeys.finishLogin("l1", "{}").status)
        finishStatus = HttpStatusCode.Unauthorized
        assertEquals(PasskeyLoginStatus.REJECTED, passkeys.finishLogin("l1", "{}").status)
        assertFalse(passkeys.remove("p1"))
        assertFalse(auth.authState.value is AuthState.Authenticated)
    }
}
