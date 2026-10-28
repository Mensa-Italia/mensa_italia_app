package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.api.endpoints.ExAppsApi
import it.mensa.shared.api.endpoints.ExportRequestGoneException
import it.mensa.shared.integrity.IntegrityGuard
import it.mensa.shared.integrity.IntegrityHeaders
import it.mensa.shared.integrity.PreconditionRequired
import it.mensa.shared.integrity.UnsupportedIntegrityProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExAppsApiTest {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    private val callbackUrl = "https://bot.example.org/telegram/callback"

    private class Recorder {
        val requests = mutableListOf<HttpRequestData>()
        suspend fun bodyOf(index: Int) = requests[index].body.toByteArray().decodeToString()
    }

    private fun api(
        recorder: Recorder,
        decideStatus: HttpStatusCode = HttpStatusCode.OK,
        callbackStatus: HttpStatusCode = HttpStatusCode.OK,
        guard: Boolean = false,
    ): ExAppsApi {
        val client = HttpClient(MockEngine { request ->
            recorder.requests += request
            when {
                request.url.encodedPath == "/api/cs/exapp/export/decide" ->
                    respond("""{"status":"approved"}""", decideStatus, jsonHeaders)
                request.url.host == "bot.example.org" ->
                    respond("ok", callbackStatus)
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest {
                url(ApiConfig.BASE_URL)
                contentType(ContentType.Application.Json)
            }
        }
        if (guard) IntegrityGuard(client, UnsupportedIntegrityProvider(), { "user-1" }).install()
        return ExAppsApi(PocketBaseClient(client), client)
    }

    // ── decide ───────────────────────────────────────────────────────────────

    @Test
    fun decidePostsTheRequestIdAndTheAnswer() = runTest {
        val rec = Recorder()
        api(rec).decideExport("req1", accepted = true)

        val sent = rec.requests.single()
        assertEquals("POST", sent.method.value)
        assertEquals("svc.mensa.it", sent.url.host)
        assertEquals("/api/cs/exapp/export/decide", sent.url.encodedPath)
        assertEquals("""{"request_id":"req1","accepted":true}""", rec.bodyOf(0))
    }

    @Test
    fun denyIsTheSameCallWithFalse() = runTest {
        val rec = Recorder()
        api(rec).decideExport("req1", accepted = false)
        assertEquals("""{"request_id":"req1","accepted":false}""", rec.bodyOf(0))
    }

    @Test
    fun expiredOrAlreadyDecidedIsATypedError() = runTest {
        for (status in listOf(HttpStatusCode.NotFound, HttpStatusCode.Conflict)) {
            val rec = Recorder()
            val e = assertFailsWith<ExportRequestGoneException> {
                api(rec, decideStatus = status).decideExport("req1", true)
            }
            assertEquals(status.value, e.status)
        }
    }

    @Test
    fun otherDecideFailuresAreNotMistakenForSuccess() = runTest {
        assertFailsWith<IllegalStateException> {
            api(Recorder(), decideStatus = HttpStatusCode.InternalServerError).decideExport("req1", true)
        }
    }

    // ── risposta completa: decisione, poi callback ───────────────────────────

    @Test
    fun answerDecidesFirstThenNotifiesTheThirdPartyWithTheRequestId() = runTest {
        val rec = Recorder()

        val delivered = api(rec).answerDataExport("req1", callbackUrl, accepted = true)

        assertTrue(delivered)
        assertEquals(2, rec.requests.size)
        assertEquals("/api/cs/exapp/export/decide", rec.requests[0].url.encodedPath)
        assertEquals(callbackUrl, rec.requests[1].url.toString())
        assertEquals("""{"accepted":true,"request_id":"req1"}""", rec.bodyOf(1))
    }

    @Test
    fun denyNotifiesWithFalse() = runTest {
        val rec = Recorder()
        api(rec).answerDataExport("req1", callbackUrl, accepted = false)
        assertEquals("""{"accepted":false,"request_id":"req1"}""", rec.bodyOf(1))
    }

    @Test
    fun noCallbackWhenTheServerRefusedTheDecision() = runTest {
        val rec = Recorder()

        assertFailsWith<ExportRequestGoneException> {
            api(rec, decideStatus = HttpStatusCode.Conflict).answerDataExport("req1", callbackUrl, true)
        }

        assertEquals(1, rec.requests.size, "l'app esterna non va avvisata di una decisione mai registrata")
    }

    @Test
    fun aDeadThirdPartyDoesNotUndoTheDecision() = runTest {
        val client = HttpClient(MockEngine { request ->
            if (request.url.host == "bot.example.org") throw RuntimeException("connection refused")
            respond("""{"status":"approved"}""", HttpStatusCode.OK, jsonHeaders)
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest { url(ApiConfig.BASE_URL) }
        }

        val delivered = ExAppsApi(PocketBaseClient(client), client).answerDataExport("req1", callbackUrl, true)

        assertFalse(delivered)
    }

    // ── callback: solo il corpo, niente di nostro ────────────────────────────

    @Test
    fun identityCallbackBodyIsUnchanged() = runTest {
        val rec = Recorder()
        api(rec).postCallback(callbackUrl, true)
        assertEquals("""{"accepted":true}""", rec.bodyOf(0))
    }

    /**
     * Il callback va a un host di un terzo: niente `Authorization`, niente
     * header di integrita', niente sfida, neanche se il terzo risponde 428 e
     * l'utente e' in modalita' proattiva.
     */
    @Test
    fun thirdPartyCallbackNeverGetsOurHeadersOrAChallenge() = runTest {
        val rec = Recorder()
        val client = HttpClient(MockEngine { request ->
            rec.requests += request
            when (request.url.host) {
                "bot.example.org" -> respond("""{"code":"integrity_required"}""", PreconditionRequired, jsonHeaders)
                else -> respond("{}", HttpStatusCode.OK, jsonHeaders)
            }
        }) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest { url(ApiConfig.BASE_URL) }
        }
        IntegrityGuard(client, UnsupportedIntegrityProvider(), { "user-1" }).install()

        ExAppsApi(PocketBaseClient(client), client).postCallback(callbackUrl, true, "req1")

        val sent = rec.requests.single()
        assertEquals("bot.example.org", sent.url.host)
        assertNull(sent.headers[HttpHeaders.Authorization])
        IntegrityHeaders.ALL.forEach { assertNull(sent.headers[it], it) }
        assertEquals(1, rec.requests.size, "nessuna sfida, nessun retry")
    }

    @Test
    fun guardInstalledDoesNotChangeTheDecideCall() = runTest {
        val rec = Recorder()
        api(rec, guard = true).decideExport("req1", true)
        assertEquals(1, rec.requests.size)
    }
}
