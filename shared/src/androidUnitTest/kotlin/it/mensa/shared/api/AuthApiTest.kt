package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import it.mensa.shared.api.endpoints.AuthApi
import it.mensa.shared.api.endpoints.InvalidCredentialsException
import it.mensa.shared.api.endpoints.LoginUnavailableException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class AuthApiTest {

    private fun api(status: HttpStatusCode, body: String) = AuthApi(
        HttpClient(MockEngine { respond(body, status, headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())) }) {
            mensaDefaults()
        },
    )

    /** Il JSON d'errore di PocketBase non deve diventare una risposta di token vuota. */
    @Test
    fun wrongPasswordIsInvalidCredentials() = runTest {
        val api = api(HttpStatusCode.Unauthorized, """{"status":401,"message":"Invalid credentials","data":{}}""")
        assertFailsWith<InvalidCredentialsException> { api.loginWithZitadel("socio@example.org", "sbagliata") }
    }

    /** Server giu' o troppi tentativi: un errore con lo stato, non un token vuoto. */
    @Test
    fun otherErrorsAreLoginUnavailable() = runTest {
        for (status in listOf(HttpStatusCode.TooManyRequests, HttpStatusCode.InternalServerError, HttpStatusCode.ServiceUnavailable)) {
            val api = api(status, """{"status":${status.value},"message":"Unable to connect to area32","data":{}}""")
            val e = assertFailsWith<LoginUnavailableException> { api.loginWithZitadel("socio@example.org", "giusta") }
            assertEquals(status.value, e.status)
            val expected = if (status == HttpStatusCode.TooManyRequests) "Troppi tentativi" else "(HTTP ${status.value})"
            assertTrue(e.message.orEmpty().contains(expected), e.message)
        }
    }

    @Test
    fun successStillDecodesTokens() = runTest {
        val api = api(HttpStatusCode.OK, """{"access_token":"a","refresh_token":"r","expires_in":3600}""")
        assertEquals("a", api.loginWithZitadel("socio@example.org", "giusta").access_token)
    }
}
