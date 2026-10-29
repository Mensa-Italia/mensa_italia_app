package it.mensa.shared.integrity

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.content.TextContent
import it.mensa.shared.auth.AccountStatus
import it.mensa.shared.net.ServerReachability
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IntegrityGuardTest {

    @BeforeTest
    fun resetAccountStatus() = AccountStatus.clear()

    @AfterTest
    fun cleanAccountStatus() = AccountStatus.clear()

    // ── Doppioni di test ─────────────────────────────────────────────────────
    //
    // FakeProvider, FakeServer e gli altri stanno in IntegrityTestSupport.kt:
    // li usano anche i test del ciclo di vita e del report.

    private companion object {
        const val CHALLENGE = FakeServer.CHALLENGE
        const val ATTEST = FakeServer.ATTEST
    }

    /** Il guard con l'orologio del test (i backoff corrono in tempo virtuale) e i report in tempo reale. */
    private fun TestScope.guard(
        server: FakeServer,
        provider: IntegrityProvider,
        user: () -> String? = { "user-1" },
        authenticated: Boolean = true,
    ): HttpClient = server.client(authenticated).also {
        IntegrityGuard(it, provider, user, clock = testTimeSource, scope = realTimeScope()).install()
    }

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

        val response = client.get("/api/collections/events/records") { expectSuccess = false }

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
        server.clearRequests()
        client.get("/api/cs/me")
        assertFalse(server.api.first().hasProof())

        // Logout (nessun utente) e rientro dello stesso utente: ancora da zero.
        user = null
        client.get("/api/health") { expectSuccess = false }
        user = "user-2"
        server.clearRequests()
        client.get("/api/cs/me")
        assertFalse(server.api.first().hasProof())
    }

    // ── Esenzioni e anonimi ──────────────────────────────────────────────────

    @Test
    fun exemptRoutesNeverGetAProofEvenInProactiveMode() = runTest {
        val server = FakeServer { req -> if (req.url.encodedPath == "/api/cs/me" && !req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val client = guard(server, FakeProvider())
        client.get("/api/cs/me")

        server.clearRequests()
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

        val response = client.get("/api/health") { expectSuccess = false }

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

        val response = client.get("/api/collections/sigs/records") { expectSuccess = false }

        assertTrue(asked)
        assertEquals(PreconditionRequired, response.status)
        assertEquals(0, server.challengeCalls)
    }

    @Test
    fun otherHostsAreNotTouched() = runTest {
        val server = FakeServer { error(HttpStatusCode.Forbidden, "account_blocked") }
        val client = guard(server, FakeProvider())

        val response = client.get("https://auth.mensa.it/oauth/v2/token") { expectSuccess = false }

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

        val response = client.post("https://bot.example.org/telegram/callback") { expectSuccess = false; setBody("""{"accepted":true}""") }

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

        client.get("https://svc.mensa.it.evil.example/api/cs/me") { expectSuccess = false }
        client.get("https://evil-svc.mensa.it/api/cs/me") { expectSuccess = false }

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
    fun unsupportedDeviceCarriesATagAndIsRememberedForever() = runTest {
        val provider = FakeProvider(platform = "ios", supported = false)
        val server = FakeServer { error(PreconditionRequired, "integrity_required") }
        val client = guard(server, provider)

        val first = assertFailsWith<IntegrityUnsupportedException> { client.get("/api/cs/me") }
        assertEquals("generate_key/1", first.failure?.tag)
        assertEquals(Disposition.UNSUPPORTED, first.failure?.disposition)

        // Senza scadenza: neanche dopo ore si riprova (e' il dispositivo, non il momento).
        delay(3.hours)
        val again = assertFailsWith<IntegrityUnsupportedException> { client.get("/api/cs/me") }

        assertEquals(first.failure, again.failure)
        assertEquals(0, server.challengeCalls)
        // Le due richieste sono andate nude, a decidere e' il server.
        assertEquals(2, server.api.size)
        assertTrue(server.api.none { it.hasProof() })
    }

    /** `generate_key` e' uno stage di App Attest: su Android sarebbe una bugia nel messaggio e nel log del server. */
    @Test
    fun anUnsupportedDeviceThatIsNotIosHasNoAppAttestTagAndReportsNothing() = runTest {
        for (platform in listOf("android", "unsupported")) {
            val server = watchedServer()
            val client = guard(server, FakeProvider(platform = platform, supported = false))

            val first = assertFailsWith<IntegrityUnsupportedException> { client.get("/api/cs/me") }
            val again = assertFailsWith<IntegrityUnsupportedException> { client.get("/api/cs/me") }

            assertNull(first.failure, platform)
            assertNull(again.failure, platform)
            assertEquals(0, server.challengeCalls, platform)
            awaitReports(server, 0)
            assertTrue(server.reports.isEmpty(), "$platform: niente report con uno stage che non e' suo")
        }
    }

    @Test
    fun challengeFailureIsUnavailable() = runTest {
        // Sfida che risponde male: il guard lo trasforma in un errore tipizzato, con la sua causa.
        val server = FakeServer { error(PreconditionRequired, "integrity_required") }
        server.challengeStatus = HttpStatusCode.InternalServerError
        val client = guard(server, FakeProvider())

        val e = assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }

        assertEquals("challenge/500", e.failure?.tag)
        assertEquals("http", e.failure?.domain)
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

    /**
     * Il server perde la chiave: la richiesta (proattiva, con la prova) viene
     * rifiutata con `attest_key_required`. Si butta la chiave che QUELLA richiesta
     * ha mandato, se ne registra una nuova e si riprova, tutto dentro la stessa richiesta.
     */
    @Test
    fun iosAttestKeyRequiredDiscardsTheKeyTheRequestSentAndReEnrollsOnce() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "old-key")
        var serverKnowsOldKey = true
        val server = FakeServer { req ->
            when {
                !req.hasProof() -> error(PreconditionRequired, "integrity_required")
                req.headers[IntegrityHeaders.KEY_ID] == "old-key" && !serverKnowsOldKey ->
                    error(PreconditionRequired, "attest_key_required")
                else -> null
            }
        }
        val client = guard(server, provider)
        client.get("/api/cs/me") // 428 nuda, prova con old-key: il server la conosce

        serverKnowsOldKey = false
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        assertEquals(1, provider.keysGenerated)
        assertEquals(listOf<String?>("old-key"), provider.discarded)
        assertEquals("key-1", server.api.last().headers[IntegrityHeaders.KEY_ID])
    }

    @Test
    fun iosServerStillNotKnowingTheKeyForgetsItAndFailsWithoutLooping() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "old-key")
        val server = FakeServer { req ->
            if (req.hasProof()) error(PreconditionRequired, "attest_key_required")
            else error(PreconditionRequired, "integrity_required")
        }
        val client = guard(server, provider)

        val e = assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }

        // La richiesta nuda e un solo retry con la prova: niente terzo giro.
        assertEquals(2, server.api.size)
        assertEquals("server/428", e.failure?.tag)
        // La chiave che il server ha rifiutato e' quella che la SECONDA richiesta ha mandato.
        assertNull(provider.key)
        assertEquals(listOf<String?>("old-key"), provider.discarded)
        assertEquals(1, server.challengeCalls)

        // Il rifiuto e' ricordato: la richiesta dopo non rifa' nessuna prova.
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, server.challengeCalls)
        assertEquals(3, server.api.size, "solo la richiesta nuda")
    }

    @Test
    fun iosAttestKeyRequiredOnANakedRequestDiscardsNothing() = runTest {
        // La richiesta rifiutata non ha mandato nessuna chiave: non c'e' niente da buttare.
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "good-key")
        var calls = 0
        val server = FakeServer { req ->
            if (req.url.encodedPath == "/api/cs/me" && ++calls == 1) error(PreconditionRequired, "attest_key_required") else null
        }
        val client = guard(server, provider)

        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        assertTrue(provider.discarded.isEmpty())
        assertEquals(0, provider.keysGenerated)
        assertEquals("good-key", server.api.last().headers[IntegrityHeaders.KEY_ID])
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
        assertEquals(listOf<String?>("stale"), provider.discarded)
    }

    @Test
    fun aSecondInvalidKeyRightAfterTheRotationIsAFailureNotALap() = runTest {
        // Anche la chiave appena registrata non firma: il guard non gira in tondo.
        val failure = ProofFailure(AttestStage.ASSERT, DC_DOMAIN, 2, "boh", null, Disposition.DISCARD_KEY)
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "stale")
        provider.invalidKeyFailure = failure
        provider.staleKeys += listOf("stale", "key-1")
        val server = FakeServer { req -> if (req.hasProof()) null else error(PreconditionRequired, "integrity_required") }
        val client = guard(server, provider)

        val e = assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }

        assertEquals(failure, e.failure)
        assertEquals(1, provider.keysGenerated, "una sola rotazione")
        // E il fallimento e' ricordato: la richiesta dopo non ricomincia.
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, provider.keysGenerated)
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

        val e = assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }

        assertNull(provider.key)
        assertEquals(1, provider.keysGenerated)
        assertEquals("register/403", e.failure?.tag)
    }

    @Test
    fun concurrentRequestsEnrollOnlyOneKey() = runTest {
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val server = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val client = guard(server, provider)

        List(5) { async { client.get("/api/cs/me") } }.awaitAll()

        assertEquals(1, provider.keysGenerated)
    }

    // ── Sessione ─────────────────────────────────────────────────────────────

    /**
     * Un server che si comporta come quello vero: senza prova 428, con la prova
     * completa ok + una sessione nuova, con la sessione ok solo se e' l'ultima
     * emessa.
     */
    private inner class SessionServer(var ttl: Int = 600) {
        var issued = 0
        var valid: String? = null
        val fullProofs = mutableListOf<String>()

        fun route(scope: MockRequestHandleScope, req: HttpRequestData) = with(scope) {
            val path = req.url.encodedPath
            val session = req.headers[IntegrityHeaders.SESSION]
            when {
                session != null ->
                    if (session == valid) null
                    else error(PreconditionRequired, "integrity_required")
                req.headers.contains(IntegrityHeaders.TOKEN) ||
                    req.headers.contains(IntegrityHeaders.ASSERTION) -> {
                    fullProofs += path
                    val s = "sess-${++issued}"
                    valid = s
                    respond(
                        "{}", HttpStatusCode.OK,
                        headersOf(
                            HttpHeaders.ContentType to listOf(ContentType.Application.Json.toString()),
                            IntegrityHeaders.SESSION to listOf(s),
                            IntegrityHeaders.SESSION_TTL to listOf(ttl.toString()),
                        ),
                    )
                }
                else -> error(PreconditionRequired, "integrity_required")
            }
        }
    }

    @Test
    fun aBurstOfRequestsCostsOneFullProof() = runTest {
        val fake = SessionServer()
        val server = FakeServer { fake.route(this, it) }
        val client = guard(server, FakeProvider())

        val responses = List(12) { i -> async { client.get("/api/collections/events/records?page=$i") } }.awaitAll()

        assertTrue(responses.all { it.status == HttpStatusCode.OK })
        assertEquals(1, fake.fullProofs.size)
        assertEquals(1, server.challengeCalls)
        // e dopo, niente piu' sfide
        client.get("/api/cs/me")
        assertEquals(1, server.challengeCalls)
        assertEquals("sess-1", server.api.last().headers[IntegrityHeaders.SESSION])
        assertNull(server.api.last().headers[IntegrityHeaders.CHALLENGE])
    }

    @Test
    fun stampsAndMedalsUseTheSessionToo() = runTest {
        val fake = SessionServer()
        val server = FakeServer { fake.route(this, it) }
        val client = guard(server, FakeProvider())

        client.get("/api/collections/events/records")
        client.get("/api/collections/stamp_users/records")
        client.get("/api/collections/view_medals/records")

        assertEquals(listOf("/api/collections/events/records"), fake.fullProofs)
        // il primo 428, la prova completa, poi solo sessione
        server.api.drop(2).forEach { assertEquals("sess-1", it.headers[IntegrityHeaders.SESSION]) }
    }

    @Test
    fun aRejectedSessionFallsBackToTheFullProof() = runTest {
        val fake = SessionServer()
        val server = FakeServer { fake.route(this, it) }
        val client = guard(server, FakeProvider())

        client.get("/api/collections/events/records")
        fake.valid = null // server riavviato: la sessione non vale piu'

        assertEquals(HttpStatusCode.OK, client.get("/api/collections/events/records").status)
        assertEquals(2, fake.fullProofs.size)
        assertEquals("sess-2", fake.valid)
        client.get("/api/cs/me")
        assertEquals("sess-2", server.api.last().headers[IntegrityHeaders.SESSION])
    }

    @Test
    fun anAlmostExpiredSessionIsNotUsed() = runTest {
        val fake = SessionServer(ttl = 20) // meno del margine: inutilizzabile da subito
        val server = FakeServer { fake.route(this, it) }
        val client = guard(server, FakeProvider())

        client.get("/api/collections/events/records")
        client.get("/api/cs/me")

        assertEquals(2, fake.fullProofs.size)
        assertTrue(server.api.none { it.headers.contains(IntegrityHeaders.SESSION) })
    }

    @Test
    fun iosUsesTheSessionToo() = runTest {
        val fake = SessionServer()
        val server = FakeServer { fake.route(this, it) }
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val client = guard(server, provider)

        val responses = List(12) { i -> async { client.get("/api/collections/events/records?page=$i") } }.awaitAll()

        assertTrue(responses.all { it.status == HttpStatusCode.OK })
        assertEquals(1, fake.fullProofs.size)
        assertEquals(1, provider.keysGenerated)
    }

    /** Le asserzioni App Attest con la stessa chiave non devono mai partire insieme. */
    @Test
    fun iosProofsNeverRunConcurrently() = runTest {
        // server senza sessioni: ogni richiesta vuole la prova completa
        val server = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "key-0")
        val client = guard(server, provider)

        client.get("/api/cs/me")
        val responses = List(10) { i -> async { client.get("/api/collections/events/records?page=$i") } }.awaitAll()

        assertTrue(responses.all { it.status == HttpStatusCode.OK })
        assertEquals(1, provider.maxConcurrentProofs)
    }

    /** Una chiave invalida scoperta da venti richieste insieme si rifa' una volta sola. */
    @Test
    fun aStaleKeyIsReenrolledOnce() = runTest {
        val server = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "stale")
        provider.staleKeys += "stale"
        val client = guard(server, provider)

        val responses = List(10) { i -> async { client.get("/api/collections/events/records?page=$i") } }.awaitAll()

        assertTrue(responses.all { it.status == HttpStatusCode.OK })
        assertEquals(1, provider.keysGenerated)
        assertEquals("key-1", provider.key)
    }

    @Test
    fun theSessionBelongsToTheUser() = runTest {
        val fake = SessionServer()
        val server = FakeServer { fake.route(this, it) }
        var user = "user-1"
        val client = guard(server, FakeProvider(), user = { user })

        client.get("/api/collections/events/records")
        user = "user-2"
        client.get("/api/collections/events/records")

        assertNull(server.api.last().headers[IntegrityHeaders.SESSION])
    }

    // ── Memoria del fallimento ───────────────────────────────────────────────

    private fun attestFailure(code: Int = 2) =
        ProofFailure(AttestStage.ATTEST_KEY, DC_DOMAIN, code, "errore $code", "NSPOSIXErrorDomain/1: boh", Disposition.DISCARD_KEY, "pending", 0)

    /** Un dispositivo iOS la cui registrazione cade sempre all'`attestKey`. */
    private fun failingProvider(code: Int = 2) = FakeProvider(
        platform = "ios", requiresKeyEnrollment = true, env = TEST_ENVIRONMENT,
    ).also {
        it.enrollmentFailure = IntegrityUnavailableException("App Attest (attest_key/$code)", failure = attestFailure(code))
    }

    /** Un server `watched`: senza la prova 428, con la prova ok. */
    private fun watchedServer() = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }

    @Test
    fun aBurstWithAFailingEnrollmentCostsOneChallengeAndOneReport() = runTest {
        val provider = failingProvider()
        val server = watchedServer()
        val client = guard(server, provider)

        val results = List(20) { i -> async { runCatching { client.get("/api/collections/events/records?page=$i") } } }.awaitAll()
        awaitReports(server, 1)

        val errors = results.map { it.exceptionOrNull() }
        assertTrue(errors.all { it is IntegrityUnavailableException }, "venti errori")
        assertEquals(1, errors.map { it?.message }.toSet().size, "identici")
        assertEquals("attest_key/2", (errors.first() as IntegrityUnavailableException).failure?.tag)
        // Una sola sfida, un solo tentativo, un solo report: gli altri 19 hanno preso l'errore noto.
        assertEquals(1, server.challengeCalls)
        assertEquals(1, provider.enrollAttempts)
        assertEquals(1, server.reports.size)
    }

    @Test
    fun aSecondWaveInsideTheWindowTouchesNeitherTheProviderNorTheChallenge() = runTest {
        val provider = failingProvider()
        val server = watchedServer()
        val client = guard(server, provider)
        List(5) { async { runCatching { client.get("/api/cs/me") } } }.awaitAll()
        val challenges = server.challengeCalls
        val attempts = provider.enrollAttempts

        delay(10.seconds)
        val wave = List(20) { i -> async { runCatching { client.get("/api/collections/events/records?page=$i") } } }.awaitAll()
        awaitReports(server, 1)

        assertTrue(wave.all { it.exceptionOrNull() is IntegrityUnavailableException })
        assertEquals(challenges, server.challengeCalls, "nessuna sfida")
        assertEquals(attempts, provider.enrollAttempts, "nessuna chiamata al provider")
        assertEquals(1, server.reports.size, "un errore servito dalla memoria non si riporta")
        // Le richieste sono partite NUDE: nessuna prova, ognuna ha preso il suo 428.
        assertTrue(server.api.none { it.hasProof() })
        assertEquals(25, server.api.size)
    }

    @Test
    fun backoffIs30sThen2minThen8minThenCapsAtTenMinutes() = runTest {
        val provider = failingProvider()
        val client = guard(watchedServer(), provider)

        suspend fun request() = assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        suspend fun noNewAttemptAfter(wait: kotlin.time.Duration) {
            val before = provider.enrollAttempts
            delay(wait)
            request()
            assertEquals(before, provider.enrollAttempts, "ancora dentro la finestra dopo $wait")
        }
        suspend fun newAttemptAfter(wait: kotlin.time.Duration) {
            val before = provider.enrollAttempts
            delay(wait)
            request()
            assertEquals(before + 1, provider.enrollAttempts, "finestra scaduta dopo $wait")
        }

        request()                                  // t = 0, tentativo 1 -> 30 s
        assertEquals(1, provider.enrollAttempts)
        noNewAttemptAfter(29.seconds); newAttemptAfter(1.seconds)      // t = 30 s, tentativo 2 -> 2 min
        noNewAttemptAfter(119.seconds); newAttemptAfter(1.seconds)     // t = 150 s, tentativo 3 -> 8 min
        noNewAttemptAfter(479.seconds); newAttemptAfter(1.seconds)     // t = 630 s, tentativo 4 -> tetto 10 min
        noNewAttemptAfter(599.seconds); newAttemptAfter(1.seconds)     // tentativo 5 -> ancora 10 min
        noNewAttemptAfter(599.seconds); newAttemptAfter(1.seconds)
    }

    @Test
    fun aSuccessResetsTheAttemptsSoTheNextFailureStartsAgainAt30s() = runTest {
        val fake = SessionServer()
        val server = FakeServer { fake.route(this, it) }
        val provider = failingProvider()
        val client = guard(server, provider)
        suspend fun request() = runCatching { client.get("/api/cs/me") }

        request()                                   // tentativo 1, finestra 30 s
        delay(30.seconds); request()                // tentativo 2, finestra 2 min
        assertEquals(2, provider.enrollAttempts)

        provider.enrollmentFailure = null           // il guasto passa
        delay(120.seconds)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        assertEquals(3, provider.enrollAttempts)

        // Nuovo guasto, questa volta alla firma: riparte da 30 s, non da 8 minuti.
        provider.proveFailure = IntegrityUnavailableException("firma", failure = attestFailure(0))
        fake.valid = null; delay(1.seconds)
        assertTrue(request().isFailure)
        val calls = provider.proveCalls
        delay(29.seconds); assertTrue(request().isFailure)
        assertEquals(calls, provider.proveCalls, "dentro i 30 s")
        delay(1.seconds); assertTrue(request().isFailure)
        assertEquals(calls + 1, provider.proveCalls, "scaduti i 30 s si riprova")
    }

    @Test
    fun aRememberedFailureDoesNotKeepTheAppLockedIfTheAccountIsFreeAgain() = runTest {
        var watched = true
        val server = FakeServer { req -> if (watched && !req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        val provider = failingProvider()
        val client = guard(server, provider)
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, provider.enrollAttempts)

        // Il titolare riporta l'account a free mentre l'app e' aperta: la richiesta nuda passa.
        watched = false
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        // E scaduta la finestra NON si ritenta una prova: l'app non chiede piu' di provare.
        delay(31.seconds)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        assertEquals(1, provider.enrollAttempts)
        assertTrue(server.api.none { it.hasProof() })
    }

    @Test
    fun changingOrLosingTheUserForgetsTheFailureEvenWithoutAReset() = runTest {
        var user: String? = "user-1"
        val provider = failingProvider()
        val client = guard(watchedServer(), provider, user = { user })
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, provider.enrollAttempts)

        // Altro utente: il suo primo tentativo parte subito, senza aspettare i 30 s di un altro.
        user = "user-2"
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(2, provider.enrollAttempts)

        // Logout e rientro dello stesso utente: ancora da zero.
        user = null
        client.get("/api/health") { expectSuccess = false }
        user = "user-2"
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(3, provider.enrollAttempts)
    }

    /** `reset()` e' quello che l'app chiama al logout: il fallimento dell'utente di prima non deve sopravvivergli. */
    @Test
    fun resetForgetsTheRememberedFailureAndTheAttemptCount() = runTest {
        val provider = failingProvider()
        val server = watchedServer()
        val client = server.client()
        val guard = IntegrityGuard(client, provider, { "user-1" }, clock = testTimeSource, scope = realTimeScope())
        guard.install()
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, provider.enrollAttempts)

        // Dentro i 30 s la richiesta dopo non ritenta...
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, provider.enrollAttempts)

        // ...ma dopo un reset si riparte da zero, subito, e dal primo tentativo (30 s, non 2 minuti).
        guard.reset()
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(2, provider.enrollAttempts)
        awaitReports(server, 2)
        val attempts = server.reports.map {
            Json.parseToJsonElement(it.body.toByteArray().decodeToString()).jsonObject["attempt"]!!.jsonPrimitive.content.toInt()
        }
        assertEquals(listOf(1, 1), attempts)
    }

    /**
     * Play Integrity non allega un `ProofFailure`, ma il suo guasto passeggero (troppe
     * richieste, rete, errore di Google) non deve ripetere sfida + chiamata a Google a
     * ogni richiesta: ha il suo backoff come App Attest. Non si riporta, non c'e' uno stage da descrivere.
     */
    @Test
    fun aFailureWithoutACauseIsRememberedWithTheBackoffButNeverReported() = runTest {
        val server = watchedServer()
        val provider = FakeProvider()
        provider.proveFailure = IntegrityUnavailableException("Play Integrity non ha risposto (codice -8)")
        val client = guard(server, provider)

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, provider.proveCalls)
        assertEquals(1, server.challengeCalls)

        // Dentro i 30 s: nessuna sfida, nessuna chiamata a Play Integrity, le richieste partono nude.
        delay(10.seconds)
        val wave = List(5) { async { runCatching { client.get("/api/cs/me") } } }.awaitAll()
        assertTrue(wave.all { it.exceptionOrNull() is IntegrityUnavailableException })
        assertEquals(1, provider.proveCalls)
        assertEquals(1, server.challengeCalls)
        assertTrue(server.api.none { it.hasProof() })

        // Scaduti i 30 s si riprova, e a guasto passato si entra.
        provider.proveFailure = null
        delay(21.seconds)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        assertEquals(2, provider.proveCalls)

        awaitReports(server, 0)
        assertTrue(server.reports.isEmpty(), "niente stage da descrivere: niente report")
    }

    @Test
    fun anOfflineChallengeIsNotRememberedAndRecoversAsSoonAsTheNetworkReturns() = runTest {
        val server = watchedServer()
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val client = guard(server, provider)
        server.challengeError = kotlinx.io.IOException("offline")

        assertFailsWith<kotlinx.io.IOException> { client.get("/api/cs/me") }
        assertFailsWith<kotlinx.io.IOException> { client.get("/api/cs/me") }
        // Nessuna memoria: ogni richiesta ha riprovato la sfida.
        assertEquals(2, server.requests.count { it.url.encodedPath == CHALLENGE })

        server.challengeError = null
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        awaitReports(server, 0)
        assertTrue(server.reports.isEmpty(), "offline non e' un guasto di integrita': nessun report")
    }

    @Test
    fun aServerErrorOnTheChallengeIsRememberedButAClientErrorIsNot() = runTest {
        val server = watchedServer()
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val client = guard(server, provider)

        server.challengeStatus = HttpStatusCode.ServiceUnavailable
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, server.requests.count { it.url.encodedPath == CHALLENGE }, "il secondo 503 viene dalla memoria")

        // Un 4xx sulla sfida (un 401 con un token da rinfrescare, per dire) non e' un guasto di integrita'.
        val other = watchedServer()
        other.challengeStatus = HttpStatusCode.Unauthorized
        val client2 = guard(other, FakeProvider(platform = "ios", requiresKeyEnrollment = true))
        assertFailsWith<IntegrityUnavailableException> { client2.get("/api/cs/me") }
        assertFailsWith<IntegrityUnavailableException> { client2.get("/api/cs/me") }
        assertEquals(2, other.requests.count { it.url.encodedPath == CHALLENGE }, "ogni richiesta riprova")
        awaitReports(server, 1)
        awaitReports(other, 0)
        assertEquals(1, server.reports.size)
        assertEquals(0, other.reports.size)
    }

    @Test
    fun serverVerdictsOnAUserRequestAreNeverRemembered() = runTest {
        // 403 integrity_failed su una richiesta dell'utente che porta la prova: un
        // verdetto del server, non un guasto di costruzione.
        val server = FakeServer { req -> if (req.hasProof()) error(HttpStatusCode.Forbidden, "integrity_failed") else error(PreconditionRequired, "integrity_required") }
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val client = guard(server, provider)

        assertFailsWith<IntegrityFailedException> { client.get("/api/cs/me") }
        assertFailsWith<IntegrityFailedException> { client.get("/api/cs/me") }

        // Ogni richiesta rifa la sua prova (sfida + asserzione, nessuna chiave nuova): nessuna memoria.
        assertEquals(1, provider.keysGenerated)
        assertEquals(2, provider.proveCalls)
        awaitReports(server, 0)
        assertTrue(server.reports.isEmpty())
    }

    /**
     * Il server che rifiuta l'attestazione risponde 403 `integrity_failed` (e' la
     * `reject` di `iosAttestHandler`). L'interceptor lo trasforma in
     * [IntegrityFailedException] prima che `register` veda lo status: se restasse
     * cosi' non sarebbe ricordato, e ogni richiesta in coda conierebbe un'altra chiave.
     */
    @Test
    fun aRegistrationTheServerRefusesWithIntegrityFailedIsARememberedRegisterFailure() = runTest {
        val server = watchedServer()
        server.attestStatus = HttpStatusCode.Forbidden
        server.attestBody = """{"status":403,"message":"no","code":"integrity_failed"}"""
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val client = guard(server, provider)

        val results = List(20) { i -> async { runCatching { client.get("/api/collections/events/records?page=$i") } } }.awaitAll()
        awaitReports(server, 1)

        val errors = results.map { it.exceptionOrNull() }
        assertTrue(errors.all { it is IntegrityUnavailableException }, "venti errori della stessa causa, non verdetti sulla richiesta")
        assertEquals(setOf("register/403"), errors.map { (it as IntegrityUnavailableException).failure?.tag }.toSet())
        assertEquals(1, provider.enrollAttempts, "una sola registrazione, non una per richiesta")
        assertEquals(1, provider.keysGenerated)
        assertEquals(1, server.requests.count { it.url.encodedPath == ATTEST })
        assertEquals(1, server.challengeCalls)
        assertEquals(1, server.reports.size)
        val body = server.reports.single().body.toByteArray().decodeToString()
        assertTrue("\"stage\":\"register\"" in body && "\"code\":403" in body, body)

        // Una seconda ondata prima della scadenza: zero registrazioni, zero sfide.
        delay(10.seconds)
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, provider.enrollAttempts)
        assertEquals(1, server.requests.count { it.url.encodedPath == ATTEST })
        assertEquals(1, server.challengeCalls)

        // Passato il backoff si riprova, con una chiave nuova (la prima e' consumata).
        delay(30.seconds)
        server.attestStatus = HttpStatusCode.OK
        server.attestBody = """{"ok":true}"""
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        assertEquals(2, provider.keysGenerated)
    }

    @Test
    fun anAccountBlockedOnTheRegistrationIsNotRemembered() = runTest {
        val server = watchedServer()
        server.attestStatus = HttpStatusCode.Forbidden
        server.attestBody = """{"status":403,"message":"no","code":"account_blocked"}"""
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true)
        val client = guard(server, provider)

        assertFailsWith<AccountBlockedException> { client.get("/api/cs/me") }
        assertFailsWith<AccountBlockedException> { client.get("/api/cs/me") }

        assertEquals(2, provider.enrollAttempts, "un account sospeso non e' un guasto: nessuna memoria")
        awaitReports(server, 0)
        assertTrue(server.reports.isEmpty())
    }
}
