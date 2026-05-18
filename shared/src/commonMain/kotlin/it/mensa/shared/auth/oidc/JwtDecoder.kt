package it.mensa.shared.auth.oidc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Minimal JWT payload decoder. The server is trusted: we only need the claims
 * (iss, client_id, exp) to bootstrap OIDC discovery and schedule refresh.
 * Signature validation is not performed here — the access token is validated
 * server-side on every protected request.
 */
@OptIn(ExperimentalEncodingApi::class)
object JwtDecoder {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val base64Url = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)

    fun payload(jwt: String): JsonObject {
        val parts = jwt.split('.')
        require(parts.size >= 2) { "Malformed JWT: expected at least 2 segments, got ${parts.size}" }
        val bytes = base64Url.decode(parts[1])
        val element = json.parseToJsonElement(bytes.decodeToString())
        return element as JsonObject
    }

    fun string(payload: JsonObject, key: String): String? =
        runCatching { payload[key]?.jsonPrimitive?.content }.getOrNull()

    fun long(payload: JsonObject, key: String): Long? =
        string(payload, key)?.toLongOrNull()
}
