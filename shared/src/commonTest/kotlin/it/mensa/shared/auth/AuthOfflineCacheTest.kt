package it.mensa.shared.auth

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.mensa.shared.api.AuthRefresherHolder
import it.mensa.shared.api.endpoints.AuthApi
import it.mensa.shared.api.installMensaInterceptors
import it.mensa.shared.api.mensaDefaults
import it.mensa.shared.auth.oidc.OidcDiscovery
import it.mensa.shared.auth.oidc.OidcDiscoveryCache
import it.mensa.shared.auth.oidc.OidcSession
import it.mensa.shared.auth.oidc.TokenRefresher
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.testing.newTestDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * La cache locale scade dopo un giorno senza il server ([OfflineCache]) e un
 * account sospeso perde i dati e resta sospeso anche offline.
 *
 * Il caso da cui nasce: un socio sospeso che tiene l'app in modalita' aereo
 * per continuare a leggere il registro soci dalla cache.
 */
class AuthOfflineCacheTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val issuer = "https://auth.example.org"
    private val tokenEndpoint = "$issuer/oauth/v2/token"
    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val user = """{"id":"u1","email":"socio@example.org"}"""

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
    private lateinit var db: MensaDatabase
    private var wipes = 0

    @AfterTest
    fun resetGlobals() {
        AuthHolder.session = null
        AuthRefresherHolder.refresher = null
        AccountStatus.clear()
    }

    private fun session(accessToken: String) = OidcSession(
        accessToken = accessToken,
        refreshToken = "refresh-$accessToken",
        idToken = null,
        expiresAtEpochSeconds = Clock.System.now().epochSeconds + 3600,
        issuer = issuer,
        clientId = "app",
        tokenEndpoint = tokenEndpoint,
    )

    /** Un access_token con i due claim che `buildSession` legge: {"alg":"none"} . {"iss":issuer,"client_id":"app"}. */
    private fun jwt(): String =
        "eyJhbGciOiJub25lIn0.eyJpc3MiOiJodHRwczovL2F1dGguZXhhbXBsZS5vcmciLCJjbGllbnRfaWQiOiJhcHAifQ.sig"

    private fun MockRequestHandleScope.ok(body: String) = respond(body, HttpStatusCode.OK, jsonHeaders)
    private fun HttpRequestData.isLogin() = url.encodedPath == "/api/cs/auth-with-zitadel"
    private fun loginOk() =
        """{"access_token":"${jwt()}","refresh_token":"r2","expires_in":3600,"record":$user}"""

    /** Nessuna rete: ogni richiesta fallisce come in modalita' aereo. */
    private val offline: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData =
        { throw RuntimeException("modalita' aereo") }

    private suspend fun repo(route: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData): AuthRepository {
        val client = HttpClient(MockEngine { req -> route(req) }) { mensaDefaults() }
            .apply { installMensaInterceptors() }
        if (!::db.isInitialized) db = newTestDatabase()
        val refresher = TokenRefresher(client, tokens, json).also { AuthRefresherHolder.refresher = it }
        return AuthRepository(
            api = AuthApi(client),
            tokenStore = tokens,
            db = db,
            json = json,
            discovery = OidcDiscoveryCache(client).apply {
                seed(issuer, OidcDiscovery(issuer = issuer, token_endpoint = tokenEndpoint))
            },
            tokenRefresher = refresher,
            credentials = credentials,
            // Come wipeAllUserData sulle tabelle che contano qui.
            wipeUserData = {
                wipes++
                db.keyValueQueries.deleteAll()
                db.regSociQueries.deleteAll()
            },
        )
    }

    /** Un socio in cache confermato dal server a [syncedAt], con la sessione su disco. */
    private suspend fun cachedMember(syncedAt: Instant?) {
        tokens.token = json.encodeToString(OidcSession.serializer(), session("old"))
        db.keyValueQueries.insertOrReplace(key = "auth.current_user", value_ = user)
        if (syncedAt != null) {
            db.keyValueQueries.insertOrReplace(
                key = "auth.current_user.synced_at",
                value_ = syncedAt.toEpochMilliseconds().toString(),
            )
        }
    }

    private suspend fun cachedUserJson(): String? =
        db.keyValueQueries.selectById("auth.current_user").awaitAsOneOrNull()?.value_

    private suspend fun suspendedFlag(): Boolean =
        db.keyValueQueries.selectById("auth.account_suspended").awaitAsOneOrNull() != null

    /** Aspetta in tempo reale una reazione che gira su Dispatchers.Default. */
    private suspend fun eventually(condition: suspend () -> Boolean) = withContext(Dispatchers.Default) {
        withTimeout(5_000) { while (!condition()) delay(10) }
    }

    @Test
    fun aFreshCacheOpensOfflineAsBefore() = runTest {
        repo(offline)
        cachedMember(syncedAt = Clock.System.now() - 2.hours)
        val auth = repo(offline)

        auth.init()

        assertIs<AuthState.Authenticated>(auth.authState.value)
        assertEquals("u1", auth.currentUser.value?.id)
        assertEquals(0, wipes)
        assertNotNull(cachedUserJson())
    }

    @Test
    fun aCacheOlderThanADayIsWipedOfflineAndGoesToLogin() = runTest {
        repo(offline)
        cachedMember(syncedAt = Clock.System.now() - 30.hours)
        credentials.stored = StoredCredentials("socio@example.org", "pw")
        val auth = repo(offline)

        auth.init()

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertNull(auth.currentUser.value)
        assertTrue(wipes > 0)
        assertNull(cachedUserJson())
        // Le credenziali restano: il primo avvio con la rete rientra da solo.
        assertNotNull(credentials.stored)
    }

    @Test
    fun aCacheWithoutConfirmationTimeIsTreatedAsStale() = runTest {
        repo(offline)
        cachedMember(syncedAt = null)
        val auth = repo(offline)

        auth.init()

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertTrue(wipes > 0)
    }

    @Test
    fun aClockMovedBackDoesNotKeepTheCacheAlive() = runTest {
        repo(offline)
        cachedMember(syncedAt = Clock.System.now() + 3.hours)
        val auth = repo(offline)

        auth.init()

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertTrue(wipes > 0)
    }

    /** Online la cache vecchia si butta lo stesso, ma si rientra subito. */
    @Test
    fun aStaleCacheOnlineIsWipedAndTheMemberLogsBackIn() = runTest {
        repo(offline)
        cachedMember(syncedAt = Clock.System.now() - 30.hours)
        credentials.stored = StoredCredentials("socio@example.org", "pw")
        val auth = repo { req -> if (req.isLogin()) ok(loginOk()) else ok(user) }

        auth.init()

        assertIs<AuthState.Authenticated>(auth.authState.value)
        assertEquals("u1", auth.currentUser.value?.id)
        assertTrue(wipes > 0)
        assertNotNull(cachedUserJson())
    }

    /** L'app rimasta aperta: la scadenza arriva al ritorno in primo piano. */
    @Test
    fun aForegroundRefreshOfflineExpiresAStaleCache() = runTest {
        repo(offline)
        cachedMember(syncedAt = Clock.System.now() - 2.hours)
        val auth = repo(offline)
        auth.init()
        assertIs<AuthState.Authenticated>(auth.authState.value)

        // Passa piu' di un giorno senza rete.
        db.keyValueQueries.insertOrReplace(
            key = "auth.current_user.synced_at",
            value_ = (Clock.System.now() - 25.hours).toEpochMilliseconds().toString(),
        )
        assertNull(auth.refreshCurrentUser())

        assertEquals(AuthState.Anonymous, auth.authState.value)
        assertTrue(wipes > 0)
    }

    @Test
    fun aForegroundRefreshOfflineKeepsAFreshCache() = runTest {
        repo(offline)
        cachedMember(syncedAt = Clock.System.now() - 2.hours)
        val auth = repo(offline)
        auth.init()

        assertNull(auth.refreshCurrentUser())

        assertIs<AuthState.Authenticated>(auth.authState.value)
        assertEquals(0, wipes)
    }

    /** `account_blocked`: via i dati, e resta scritto. */
    @Test
    fun aSuspensionWipesTheDataAndIsRemembered() = runTest {
        repo(offline)
        cachedMember(syncedAt = Clock.System.now() - 1.hours)
        val auth = repo(offline)
        auth.init()

        AccountStatus.markSuspended()

        eventually { suspendedFlag() }
        assertTrue(wipes > 0)
        assertNull(cachedUserJson())
    }

    /** Il riavvio in modalita' aereo di un account sospeso resta sulla sospensione. */
    @Test
    fun aSuspendedAccountStaysSuspendedOnAnOfflineRestart() = runTest {
        repo(offline)
        tokens.token = json.encodeToString(OidcSession.serializer(), session("old"))
        db.keyValueQueries.insertOrReplace(key = "auth.account_suspended", value_ = "1")
        val auth = repo(offline)

        auth.init()

        assertTrue(AccountStatus.suspended.value)
        assertIs<AuthState.Authenticated>(auth.authState.value)
        assertNull(auth.currentUser.value)
        assertTrue(suspendedFlag())
    }

    /** Riattivato dal server: il segno di sospensione se ne va. */
    @Test
    fun aRestoredAccountForgetsTheSuspension() = runTest {
        repo(offline)
        val auth = repo(offline)
        AccountStatus.markSuspended()
        eventually { suspendedFlag() }

        AccountStatus.clear()

        eventually { !suspendedFlag() }
        assertNotNull(auth)
    }
}
