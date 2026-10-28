package it.mensa.shared.integrity

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.api.ApiConfig
import it.mensa.shared.auth.AccountStatus
import it.mensa.shared.net.ServerReachability
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IntegrityGuardTest {

    @BeforeTest
    fun resetAccountStatus() = AccountStatus.clear()

    @AfterTest
    fun cleanAccountStatus() = AccountStatus.clear()

    // ── Doppioni di test ─────────────────────────────────────────────────────

    private class FakeProvider(
        override val platform: String = "android",
        override val requiresKeyEnrollment: Boolean = false,
        var supported: Boolean = true,
        var key: String? = null,
    ) : IntegrityProvider {
        var keysGenerated = 0
        val forgotten = mutableListOf<String>()

        /** Fa fallire la prossima `prove` come se la chiave fosse invalida. */
        var invalidateKeyOnNextProve = false

        override suspend fun isSupported() = supported
        override suspend fun hasKey(userId: String) = key != null

        override suspend fun attest(challenge: String) =
            KeyAttestation(keyId = "key-${++keysGenerated}", attestation = "att($challenge)")

        override suspend fun saveKey(userId: String, keyId: String) {
            key = keyId
        }

        override suspend fun forgetKey(userId: String) {
            key = null
            forgotten += userId
        }

        override suspend fun prove(userId: String, challenge: String, clientData: String): Map<String, String> {
            if (invalidateKeyOnNextProve) {
                invalidateKeyOnNextProve = false
                throw IntegrityKeyInvalidException("chiave invalida")
            }
            return if (requiresKeyEnrollment) {
                mapOf(IntegrityHeaders.KEY_ID to key!!, IntegrityHeaders.ASSERTION to "asr($clientData)")
            } else {
                mapOf(IntegrityHeaders.TOKEN to "tok($clientData)")
            }
        }
    }

    /** Un server finto: registra ogni richiesta e risponde alla sfida da solo. */
    private class FakeServer(private val route: MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData?) {
        val requests = mutableListOf<HttpRequestData>()
        var challenges = 0
        var attestStatus = HttpStatusCode.OK

        val api: List<HttpRequestData> get() = requests.filter { it.url.encodedPath != CHALLENGE && it.url.encodedPath != ATTEST }
        val challengeCalls: Int get() = requests.count { it.url.encodedPath == CHALLENGE }

        fun client(authenticated: Boolean = true): HttpClient {
            val engine = MockEngine(MockEngineConfig().apply {
                addHandler { request ->
                    requests += request
                    when (request.url.encodedPath) {
                        CHALLENGE -> respond(
                            """{"challenge":"chal-${++challenges}","expires_in":120}""",
                            HttpStatusCode.OK, jsonHeaders,
                        )
                        ATTEST -> respond("""{"ok":true}""", attestStatus, jsonHeaders)
                        else -> route(request) ?: respond("{}", HttpStatusCode.OK, jsonHeaders)
                    }
                }
            })
            return HttpClient(engine) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                defaultRequest {
                    url(ApiConfig.BASE_URL)
                    contentType(ContentType.Application.Json)
                    if (authenticated) header(HttpHeaders.Authorization, "Bearer test-token")
                }
            }
        }
    }

    private companion object {
        const val CHALLENGE = "/api/integrity/challenge"
        const val ATTEST = "/api/integrity/ios/attest"
        val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    }

    private fun MockRequestHandleScope.error(status: HttpStatusCode, code: String) =
        respond("""{"status":${status.value},"message":"msg $code","code":"$code"}""", status, jsonHeaders)

    private fun guard(
        server: FakeServer,
        provider: IntegrityProvider,
        user: () -> String? = { "user-1" },
        authenticated: Boolean = true,
    ): HttpClient = server.client(authenticated).also { IntegrityGuard(it, provider, user).install() }

    private fun HttpRequestData.hasProof() = headers.contains(IntegrityHeaders.CHALLENGE)

    // ── Flusso base ──────────────────────────────────────────────────────────

    @Test
    fun noProofNeededMeansNoExtraRequests() = runTest {
        val server = FakeServer { null }
        val client = guard(server, FakeProvider())

        assertEquals(HttpStatusCode.OK, client.get("/api/collections/events/records").status)

        assertEquals(1, server.requests.size)
        assertFalse(server.requests.single().hasProof())
    }

    @Test
    fun challengeThenRetryWithHeadersThenOk() = runTest {
        var calls = 0
        val server = FakeServer { req ->
            if (req.url.encodedPath == "/api/collections/stamp/records" && ++calls == 1) {
                error(PreconditionRequired, "integrity_required")
            } else null
        }
        val client = guard(server, FakeProvider())

        val response = client.get("/api/collections/stamp/records?page=1&perPage=50")

        assertEquals(HttpStatusCode.OK, response.status)
        // 1) la richiesta nuda, 2) la sfida, 3) il retry con la prova.
        assertEquals(
            listOf("/api/collections/stamp/records", CHALLENGE, "/api/collections/stamp/records"),
            server.requests.map { it.url.encodedPath },
        )
        val first = server.requests[0]
        val retry = server.requests[2]
        assertFalse(first.hasProof())
        assertEquals("android", retry.headers[IntegrityHeaders.PLATFORM])
        assertEquals("chal-1", retry.headers[IntegrityHeaders.CHALLENGE])
        // La query string non entra in clientData.
        assertEquals(
            "tok(chal-1|GET|/api/collections/stamp/records)",
            retry.headers[IntegrityHeaders.TOKEN],
        )
        // La richiesta ripetuta e' la stessa, query compresa.
        assertEquals("page=1&perPage=50", retry.url.encodedQuery)
    }

    @Test
    fun postBodyIsResentIdenticallyOnRetry() = runTest {
        var calls = 0
        val server = FakeServer { req ->
            if (req.url.encodedPath == "/api/collections/stamp_users/records" && ++calls == 1) {
                error(PreconditionRequired, "integrity_required")
            } else null
        }
        val client = guard(server, FakeProvider())

        val body = """{"stamp":"s1","code":"c","user":"u"}"""
        val response = client.post("/api/collections/stamp_users/records") { setBody(TextContent(body, ContentType.Application.Json)) }

        assertEquals(HttpStatusCode.OK, response.status)
        val posts = server.requests.filter { it.url.encodedPath == "/api/collections/stamp_users/records" }
        assertEquals(2, posts.size)
        assertEquals(body, posts[0].body.toByteArray().decodeToString())
        assertEquals(body, posts[1].body.toByteArray().decodeToString())
        assertEquals("tok(chal-1|POST|/api/collections/stamp_users/records)", posts[1].headers[IntegrityHeaders.TOKEN])
    }

    @Test
    fun serializedBodiesAreResentToo() = runTest {
        var calls = 0
        val server = FakeServer { req ->
            if (req.url.encodedPath == "/api/x" && ++calls == 1) error(PreconditionRequired, "integrity_required") else null
        }
        val client = guard(server, FakeProvider())

        // Corpo serializzato dal ContentNegotiation, come fa PocketBaseClient.create().
        client.post("/api/x") { setBody(mapOf("a" to "b")) }

        val posts = server.requests.filter { it.url.encodedPath == "/api/x" }
        assertEquals(2, posts.size)
        assertEquals("""{"a":"b"}""", posts[1].body.toByteArray().decodeToString())
    }

    // ── Errori senza retry ───────────────────────────────────────────────────

    @Test
    fun integrityFailedIsTypedAndNeverRetried() = runTest {
        val server = FakeServer { error(HttpStatusCode.Forbidden, "integrity_failed") }
        val client = guard(server, FakeProvider())

        assertFailsWith<IntegrityFailedException> { client.get("/api/collections/stamp/records") }

        assertEquals(1, server.requests.size)
        assertEquals(0, server.challengeCalls)
    }

    @Test
    fun integrityFailedOnTheRetryIsTypedToo() = runTest {
        val server = FakeServer { req ->
            if (req.hasProof()) error(HttpStatusCode.Forbidden, "integrity_failed")
            else error(PreconditionRequired, "integrity_required")
        }
        val client = guard(server, FakeProvider())

        assertFailsWith<IntegrityFailedException> { client.get("/api/collections/stamp/records") }

        assertEquals(2, server.api.size)
    }

    @Test
    fun accountBlockedIsTypedAndFlipsTheGlobalState() = runTest {
        val server = FakeServer { error(HttpStatusCode.Forbidden, "account_blocked") }
        val client = guard(server, FakeProvider())

        assertFalse(AccountStatus.suspended.value)
        val e = assertFailsWith<AccountBlockedException> { client.get("/api/cs/me") }

        assertEquals("msg account_blocked", e.message)
        assertTrue(AccountStatus.suspended.value)
        assertEquals(1, server.requests.size, "nessun retry")
        assertEquals(0, server.challengeCalls)
    }

    @Test
    fun accountBlockedIsRecognisedOnExemptRoutesToo() = runTest {
        val server = FakeServer { error(HttpStatusCode.Forbidden, "account_blocked") }
        val client = guard(server, FakeProvider())

        assertFailsWith<AccountBlockedException> { client.get("/api/health") }
        assertTrue(AccountStatus.suspended.value)
    }

    @Test
    fun suspensionClearsOnTheNextSuccessfulAuthenticatedResponse() = runTest {
        var blocked = true
        val server = FakeServer { if (blocked) error(HttpStatusCode.Forbidden, "account_blocked") else null }
        val client = guard(server, FakeProvider())

        assertFailsWith<AccountBlockedException> { client.get("/api/collections/events/records") }
        assertTrue(AccountStatus.suspended.value)

        blocked = false
        client.get("/api/collections/events/records")
        assertFalse(AccountStatus.suspended.value)
    }

    @Test
    fun otherForbiddenResponsesPassThroughUntouched() = runTest {
        // Un 403 normale di PocketBase (API rule) non ha `code`: non e' roba nostra.
        val server = FakeServer { _ ->
            respond("""{"status":403,"message":"nope","data":{}}""", HttpStatusCode.Forbidden, jsonHeaders)
        }
        val client = guard(server, FakeProvider())

        val response = client.get("/api/collections/events/records")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("""{"status":403,"message":"nope","data":{}}""", response.bodyAsText())
        assertFalse(AccountStatus.suspended.value)
    }

    // ── Un solo retry ────────────────────────────────────────────────────────

    @Test
    fun atMostOneRetryThenUnavailable() = runTest {
        val server = FakeServer { error(PreconditionRequired, "integrity_required") }
        val client = guard(server, FakeProvider())

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/collections/stamp/records") }

        // Originale + un solo retry.
        assertEquals(2, server.api.size)
        assertEquals(1, server.challengeCalls)
    }

    // ── Proattivo ────────────────────────────────────────────────────────────

    @Test
    fun afterTheFirst428EveryRequestCarriesAFreshProof() = runTest {
        var first = true
        val server = FakeServer { req ->
            if (!req.hasProof()) {
                // Il primo 428 arriva su una route qualunque, non solo timbri/medaglie.
                if (first) { first = false; error(PreconditionRequired, "integrity_required") }
                else error(PreconditionRequired, "integrity_required")
            } else null
        }
        val client = guard(server, FakeProvider())

        client.get("/api/collections/events/records")   // 428 -> sfida 1 -> retry
        client.get("/api/cs/me")                        // gia' con la prova: sfida 2
        client.get("/api/collections/sigs/records")     // sfida 3

        val api = server.api
        assertEquals(4, api.size, "nessun 428 in piu': la prova parte con la prima richiesta")
        assertTrue(api[2].hasProof())
        assertTrue(api[3].hasProof())
        assertEquals("chal-2", api[2].headers[IntegrityHeaders.CHALLENGE])
        assertEquals("tok(chal-2|GET|/api/cs/me)", api[2].headers[IntegrityHeaders.TOKEN])
        assertEquals("chal-3", api[3].headers[IntegrityHeaders.CHALLENGE])
    }

    @Test
    fun concurrentRequestsNeverShareAChallenge() = runTest {
        val server = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val client = guard(server, FakeProvider())
        client.get("/api/cs/me") // attiva il modo proattivo (sfida 1)

        val before = server.challenges
        List(8) { i -> async { client.get("/api/collections/events/records?page=$i") } }.awaitAll()

        val used = server.api.drop(2).mapNotNull { it.headers[IntegrityHeaders.CHALLENGE] }
        assertEquals(8, used.size)
        assertEquals(8, used.toSet().size, "ogni richiesta ha la sua sfida")
        assertEquals(before + 8, server.challenges)
    }

    @Test
    fun proactiveModeResetsOnLogoutAndUserChange() = runTest {
        var user: String? = "user-1"
        val server = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val client = guard(server, FakeProvider(), user = { user })

        client.get("/api/cs/me")
        assertTrue(server.api.last().hasProof())

        // Altro utente: si riparte da una richiesta nuda (e dal suo 428).
        user = "user-2"
        server.requests.clear()
        client.get("/api/cs/me")
        assertFalse(server.api.first().hasProof())

        // Logout (nessun utente) e rientro dello stesso utente: ancora da zero.
        user = null
        client.get("/api/health")
        user = "user-2"
        server.requests.clear()
        client.get("/api/cs/me")
        assertFalse(server.api.first().hasProof())
    }

    // ── Esenzioni e anonimi ──────────────────────────────────────────────────

    @Test
    fun exemptRoutesNeverGetAProofEvenInProactiveMode() = runTest {
        val server = FakeServer { req -> if (req.url.encodedPath == "/api/cs/me" && !req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val client = guard(server, FakeProvider())
        client.get("/api/cs/me")

        server.requests.clear()
        client.get("/api/health")
        client.get("/api/files/stamp/abc/img.png")
        client.get("/api/realtime")

        assertEquals(3, server.requests.size)
        assertTrue(server.requests.none { it.hasProof() })
        assertEquals(0, server.challengeCalls)
    }

    @Test
    fun a428OnAnExemptRouteIsNotOurBusiness() = runTest {
        val server = FakeServer { error(PreconditionRequired, "integrity_required") }
        val client = guard(server, FakeProvider())

        val response = client.get("/api/health")

        assertEquals(PreconditionRequired, response.status)
        assertEquals(1, server.requests.size)
    }

    @Test
    fun anonymousRequestsAreLeftAlone() = runTest {
        var asked = false
        val server = FakeServer { req ->
            if (!req.hasProof()) { asked = true; error(PreconditionRequired, "integrity_required") } else null
        }
        val client = guard(server, FakeProvider(), authenticated = false)

        val response = client.get("/api/collections/sigs/records")

        assertTrue(asked)
        assertEquals(PreconditionRequired, response.status)
        assertEquals(0, server.challengeCalls)
    }

    @Test
    fun otherHostsAreNotTouched() = runTest {
        val server = FakeServer { error(HttpStatusCode.Forbidden, "account_blocked") }
        val client = guard(server, FakeProvider())

        val response = client.get("https://auth.mensa.it/oauth/v2/token")

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertFalse(AccountStatus.suspended.value)
    }

    @Test
    fun thirdPartyHostGetsNoProofChallengeOrRetryEvenWithOurTokenAttached() = runTest {
        // Caso peggiore: modo proattivo attivo e un Authorization finito per sbaglio
        // sulla richiesta verso l'host di un terzo (come il callback di un'app esterna).
        val server = FakeServer { req ->
            if (req.url.host == "bot.example.org") error(PreconditionRequired, "integrity_required")
            else if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null
        }
        val client = guard(server, FakeProvider())
        client.get("/api/cs/me") // attiva il modo proattivo
        val challengesBefore = server.challengeCalls

        val response = client.post("https://bot.example.org/telegram/callback") { setBody("""{"accepted":true}""") }

        assertEquals(PreconditionRequired, response.status, "la risposta del terzo e' restituita com'e'")
        val sent = server.requests.last()
        assertEquals("bot.example.org", sent.url.host)
        IntegrityHeaders.ALL.forEach { assertNull(sent.headers[it], it) }
        assertEquals(challengesBefore, server.challengeCalls, "nessuna sfida per un host che non e' il nostro")
        assertEquals(1, server.requests.count { it.url.host == "bot.example.org" }, "nessun retry")
    }

    @Test
    fun lookalikeHostsAreNotOurServer() = runTest {
        val server = FakeServer { error(PreconditionRequired, "integrity_required") }
        val client = guard(server, FakeProvider())

        client.get("https://svc.mensa.it.evil.example/api/cs/me")
        client.get("https://evil-svc.mensa.it/api/cs/me")

        assertEquals(2, server.requests.size)
        assertEquals(0, server.challengeCalls)
        assertTrue(server.requests.none { it.hasProof() })
    }

    // ── Bloccati: allowlist, raggiungibilita' ────────────────────────────────

    @Test
    fun successOnAnAllowlistedRouteDoesNotLiftTheSuspension() = runTest {
        // Un account sospeso: la registrazione del device e le conferme riescono lo stesso.
        val server = FakeServer { req ->
            val path = req.url.encodedPath
            if (path.startsWith("/api/collections/users_devices/") ||
                path.startsWith("/api/collections/user_notifications/") ||
                path.startsWith("/api/collections/ex_apps/") ||
                path.startsWith("/api/collections/ex_granted_permissions/") ||
                path == "/api/cs/exapp/export/decide"
            ) null else error(HttpStatusCode.Forbidden, "account_blocked")
        }
        val client = guard(server, FakeProvider())

        assertFailsWith<AccountBlockedException> { client.get("/api/collections/events/records") }
        assertTrue(AccountStatus.suspended.value)

        client.get("/api/collections/users_devices/records?filter=x")
        client.get("/api/collections/ex_apps/records/abc")
        client.post("/api/collections/ex_granted_permissions/records") { setBody("{}") }
        client.post("/api/cs/exapp/export/decide") { setBody("{}") }
        // Leggere, segnare come vista e cancellare le notifiche non riattiva l'account.
        client.get("/api/collections/user_notifications/records?filter=seen%20%3D%20null&sort=-created")
        client.patch("/api/collections/user_notifications/records/n1") { setBody("""{"seen":"2026-10-28"}""") }
        client.delete("/api/collections/user_notifications/records/n1")

        assertTrue(AccountStatus.suspended.value, "le route consentite ai bloccati non provano che sia tornato attivo")
    }

    @Test
    fun successOnAnyOtherRouteLiftsTheSuspension() = runTest {
        var blocked = true
        val server = FakeServer { if (blocked) error(HttpStatusCode.Forbidden, "account_blocked") else null }
        val client = guard(server, FakeProvider())
        assertFailsWith<AccountBlockedException> { client.get("/api/collections/events/records") }

        blocked = false
        client.get("/api/collections/events/records?perPage=1&skipTotal=1")

        assertFalse(AccountStatus.suspended.value)
    }

    @Test
    fun accountBlockedOnAnAnonymousRequestDoesNotSuspendTheSession() = runTest {
        val server = FakeServer { error(HttpStatusCode.Forbidden, "account_blocked") }
        val client = guard(server, FakeProvider(), authenticated = false)

        assertFailsWith<AccountBlockedException> { client.post("/api/cs/auth-with-zitadel") }

        assertFalse(AccountStatus.suspended.value)
    }

    @Test
    fun aTypedErrorStillCountsAsTheServerAnswering() = runTest {
        // Senza questo, un account sospeso farebbe sembrare il server irraggiungibile.
        val server = FakeServer { error(HttpStatusCode.Forbidden, "account_blocked") }
        val client = guard(server, FakeProvider())

        assertFailsWith<AccountBlockedException> { client.get("/api/health") }

        assertEquals(true, ServerReachability.reachable.value)
        assertTrue(ServerReachability.lastContact.value != null)
    }

    // ── Dispositivo e sfida ──────────────────────────────────────────────────

    @Test
    fun unsupportedDeviceIsATypedErrorWithoutLoops() = runTest {
        val server = FakeServer { error(PreconditionRequired, "integrity_required") }
        val client = guard(server, FakeProvider(supported = false))

        assertFailsWith<IntegrityUnsupportedException> { client.get("/api/collections/stamp/records") }

        assertEquals(1, server.api.size)
        assertEquals(0, server.challengeCalls)
    }

    @Test
    fun challengeFailureIsUnavailable() = runTest {
        // Sfida che risponde male: il guard lo trasforma in un errore tipizzato.
        val client = HttpClient(MockEngine { request ->
            when (request.url.encodedPath) {
                CHALLENGE -> respond("boom", HttpStatusCode.InternalServerError)
                else -> error(PreconditionRequired, "integrity_required")
            }
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest {
                url(ApiConfig.BASE_URL)
                header(HttpHeaders.Authorization, "Bearer t")
            }
        }
        IntegrityGuard(client, FakeProvider(), { "user-1" }).install()

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
    }

    // ── iOS: chiavi App Attest ───────────────────────────────────────────────

    @Test
    fun iosWithoutAKeyEnrollsThenRetries() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        var calls = 0
        val server = FakeServer { req ->
            if (req.url.encodedPath == "/api/collections/stamp/records" && ++calls == 1) {
                error(PreconditionRequired, "integrity_required")
            } else null
        }
        val client = guard(server, provider)

        assertEquals(HttpStatusCode.OK, client.get("/api/collections/stamp/records").status)

        assertEquals(
            listOf("/api/collections/stamp/records", CHALLENGE, ATTEST, CHALLENGE, "/api/collections/stamp/records"),
            server.requests.map { it.url.encodedPath },
        )
        val attest = server.requests[2].body.toByteArray().decodeToString()
        assertEquals("""{"key_id":"key-1","attestation":"att(chal-1)","challenge":"chal-1"}""", attest)
        val retry = server.requests[4]
        assertEquals("ios", retry.headers[IntegrityHeaders.PLATFORM])
        assertEquals("chal-2", retry.headers[IntegrityHeaders.CHALLENGE])
        assertEquals("key-1", retry.headers[IntegrityHeaders.KEY_ID])
        assertEquals("asr(chal-2|GET|/api/collections/stamp/records)", retry.headers[IntegrityHeaders.ASSERTION])
        assertEquals("key-1", provider.key)
    }

    @Test
    fun iosAttestKeyRequiredReEnrollsOnceAndRetries() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "old-key")
        var calls = 0
        val server = FakeServer { req ->
            if (req.url.encodedPath == "/api/cs/me" && ++calls == 1) error(PreconditionRequired, "attest_key_required") else null
        }
        val client = guard(server, provider)

        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        assertEquals(1, provider.keysGenerated)
        assertEquals("key-1", server.api.last().headers[IntegrityHeaders.KEY_ID])
    }

    @Test
    fun iosServerStillNotKnowingTheKeyForgetsItAndFailsWithoutLooping() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "old-key")
        val server = FakeServer { error(PreconditionRequired, "attest_key_required") }
        val client = guard(server, provider)

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }

        assertEquals(2, server.api.size)
        assertEquals(1, provider.keysGenerated)
        assertNull(provider.key)
        assertEquals(listOf("user-1"), provider.forgotten)
    }

    @Test
    fun iosInvalidLocalKeyIsReplacedOnce() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "stale")
        provider.invalidateKeyOnNextProve = true
        var calls = 0
        val server = FakeServer { req ->
            if (req.url.encodedPath == "/api/cs/me" && ++calls == 1) error(PreconditionRequired, "integrity_required") else null
        }
        val client = guard(server, provider)

        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        assertEquals(1, provider.keysGenerated)
        assertEquals("key-1", provider.key)
    }

    @Test
    fun iosRejectedAttestationIsAnErrorNotALoop() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val server = FakeServer { req ->
            if (req.hasProof()) null else error(PreconditionRequired, "integrity_required")
        }
        server.attestStatus = HttpStatusCode.Forbidden
        // Il fake risponde 403 senza codice: il guard non puo' registrare la chiave.
        val client = guard(server, provider)

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }

        assertNull(provider.key)
        assertEquals(1, provider.keysGenerated)
    }

    @Test
    fun concurrentRequestsEnrollOnlyOneKey() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val server = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val client = guard(server, provider)

        List(5) { async { client.get("/api/cs/me") } }.awaitAll()

        assertEquals(1, provider.keysGenerated)
    }
}
