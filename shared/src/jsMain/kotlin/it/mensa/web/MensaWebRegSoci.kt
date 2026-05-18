@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.repository.RegSociRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebRegSoci internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: RegSociRepository get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (members: Array<MensaWebMember>) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.observeAll().collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return { job.cancel() }
    }

    fun refresh(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.refresh()
    }

    fun getById(id: String): Promise<MensaWebMember?> = scope.promise {
        sdk.awaitReady()
        repo.getById(id)?.toJs()
    }

    fun searchByName(query: String): Promise<Array<MensaWebMember>> = scope.promise {
        sdk.awaitReady()
        repo.searchByName(query).map { it.toJs() }.toTypedArray()
    }
}

/**
 * The PocketBase `members_registry` collection collapses everything into a
 * single `name` field (no first/last split) and a freeform `full_data` JSON
 * blob that contains the rest. We expose the most-frequently-used fields
 * (firstName/lastName/email/phone/region/sigs/localOffices) by pulling them
 * out of that blob — the iOS UI treats this collection the same way.
 *
 * Best-effort: missing keys silently default to empty strings / empty arrays.
 */
@JsExport
data class MensaWebMember(
    val id: String,
    val name: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val region: String,
    val city: String,
    val avatarUrl: String,
    val sigs: Array<String>,
    val localOffices: Array<String>,
    val fullProfileLink: String,
    /** Epoch ms; 0 when the backend didn't supply a birthdate. */
    val birthdateMs: Double,
    /** Free-form key/value entries from the PocketBase `full_data` JSON
     *  blob — Mobile renders these classified by keyword into Profile /
     *  Mensa / Contatti / SIG sections. Exposed as a flat array because
     *  `Map<String,String>` isn't `@JsExport`-friendly. */
    val fullData: Array<MensaWebMemberField>,
)

@JsExport
data class MensaWebMemberField(
    val key: String,
    val value: String,
)

internal fun RegSociModel.toJs(): MensaWebMember {
    val base = MensaSdk.apiBaseUrl()
    val avatar = if (image.isNotBlank()) "$base/api/files/members_registry/$id/$image" else ""
    val (first, last) = name.splitFirstLast()
    val email = fullData.stringOrEmpty("email")
    val phone = fullData.stringOrEmpty("phone").ifBlank { fullData.stringOrEmpty("telephone") }
    val sigs = fullData.stringArray("sigs")
    val offices = fullData.stringArray("local_offices")
    // Flatten the full_data JSON blob into [{key,value}] entries the web
    // component can iterate, mirroring iOS/Android's `extractFullData`.
    val extras = fullData.entries
        .mapNotNull { (k, v) ->
            val s = when (v) {
                is JsonPrimitive -> v.content
                else -> v.toString().trim('"')
            }
            if (s.isEmpty() || s == "null") null else MensaWebMemberField(k, s)
        }
        .sortedBy { it.key.lowercase() }
        .toTypedArray()
    return MensaWebMember(
        id = id,
        name = name,
        firstName = first,
        lastName = last,
        email = email,
        phone = phone,
        region = state,
        city = city,
        avatarUrl = avatar,
        sigs = sigs.toTypedArray(),
        localOffices = offices.toTypedArray(),
        fullProfileLink = fullProfileLink ?: "",
        birthdateMs = birthdate?.toEpochMilliseconds()?.toDouble() ?: 0.0,
        fullData = extras,
    )
}

private fun String.splitFirstLast(): Pair<String, String> {
    val parts = trim().split(' ', limit = 2)
    if (parts.size <= 1) return parts.firstOrNull().orEmpty() to ""
    return parts[0] to parts[1]
}

private fun JsonObject.stringOrEmpty(key: String): String {
    val el = this[key] ?: return ""
    return runCatching { (el as? JsonPrimitive)?.content }.getOrNull() ?: ""
}

private fun JsonObject.stringArray(key: String): List<String> {
    val el = this[key] ?: return emptyList()
    val arr = runCatching { el as? JsonArray }.getOrNull() ?: return emptyList()
    return arr.mapNotNull { (it as? JsonPrimitive)?.content }
}
