package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.auth.ITokenStore
import kotlinx.serialization.json.Json

/**
 * Browser actual for [HttpClientFactory] on the `js(IR)` target — built on
 * Ktor's `Js` engine (browser `fetch` + `WebSocket` + `EventSource`).
 *
 * Auth strategy mirrors the iOS Darwin / wasmJs actuals: the token is read
 * synchronously from [AuthHolder] (in-memory, kept in sync by
 * [it.mensa.shared.auth.AuthRepository] on init/login/logout). Suspend reads
 * from [it.mensa.shared.auth.TokenStore] inside `defaultRequest` are not
 * an option on the browser engine where `runBlocking` is unavailable.
 *
 * Realtime / SSE caveat: same as wasmJs — Ktor's `Js` engine cannot attach
 * custom `Authorization` headers to `EventSource`, so the PocketBase realtime
 * handshake would be anonymous. `RealtimeClient`-driven repos (notifications,
 * payments, tickets) should either poll or be re-implemented with a separate
 * `EventSource(...)` wrapper that passes the token via a query param.
 */
actual class HttpClientFactory actual constructor(
    @Suppress("unused") private val tokenStore: ITokenStore,
) {
    actual fun create(): HttpClient = HttpClient(Js) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(Logging) {
            level = LogLevel.HEADERS
            logger = Logger.SIMPLE
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 30_000
            socketTimeoutMillis = 30_000
        }
        install(AuthPlugin)
        defaultRequest {
            url(ApiConfig.BASE_URL)
            contentType(ContentType.Application.Json)
        }
    }.apply { installAuthRetry() }
}
