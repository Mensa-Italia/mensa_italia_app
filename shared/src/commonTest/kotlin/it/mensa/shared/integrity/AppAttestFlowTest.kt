package it.mensa.shared.integrity

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.testTimeSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Il guard e il ciclo di vita della chiave insieme, con Apple e server finti:
 * i casi che hanno fatto male davvero (la tempesta di sfide senza nemmeno un
 * `attest`, la cascata di chiavi, la chiave stantia del telefono del titolare)
 * come test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppAttestFlowTest {

    private companion object {
        const val START = 1_700_000_000_000L
        const val STALE_POINTER = "integrity.appattest.key.user-1"
    }

    /** Un dispositivo iOS completo: Apple finto, container, Keychain vecchio, server `watched` e client con il guard. */
    private class Device(scope: TestScope) {
        val api = FakeAppAttestApi()
        val prefs = InMemorySettings()
        val keychain = InMemorySettings()
        val server = FakeServer { req -> if (!req.hasProof()) error(PreconditionRequired, "integrity_required") else null }
        private val wallClock = { START + scope.currentTime }

        fun lifecycle() = AppAttestLifecycle(api, KeyRecordStore(prefs), legacy = keychain, now = wallClock)

        /** Un processo dell'app: guard e ciclo di vita nuovi, stesso container. Il guard ha lo scope e l'orologio del test. */
        fun start(scope: TestScope, user: String = "user-1"): HttpClient {
            val client = server.client()
            IntegrityGuard(
                client, LifecycleBackedProvider(lifecycle()), { user },
                clock = scope.testTimeSource, scope = scope.realTimeScope(),
            ).install()
            return client
        }
    }

    private suspend fun Device.keyIdsSentToTheServer(): List<String> = server.requests
        .filter { it.url.encodedPath == FakeServer.ATTEST }
        .map { Json.parseToJsonElement(it.body.toByteArray().decodeToString()).jsonObject["key_id"]!!.jsonPrimitive.content }

    private suspend fun Device.reportBodies(): List<JsonObject> = server.reports
        .map { Json.parseToJsonElement(it.body.toByteArray().decodeToString()).jsonObject }

    // ── La tempesta di sfide senza un attest ─────────────────────────────────

    /** Regressione: 168 sfide e zero attest in una mattina. */
    @Test
    fun twentyConcurrentRequestsWithAFailingAttestKeyCostOneOfEverything() = runTest {
        val phone = Device(this)
        phone.api.attestErrors += appleError(2)
        val client = phone.start(this)

        val results = List(20) { i -> async { runCatching { client.get("/api/collections/events/records?page=$i") } } }.awaitAll()
        awaitReports(phone.server, 1)

        val errors = results.map { it.exceptionOrNull() }
        assertTrue(errors.all { it is IntegrityUnavailableException })
        assertEquals(setOf("attest_key/2"), errors.map { (it as IntegrityUnavailableException).failure?.tag }.toSet(), "venti errori identici")
        assertEquals(1, phone.api.generateKeyCalls)
        assertEquals(1, phone.api.attestKeyCalls)
        assertEquals(1, phone.server.challengeCalls)
        assertEquals(1, phone.server.reports.size)

        // Una seconda ondata prima della scadenza: zero chiamate ad Apple, zero sfide.
        val calls = phone.api.calls.toList()
        delay(10.seconds)
        List(20) { i -> async { runCatching { client.get("/api/collections/events/records?page=$i") } } }.awaitAll()
        awaitReports(phone.server, 1)

        assertEquals(calls, phone.api.calls)
        assertEquals(1, phone.server.challengeCalls)
        assertEquals(1, phone.server.reports.size)
    }

    @Test
    fun afterTheWindowADiscardedKeyIsReplacedAndTheSessionEnds() = runTest {
        val phone = Device(this)
        phone.api.attestErrors += appleError(2)
        val client = phone.start(this)
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }

        delay(30.seconds)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        // La chiave che Apple ha rifiutato non si riusa: ne nasce una nuova e il server vede solo quella.
        assertEquals(2, phone.api.generateKeyCalls)
        assertEquals(listOf("key-2"), phone.keyIdsSentToTheServer())
        assertEquals("key-2", phone.server.api.last().headers[IntegrityHeaders.KEY_ID])
    }

    @Test
    fun aTransientAttestErrorRetriesWithTheSameKeyAndTheServerSeesOne() = runTest {
        val phone = Device(this)
        phone.api.attestErrors += appleError(4)
        val client = phone.start(this)
        val e = assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals("attest_key/4", e.failure?.tag)

        delay(30.seconds)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        assertEquals(1, phone.api.generateKeyCalls)
        assertEquals(listOf("key-1"), phone.keyIdsSentToTheServer())
    }

    @Test
    fun anAppRestartIsALegitimateRetryPointAndResumesTheSamePendingKey() = runTest {
        val phone = Device(this)
        phone.api.attestErrors += appleError(4)
        val first = phone.start(this)
        assertFailsWith<IntegrityUnavailableException> { first.get("/api/cs/me") }

        // Il fallimento ricordato vive in RAM: all'avvio dopo nessuna attesa, e la chiave e' ancora la stessa.
        val second = phone.start(this)
        assertEquals(HttpStatusCode.OK, second.get("/api/cs/me").status)

        assertEquals(1, phone.api.generateKeyCalls)
        assertEquals(listOf("key-1"), phone.keyIdsSentToTheServer())
    }

    // ── La chiave stantia del telefono del titolare ──────────────────────────

    @Test
    fun aStaleKeyFromTheOldKeychainFailingWithAnyCodeIsRotatedOnceAndReportedWithTheRealError() = runTest {
        for (code in listOf(0, 2, 3)) {
            val phone = Device(this)
            phone.keychain.putString(STALE_POINTER, "stale")
            phone.api.brokenKeys["stale"] = appleError(code, underlying = "NSOSStatusErrorDomain/-1: Invalid appUUID")
            val client = phone.start(this)

            assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status, "code $code")
            awaitReports(phone.server, 1)

            // Una chiave nuova, registrata una volta, e la richiesta e' passata con lei.
            assertEquals(1, phone.api.generateKeyCalls, "code $code")
            assertEquals(listOf("key-1"), phone.keyIdsSentToTheServer())
            assertEquals("key-1", phone.server.api.last().headers[IntegrityHeaders.KEY_ID])
            assertNull(phone.keychain.getStringOrNull(STALE_POINTER), "code $code: il vecchio puntatore e' sparito")
            // E il server ha visto il DCError REALE della chiave vecchia, pur essendo andato tutto bene.
            val report = phone.reportBodies().single()
            assertEquals("rotated", report["outcome"]!!.jsonPrimitive.content)
            assertEquals("assertion", report["stage"]!!.jsonPrimitive.content)
            assertEquals(code, report["code"]!!.jsonPrimitive.content.toInt())
            assertEquals(DC_DOMAIN, report["domain"]!!.jsonPrimitive.content)
            assertEquals("NSOSStatusErrorDomain/-1: Invalid appUUID", report["underlying"]!!.jsonPrimitive.content)
            assertEquals("discard_key", report["disposition"]!!.jsonPrimitive.content)
            assertEquals("ready", report["key_phase"]!!.jsonPrimitive.content)
            assertEquals(-1, report["key_age_s"]!!.jsonPrimitive.content.toInt())
        }
    }

    @Test
    fun aGoodKeyFromTheOldKeychainIsAdoptedWithoutRegisteringAnything() = runTest {
        val phone = Device(this)
        phone.keychain.putString(STALE_POINTER, "good-key")
        val client = phone.start(this)

        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        assertEquals(emptyList<String>(), phone.api.calls.filter { it.startsWith("generateKey") || it.startsWith("attestKey") })
        assertTrue(phone.keyIdsSentToTheServer().isEmpty(), "nessun attest nuovo")
        assertEquals("good-key", phone.server.api.last().headers[IntegrityHeaders.KEY_ID])
    }

    // ── Niente cascata ───────────────────────────────────────────────────────

    /**
     * Una chiave nata da poco che non sa firmare e' un guasto del dispositivo: per
     * un'ora non si ruota mai (18 chiavi in tre ore sono quello che e' successo), poi si'.
     */
    @Test
    fun aYoungKeyThatCannotSignNeverCausesACascadeOfKeys() = runTest {
        val phone = Device(this)
        val client = phone.start(this)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        assertEquals(1, phone.api.generateKeyCalls)

        phone.api.brokenKeys["key-1"] = appleError(2)
        repeat(5) { // 55 minuti, un tentativo ogni 11 (piu' del tetto del backoff)
            delay(11.minutes)
            assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
            assertEquals(1, phone.api.generateKeyCalls, "ancora la chiave di prima")
        }

        // Passata l'ora la chiave e' stantia davvero: una rotazione, e il guasto sparisce.
        delay(11.minutes)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        assertEquals(2, phone.api.generateKeyCalls)
        assertEquals("key-2", phone.server.api.last().headers[IntegrityHeaders.KEY_ID])
    }

    // ── Timeout ──────────────────────────────────────────────────────────────

    @Test
    fun anAttestKeyThatNeverAnswersFailsAt30sAndTheQueueFailsRightAfterWithoutAnother30s() = runTest {
        val phone = Device(this)
        phone.api.onAttestKey = { awaitCancellation() }
        val client = phone.start(this)

        val t0 = currentTime
        val results = List(5) { i -> async { runCatching { client.get("/api/collections/events/records?page=$i") } } }.awaitAll()
        val elapsed = currentTime - t0

        // 30 s di timeout (piu' i pochi ms del finto Apple), non 5 x 30 s.
        assertTrue(elapsed in 30_000..30_100, "trascorsi $elapsed ms")
        val tags = results.map { (it.exceptionOrNull() as IntegrityUnavailableException).failure?.tag }
        assertEquals(List(5) { "attest_key/-2" }, tags)
        assertEquals(1, phone.api.attestKeyCalls)

        // E il report dice quanto c'e' voluto davvero: e' la differenza fra i 0,15 s di
        // un rifiuto immediato e i 62 s di un'attesa che non finisce mai.
        awaitReports(phone.server, 1)
        val report = phone.reportBodies().single()
        assertEquals("attest_key", report["stage"]!!.jsonPrimitive.content)
        assertEquals("it.mensa.timeout", report["domain"]!!.jsonPrimitive.content)
        assertEquals(-2, report["code"]!!.jsonPrimitive.content.toInt())
        val reportedMs = report["elapsed_ms"]!!.jsonPrimitive.content.toLong()
        assertTrue(reportedMs in 30_000..30_100, "elapsed_ms $reportedMs")

        // Riprova a 30 s con la STESSA chiave, e questa volta Apple risponde.
        phone.api.onAttestKey = null
        delay(30.seconds)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        assertEquals(1, phone.api.generateKeyCalls)
        assertEquals(listOf("key-1"), phone.keyIdsSentToTheServer())
    }

    // ── Invarianti ───────────────────────────────────────────────────────────

    @Test
    fun appleIsNeverCalledConcurrently() = runTest {
        val phone = Device(this)
        val client = phone.start(this)
        client.get("/api/cs/me")

        val responses = List(10) { i -> async { client.get("/api/collections/events/records?page=$i") } }.awaitAll()

        assertTrue(responses.all { it.status == HttpStatusCode.OK })
        assertEquals(1, phone.api.maxConcurrentCalls)
    }

    @Test
    fun anUnsupportedDeviceNeverCallsAppleNorAsksForAChallenge() = runTest {
        val phone = Device(this)
        phone.api.supported = false
        val client = phone.start(this)

        assertFailsWith<IntegrityUnsupportedException> { client.get("/api/cs/me") }
        delay(2.hours)
        assertFailsWith<IntegrityUnsupportedException> { client.get("/api/cs/me") }

        assertTrue(phone.api.calls.isEmpty())
        assertEquals(0, phone.server.challengeCalls)
    }

    @Test
    fun featureUnsupportedFromAppleIsRememberedForTheWholeProcess() = runTest {
        val phone = Device(this)
        phone.api.generateKeyErrors += appleError(1)
        val client = phone.start(this)
        val e = assertFailsWith<IntegrityUnsupportedException> { client.get("/api/cs/me") }
        assertEquals("generate_key/1", e.failure?.tag)
        val calls = phone.api.calls.size

        delay(2.hours)
        assertFailsWith<IntegrityUnsupportedException> { client.get("/api/cs/me") }

        assertEquals(calls, phone.api.calls.size, "nessuna nuova chiamata, nemmeno dopo ore")
        assertEquals(0, phone.server.challengeCalls, "Apple ha rifiutato prima ancora della sfida")
    }

    /**
     * Il server che rifiuta l'attestazione risponde 403 `integrity_failed`
     * (`iosAttestHandler` -> `reject`). Non e' un verdetto su una richiesta
     * dell'utente: e' la registrazione che non va, e senza memoria ogni richiesta
     * conierebbe una chiave nuova (la cascata da 18 chiavi in tre ore).
     */
    @Test
    fun aRegistrationTheServerRefusesConsumesTheKeyAndIsRemembered() = runTest {
        val phone = Device(this)
        phone.server.attestStatus = HttpStatusCode.Forbidden
        phone.server.attestBody = """{"status":403,"message":"no","code":"integrity_failed"}"""
        val client = phone.start(this)

        val results = List(20) { i -> async { runCatching { client.get("/api/collections/events/records?page=$i") } } }.awaitAll()
        awaitReports(phone.server, 1)

        val errors = results.map { it.exceptionOrNull() }
        assertTrue(errors.all { it is IntegrityUnavailableException }, "una causa sola, non venti verdetti")
        assertEquals(setOf("register/403"), errors.map { (it as IntegrityUnavailableException).failure?.tag }.toSet())
        assertEquals(1, phone.api.generateKeyCalls, "una sola chiave per venti richieste")
        assertEquals(1, phone.api.attestKeyCalls)
        assertEquals(1, phone.server.challengeCalls)
        assertEquals(listOf("key-1"), phone.keyIdsSentToTheServer())
        // L'attestazione e' consumata: il record e' sparito.
        assertNull(KeyRecordStore(phone.prefs).get("user-1"))
        val report = phone.reportBodies().single()
        assertEquals("register", report["stage"]?.jsonPrimitive?.content)
        assertEquals("403", report["code"]?.jsonPrimitive?.content)
        assertEquals("pending", report["key_phase"]?.jsonPrimitive?.content)

        // Una seconda ondata prima della scadenza: nessuna chiave, nessuna sfida.
        delay(10.seconds)
        assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }
        assertEquals(1, phone.api.generateKeyCalls)
        assertEquals(1, phone.server.challengeCalls)

        // Passato il backoff si riprova, con una chiave nuova.
        delay(30.seconds)
        phone.server.attestStatus = HttpStatusCode.OK
        phone.server.attestBody = """{"ok":true}"""
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)

        assertEquals(2, phone.api.generateKeyCalls)
        assertEquals("key-2", phone.server.api.last().headers[IntegrityHeaders.KEY_ID])
    }

    @Test
    fun aServerErrorOnTheRegistrationIsAFailureWithItsOwnTag() = runTest {
        val phone = Device(this)
        phone.server.attestStatus = HttpStatusCode.ServiceUnavailable
        val client = phone.start(this)

        val e = assertFailsWith<IntegrityUnavailableException> { client.get("/api/cs/me") }

        assertEquals("register/503", e.failure?.tag)
        assertEquals("pending", e.failure?.keyPhase)
        // L'attestazione era per una chiave che il server non ha accettato: consumata, ne servira' un'altra.
        phone.server.attestStatus = HttpStatusCode.OK
        delay(30.seconds)
        assertEquals(HttpStatusCode.OK, client.get("/api/cs/me").status)
        assertEquals(2, phone.api.generateKeyCalls)
    }
}
