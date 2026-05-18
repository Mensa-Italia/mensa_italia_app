package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.AddonModel
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Query-parameter pairs to append to the addon's base URL when opening
 * its webview. Order is stable to ease debugging on the server.
 *
 * The server returns an arbitrary JSON object (typically `{payload, signature, addon}`,
 * but flexible) — Flutter spreads it as `Uri.replace(queryParameters: ...)`,
 * so we mirror that contract here.
 */
data class AddonAccessData(
    /**
     * Ordered key-value pairs to append as query parameters. Exposed as a
     * `Map<String, String>` (which bridges to `NSDictionary` in Swift) — the
     * server contract uses unique field names so map semantics are safe.
     */
    val params: Map<String, String>,
)

class AddonsApi(
    private val pb: PocketBaseClient,
    private val client: HttpClient,
) {
    suspend fun list(): List<AddonModel> =
        pb.fullList("addons", sort = "name")

    suspend fun get(id: String): AddonModel =
        pb.getOne("addons", id)

    /**
     * Calls the custom endpoint GET /api/cs/sign-payload/{addonId}.
     * Returns the signed payload as a list of (name, value) query-param pairs.
     */
    suspend fun getAccessData(addonId: String): AddonAccessData {
        val body: JsonObject = client.get("/api/cs/sign-payload/$addonId").body()
        val params = linkedMapOf<String, String>()
        for ((k, v) in body.entries) {
            params[k] = jsonElementToString(v)
        }
        return AddonAccessData(params = params)
    }

    private fun jsonElementToString(el: JsonElement): String = when (el) {
        is JsonPrimitive -> el.contentOrNull ?: el.toString()
        else -> el.toString()
    }
}
