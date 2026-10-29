package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.client.plugins.sse.SSE
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.auth.ITokenStore
import it.mensa.shared.net.installReachabilityTracking
import kotlinx.serialization.json.Json

expect class HttpClientFactory(tokenStore: ITokenStore) {
    fun create(): HttpClient
}

/**
 * La configurazione del client, uguale per OkHttp e Darwin. Sta qui e non nelle
 * due factory perche' i test la usino cosi' com'e': un client di test montato a
 * mano non si comportava come quello vero, ed e' cosi' che nessun test si e'
 * accorto di `expectSuccess` spento.
 *
 * `expectSuccess = true`: una risposta non 2xx diventa una `ResponseException`
 * con lo stato. Spento (il default di Ktor dalla 2.0), `body()` leggeva il JSON
 * d'errore come se fosse la risposta: un refresh_token revocato diventava una
 * sessione con il token vuoto, un `/api/cs/me` rifiutato un socio senza dati,
 * un 500 una lista vuota. E ogni `catch (e: ResponseException)` dell'auth non
 * scattava mai. Chi vuole leggere lo stato da se' lo dice sulla richiesta, con
 * `expectSuccess = false`.
 *
 * Gli interceptor di [installMensaInterceptors] (e quello di `IntegrityGuard`)
 * vedono comunque la risposta grezza: la validazione e' l'interceptor di
 * `HttpSend` piu' esterno, montato con il client, e si applica a quello che
 * loro restituiscono. Lo verifica `HttpClientConfigTest`.
 */
internal fun HttpClientConfig<*>.mensaDefaults() {
    expectSuccess = true
    install(SSE)
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
}

/** Gli interceptor che vanno montati sul client gia' costruito. */
internal fun HttpClient.installMensaInterceptors() {
    installAuthRetry()
    installReachabilityTracking()
}
