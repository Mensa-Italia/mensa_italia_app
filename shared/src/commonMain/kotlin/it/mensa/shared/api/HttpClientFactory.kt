package it.mensa.shared.api

import io.ktor.client.HttpClient
import it.mensa.shared.auth.ITokenStore

expect class HttpClientFactory(tokenStore: ITokenStore) {
    fun create(): HttpClient
}
