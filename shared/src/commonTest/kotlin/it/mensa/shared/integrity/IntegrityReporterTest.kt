package it.mensa.shared.integrity

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.get
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import it.mensa.shared.auth.AccountStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Il report del fallimento al server (`POST /api/integrity/report`): cosa
 * manda, che non sta mai sul percorso della richiesta, e che non puo' rompere
 * niente. Lato server il report descrive e non concede; qui si prova che il
 * client lo manda per un fallimento fresco e basta.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IntegrityReporterTest {

    @BeforeTest
    fun resetAccountStatus() = AccountStatus.clear()

    @AfterTest
    fun cleanAccountStatus() = AccountStatus.clear()

    private fun TestScope.guard(server: FakeServer, provider: IntegrityProvider): HttpClient =
        server.client().also {
            IntegrityGuard(it, provider, { "user-1" }, clock = testTimeSource, scope = realTimeScope()).install()
        }

    private fun watched() = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }

    private val staleAttest = ProofFailure(
        stage = AttestStage.ATTEST_KEY,
        domain = DC_DOMAIN,
        code = 2,
        detail = "The operation couldn't be completed.",
        underlying = "NSOSStatusErrorDomain/-1: Invalid appUUID",
        disposition = Disposition.DISCARD_KEY,
        keyPhase = "pending",
        keyAgeSeconds = 12,
    )

    private fun failingProvider(failure: ProofFailure = staleAttest, env: DeviceEnvironment = TEST_ENVIRONMENT) =
        FakeProvider(platform = "ios", requiresKeyEnrollment = true, env = env).also {
            it.enrollmentFailure = IntegrityUnavailableException("App Attest (${failure.tag})", failure = failure)
        }

    private suspend fun FakeServer.reportJson(index: Int = 0): JsonObject =
        Json.parseToJsonElement(reports[index].body.toByteArray().decodeToString()).jsonObject

    // ── Cosa si manda ────────────────────────────────────────────────────────

    @Test
    fun theReportCarriesTheExactPayload() = runTest {
        val server = watched()
        val client = guard(server, failingProvider())

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 1)

        assertEquals(1, server.reports.size)
        val expected = buildJsonObject {
            put("platform", "ios")
            put("stage", "attest_key")
            put("domain", "com.apple.devicecheck.error")
            put("code", 2)
            put("detail", "The operation couldn't be completed.")
            put("underlying", "NSOSStatusErrorDomain/-1: Invalid appUUID")
            put("disposition", "discard_key")
            put("outcome", "failed")
            put("attempt", 1)
            put("key_phase", "pending")
            put("key_age_s", 12)
            put("elapsed_ms", 0)
            put("app", "31000984")
            put("os", "iOS 27.0.1 (24A446)")
            put("ios_app_on_mac", false)
        }
        assertEquals(expected, server.reportJson())
        val request = server.reports.single()
        assertEquals("POST", request.method.value)
        assertTrue(request.body.contentType.toString().startsWith("application/json"))
    }

    @Test
    fun theAttemptNumberGrowsWithTheBackoffAndMissingFieldsAreOmitted() = runTest {
        val server = watched()
        // Senza dominio, codice, errore sotto e ambiente: i campi opzionali non ci sono.
        val bare = ProofFailure(AttestStage.CHALLENGE, null, 503, "giu'", null, Disposition.TRANSIENT)
        val client = guard(server, failingProvider(bare, env = DeviceEnvironment.UNKNOWN))

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 1)
        delay(30.seconds)
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 2)

        val first = server.reportJson(0)
        val second = server.reportJson(1)
        assertEquals(1, first["attempt"]!!.jsonPrimitive.content.toInt())
        assertEquals(2, second["attempt"]!!.jsonPrimitive.content.toInt())
        listOf("domain", "underlying", "app", "os").forEach { assertNull(first[it], it) }
        assertEquals("challenge", first["stage"]!!.jsonPrimitive.content)
        assertEquals(503, first["code"]!!.jsonPrimitive.content.toInt())
        assertEquals("none", first["key_phase"]!!.jsonPrimitive.content)
        assertEquals(-1, first["key_age_s"]!!.jsonPrimitive.content.toInt())
        assertEquals(false, first["ios_app_on_mac"]!!.jsonPrimitive.content.toBoolean())
    }

    @Test
    fun theTextsAreCutAt200Characters() = runTest {
        val server = watched()
        val long = ProofFailure(
            AttestStage.ASSERT, DC_DOMAIN, 0, "d".repeat(500), "u".repeat(500), Disposition.DISCARD_KEY,
        )
        val client = guard(server, failingProvider(long))

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 1)

        assertEquals(200, server.reportJson()["detail"]!!.jsonPrimitive.content.length)
        assertEquals(200, server.reportJson()["underlying"]!!.jsonPrimitive.content.length)
    }

    @Test
    fun iosAppOnMacIsReportedSoTheReviewersCanBeToldApart() = runTest {
        val server = watched()
        val client = guard(server, failingProvider(env = DeviceEnvironment("31000984", "iOS 27.0.1 (24A446)", iosAppOnMac = true)))

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 1)

        assertEquals(JsonPrimitive(true), server.reportJson()["ios_app_on_mac"])
    }

    // ── Esente, a senso unico, mai sul percorso ──────────────────────────────

    @Test
    fun theReportRequestCarriesNoProofAndCreatesNoChallenge() = runTest {
        val server = watched()
        val client = guard(server, failingProvider())

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 1)

        val report = server.reports.single()
        IntegrityHeaders.ALL.forEach { assertNull(report.headers[it], it) }
        // E' autenticata come tutte le altre (l'utente il server lo prende da li').
        assertEquals("Bearer test-token", report.headers[HttpHeaders.Authorization])
        // Una sola sfida, quella del tentativo: il report non ne ha chiesta una, e non ha preso un 428.
        assertEquals(1, server.challengeCalls)
    }

    @Test
    fun aServerThatIsDownDoesNotChangeWhatTheRequestSees() = runTest {
        val down = listOf<(FakeServer) -> Unit>(
            { it.reportError = kotlinx.io.IOException("giu'") },
            { it.reportStatus = HttpStatusCode.InternalServerError },
            { it.reportStatus = HttpStatusCode.NotFound }, // server vecchio senza la route
        )
        for (breakIt in down) {
            val server = watched()
            breakIt(server)
            val client = guard(server, failingProvider())

            val e = assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
            awaitReports(server, 1)

            assertEquals("attest_key/2", e.failure?.tag, "l'errore e' quello della prova, non del report")
            assertEquals(1, server.reports.size)
        }
    }

    /**
     * Un server che prende la richiesta e non risponde mai non deve lasciare una
     * coroutine appesa per sempre nello scope dell'app: dopo 3 s il report si
     * abbandona. Tempo virtuale: il motore finto non risponde, quindi il timeout
     * scade senza aspettarlo davvero.
     */
    @Test
    fun aReportEndpointThatNeverAnswersIsGivenUpAfterThreeSeconds() = runTest {
        val server = watched()
        server.onReport = { awaitCancellation() }
        val reporter = IntegrityReporter(server.client(), backgroundScope)
        val t0 = currentTime

        val job = reporter.report(
            platform = "ios",
            environment = TEST_ENVIRONMENT,
            failure = staleAttest,
            outcome = ReportOutcome.FAILED,
            attempt = 1,
            elapsedMs = 0,
        )
        // Il tetto in tempo REALE: senza il timeout del reporter il job non finirebbe mai.
        withContext(Dispatchers.Default) { withTimeout(10_000) { job.join() } }

        assertTrue(job.isCompleted)
        assertFalse(job.isCancelled, "scade da solo, non e' stato annullato da fuori")
        assertEquals(3_000L, currentTime - t0)
    }

    @Test
    fun anAccountBlockedAnswerToTheReportDoesNotBreakTheRequestInFlight() = runTest {
        val server = watched()
        server.reportStatus = HttpStatusCode.Forbidden
        server.reportResponseBody = """{"status":403,"message":"sospeso","code":"account_blocked"}"""
        val client = guard(server, failingProvider())

        // Chi aspetta riceve il SUO errore, non quello del report.
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 1)

        assertEquals(1, server.reports.size)
        // (Il blocco, quello si', resta valido: lo stato globale lo prende come da qualunque 403 autenticato.)
        withContext(Dispatchers.Default) { withTimeout(5_000) { while (!AccountStatus.suspended.value) delay(5) } }
        assertTrue(AccountStatus.suspended.value)
    }

    // ── Quando si manda ──────────────────────────────────────────────────────

    @Test
    fun aRotationReportsTheOriginalErrorOfTheDiscardedKeyEvenWhenEverythingThenSucceeds() = runTest {
        val server = watched()
        val original = ProofFailure(
            AttestStage.ASSERT, DC_DOMAIN, 0, "unknown system failure", "NSOSStatusErrorDomain/-1: boh",
            Disposition.DISCARD_KEY, keyPhase = "ready", keyAgeSeconds = -1,
        )
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "stale", env = TEST_ENVIRONMENT)
        provider.staleKeys += "stale"
        provider.invalidKeyFailure = original
        val client = guard(server, provider)

        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        awaitReports(server, 1)

        val body = server.reportJson()
        assertEquals(1, server.reports.size)
        assertEquals("rotated", body["outcome"]!!.jsonPrimitive.content)
        assertEquals("assertion", body["stage"]!!.jsonPrimitive.content)
        assertEquals(0, body["code"]!!.jsonPrimitive.content.toInt())
        assertEquals("NSOSStatusErrorDomain/-1: boh", body["underlying"]!!.jsonPrimitive.content)
        assertEquals("ready", body["key_phase"]!!.jsonPrimitive.content)
        assertEquals(-1, body["key_age_s"]!!.jsonPrimitive.content.toInt())
        assertEquals(1, body["attempt"]!!.jsonPrimitive.content.toInt())
    }

    @Test
    fun aRotationWhoseRegistrationThenFailsReportsBoth() = runTest {
        val server = watched()
        val original = ProofFailure(AttestStage.ASSERT, DC_DOMAIN, 2, "boh", null, Disposition.DISCARD_KEY, "ready", -1)
        val registration = ProofFailure(AttestStage.ATTEST_KEY, DC_DOMAIN, 2, "no", null, Disposition.DISCARD_KEY, "pending", 0)
        val provider = FakeProvider(platform = "ios", requiresKeyEnrollment = true, key = "stale", env = TEST_ENVIRONMENT)
        provider.staleKeys += "stale"
        provider.invalidKeyFailure = original
        provider.enrollmentFailure = IntegrityUnavailableException("no", failure = registration)
        val client = guard(server, provider)

        // key e' "stale": il primo ensureEnrolled non fa niente, la firma ruota, la nuova registrazione cade.
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 2)

        // (Le due richieste partono insieme: l'ordine d'arrivo non e' garantito, il contenuto si'.)
        assertEquals(
            setOf("rotated" to "assertion", "failed" to "attest_key"),
            (0 until server.reports.size).map {
                server.reportJson(it)["outcome"]!!.jsonPrimitive.content to server.reportJson(it)["stage"]!!.jsonPrimitive.content
            }.toSet(),
        )
        assertEquals(2, server.reports.size)
    }

    @Test
    fun noReportForAnErrorServedFromMemoryOrForRequestsThatSimplySucceed() = runTest {
        val server = watched()
        val provider = failingProvider()
        val client = guard(server, provider)
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        repeat(10) { assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") } }
        awaitReports(server, 1)
        assertEquals(1, server.reports.size)

        // E con un dispositivo che funziona non si riporta niente.
        val fine = watched()
        val ok = guard(fine, FakeProvider(platform = "ios", requiresKeyEnrollment = true, env = TEST_ENVIRONMENT))
        assertEquals(HttpStatusCode.OK, ok.get("/api/cs/me").status)
        awaitReports(fine, 0)
        assertTrue(fine.reports.isEmpty())
    }

    @Test
    fun aPlatformWithoutACauseIsNotReported() = runTest {
        // Play Integrity, per ora, non allega un ProofFailure: niente da riportare.
        val server = watched()
        val provider = FakeProvider()
        provider.proveFailure = IntegrityUnavailableException("Play Integrity non ha risposto (codice -8)")
        val client = guard(server, provider)

        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        awaitReports(server, 0)

        assertTrue(server.reports.isEmpty())
        assertFalse(AccountStatus.suspended.value)
    }
}
