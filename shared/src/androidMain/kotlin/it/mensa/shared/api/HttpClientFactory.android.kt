package it.mensa.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import it.mensa.shared.auth.ITokenStore

actual class HttpClientFactory actual constructor(@Suppress("unused") private val tokenStore: ITokenStore) {
    actual fun create(): HttpClient = HttpClient(OkHttp) { mensaDefaults() }.apply { installMensaInterceptors() }
}
