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
 * Browser actual for [HttpClientFactory], built on Ktor's `Js` engine
 * (browser `fetch` for HTTP, `WebSocket` for WS, `EventSource` for SSE).
 *
 * Auth strategy: reads the token from [AuthHolder] (in-memory, kept in sync
 * by [it.mensa.shared.auth.AuthRepository] on init/login/logout). This mirrors
 * the iOS Darwin implementation because `runBlocking { tokenStore.read() }`
 * is unavailable on wasmJs (no real threads to block).
 *
 * Realtime / SSE caveat: the `SSE` plugin is intentionally NOT installed here
 * because Ktor's wasmJs `Js` engine implements SSE on top of the browser
 * `EventSource`, which CANNOT attach custom `Authorization` headers — so the
 * PocketBase `/api/realtime` GET handshake would go out anonymously. The
 * existing `RealtimeClient` (commonMain) depends on `client.serverSentEventsSession`,
 * which currently is not supported on the `Js` engine for wasmJs as of Ktor
 * 3.1.x. The web build should either:
 *   (a) use polling instead of SSE for the three realtime-subscribing repos
 *       (notifications, payments, tickets), or
 *   (b) connect via a separate `EventSource(...)` directly in a wasmJs-only
 *       wrapper that injects the token as a URL query parameter (requires
 *       backend changes to accept it).
 * This is a follow-up; the rest of the HTTP surface works without SSE.
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
