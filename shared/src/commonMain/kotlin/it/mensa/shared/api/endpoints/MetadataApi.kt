package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import kotlinx.serialization.Serializable

@Serializable
data class MetadataEntry(
    val id: String = "",
    val key: String = "",
    val value: String = "",
    val user: String = "",
)

@Serializable
private data class CreateMetadataPayload(
    val key: String,
    val value: String,
    val user: String,
)

@Serializable
private data class UpdateMetadataPayload(
    val key: String,
    val value: String,
)

/**
 * Wraps `users_metadata` (per-user key/value store). Uses JSON to the collection
 * API (not form-encoded — those are reserved for /api/cs/ and /api/payment/ endpoints).
 */
class MetadataApi(private val pb: PocketBaseClient) {

    suspend fun getMetadataMap(userId: String): Map<String, String> {
        val safe = userId.replace("'", "")
        val items = pb.fullList<MetadataEntry>(
            collection = "users_metadata",
            filter = "user = '$safe'",
        )
        return items.associate { it.key to it.value }
    }

    suspend fun getByKey(userId: String, key: String): MetadataEntry? {
        val sU = userId.replace("'", "")
        val sK = key.replace("'", "")
        return pb.list<MetadataEntry>(
            collection = "users_metadata",
            perPage = 1,
            filter = "user = '$sU' && key = '$sK'",
        ).items.firstOrNull()
    }

    suspend fun setMetadata(userId: String, key: String, value: String): MetadataEntry {
        val existing = getByKey(userId, key)
        return if (existing != null) {
            pb.update<MetadataEntry, UpdateMetadataPayload>(
                "users_metadata",
                existing.id,
                UpdateMetadataPayload(key = key, value = value),
            )
        } else {
            pb.create<MetadataEntry, CreateMetadataPayload>(
                "users_metadata",
                CreateMetadataPayload(key = key, value = value, user = userId),
            )
        }
    }
}
