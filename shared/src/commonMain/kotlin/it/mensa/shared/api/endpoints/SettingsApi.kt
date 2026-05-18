package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.CalendarLinkModel
import kotlinx.serialization.Serializable

@Serializable
data class UserMetadata(
    val id: String = "",
    val key: String = "",
    val value: String = "",
    val user: String = "",
)

@Serializable
data class CreateMetadataBody(
    val key: String,
    val value: String,
    val user: String,
)

@Serializable
data class UpdateMetadataBody(
    val key: String,
    val value: String,
)

class SettingsApi(
    private val pb: PocketBaseClient,
    private val client: HttpClient,
) {
    /** Returns all config entries as key→value map (collection: configs). */
    suspend fun settings(): Map<String, String> {
        val items = pb.fullList<ConfigEntry>("configs")
        return items.associate { it.key to it.value }
    }

    // Memoized configs — configs change rarely, reuse within the session.
    private var _cachedConfigs: Map<String, String>? = null

    /**
     * Returns configs map (collection: configs). Memoized: fetched once per session
     * then returned from cache. Used by TranslationLoader.
     */
    suspend fun configs(): Map<String, String> {
        _cachedConfigs?.let { return it }
        val fresh = settings()
        _cachedConfigs = fresh
        return fresh
    }

    /** Returns all user_metadata entries as key→value map. */
    suspend fun getMetadata(): Map<String, String> {
        val items = pb.fullList<UserMetadata>("users_metadata")
        return items.associate { it.key to it.value }
    }

    /** Gets a single metadata entry by key. Returns null if not found. */
    suspend fun getMetadataByKey(key: String): UserMetadata? {
        val resp = pb.list<UserMetadata>(
            "users_metadata",
            filter = "key='$key'",
            perPage = 1
        )
        return resp.items.firstOrNull()
    }

    /** Creates or updates a user_metadata entry. */
    suspend fun setMetadata(userId: String, key: String, value: String) {
        val existing = getMetadataByKey(key)
        if (existing != null) {
            pb.update<UserMetadata, UpdateMetadataBody>(
                "users_metadata",
                existing.id,
                UpdateMetadataBody(key = key, value = value)
            )
        } else {
            pb.create<UserMetadata, CreateMetadataBody>(
                "users_metadata",
                CreateMetadataBody(key = key, value = value, user = userId)
            )
        }
    }

    /** Fetches the calendar link from PocketBase. */
    suspend fun getCalendarLink(): CalendarLinkModel {
        val resp = pb.list<CalendarLinkModel>("calendar_link", perPage = 1)
        return resp.items.first()
    }
}

@Serializable
data class ConfigEntry(
    val id: String = "",
    val key: String = "",
    val value: String = "",
)
