package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.plugin
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.mensa.shared.net.HttpErrorText
import it.mensa.shared.net.HttpErrors
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Il contratto del client di produzione ([mensaDefaults]): una risposta non 2xx
 * e' un'eccezione, e gli interceptor montati dopo vedono lo stesso la risposta
 * grezza. Se la seconda cosa smettesse di valere, il retry sul 401 e
 * `IntegrityGuard` riceverebbero un'eccezione al posto del 401 o del 428 che
 * devono gestire.
 */
class HttpClientConfigTest {

    @Serializable
    private data class Payload(val value: String = "")

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

    private fun client(handler: MockRequestHandler) = HttpClient(MockEngine(handler)) { mensaDefaults() }

    private fun answering(status: HttpStatusCode, body: String = """{"value":"ok"}""") =
        client { respond(body, status, jsonHeaders) }

    /** Il JSON d'errore non diventa piu' un oggetto con i campi vuoti. */
    @Test
    fun errorStatusesThrow() = runTest {
        val notFound = assertFailsWith<ClientRequestException> {
            answering(HttpStatusCode.NotFound, """{"status":404,"message":"Not found","data":{}}""").get("/x").body<Payload>()
        }
        assertEquals(HttpStatusCode.NotFound, notFound.response.status)

        val down = assertFailsWith<ServerResponseException> {
            answering(HttpStatusCode.ServiceUnavailable).get("/x").body<Payload>()
        }
        assertEquals(HttpStatusCode.ServiceUnavailable, down.response.status)
    }

    /** Anche una chiamata che non legge il body (delete, finish) vede l'errore. */
    @Test
    fun errorStatusThrowsWithoutReadingTheBody() = runTest {
        assertFailsWith<ClientRequestException> { answering(HttpStatusCode.Forbidden).get("/x") }
    }

    /**
     * I file passano da un 307 verso il link firmato su S3: il redirect va
     * seguito prima che la validazione veda il 3xx.
     */
    @Test
    fun redirectsAreFollowedBeforeValidation() = runTest {
        val http = client { req ->
            if (req.url.host == "s3.example.org") respond("""{"value":"file"}""", HttpStatusCode.OK, jsonHeaders)
            else respond("", HttpStatusCode.TemporaryRedirect, headersOf(HttpHeaders.Location, "https://s3.example.org/f"))
        }

        assertEquals("file", http.get("/api/files/x/y/z.jpg").body<Payload>().value)
    }

    @Test
    fun aRequestCanReadTheStatusItself() = runTest {
        val response = answering(HttpStatusCode.Unauthorized).get("/x") { expectSuccess = false }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    /**
     * Come `installAuthRetry`: un interceptor montato dopo il client vede il 401,
     * rifa la richiesta, e chi chiama riceve il 200 del secondo giro, senza
     * eccezione per il primo.
     */
    @Test
    fun laterInterceptorsSeeTheRawResponse() = runTest {
        val http = client { req ->
            if (req.headers["X-Retry"] == "1") respond("""{"value":"retried"}""", HttpStatusCode.OK, jsonHeaders)
            else respond("""{"status":401}""", HttpStatusCode.Unauthorized, jsonHeaders)
        }
        var seen: HttpStatusCode? = null
        http.plugin(HttpSend).intercept { request ->
            val call = execute(request)
            seen = call.response.status
            if (call.response.status != HttpStatusCode.Unauthorized) return@intercept call
            request.header("X-Retry", "1")
            execute(request)
        }

        assertEquals("retried", http.get("/x").body<Payload>().value)
        assertEquals(HttpStatusCode.Unauthorized, seen)
    }

    /** Se anche il secondo giro fallisce, l'errore arriva a chi chiama. */
    @Test
    fun theFinalResponseIsValidated() = runTest {
        val http = answering(HttpStatusCode.Unauthorized)
        http.plugin(HttpSend).intercept { request -> execute(request).let { execute(request) } }

        val e = assertFailsWith<ClientRequestException> { http.get("/x") }
        assertEquals(HttpStatusCode.Unauthorized, e.response.status)
    }

    /** Il testo per la UI: per l'errore HTTP grezzo, non per chi l'ha gia' tradotto. */
    @Test
    fun errorTextIsOnlyForUntranslatedErrors() = runTest {
        val raw = assertFailsWith<ServerResponseException> { answering(HttpStatusCode.BadGateway).get("/x") }

        assertEquals(HttpErrorText(502, "app.error.http.server", HttpErrors.textFor(502).fallback), HttpErrors.text(raw))
        assertEquals(null, HttpErrors.text(IllegalStateException("wrapped", raw)))
    }

    /** Un interceptor che lancia la sua eccezione tipizzata (IntegrityGuard) non viene coperto. */
    @Test
    fun typedExceptionsFromInterceptorsPassThrough() = runTest {
        class Typed : Exception()
        val http = answering(HttpStatusCode.Forbidden)
        http.plugin(HttpSend).intercept { request ->
            val call = execute(request)
            if (call.response.status == HttpStatusCode.Forbidden) throw Typed()
            call
        }

        assertFailsWith<Typed> { http.get("/x") }
    }
}
