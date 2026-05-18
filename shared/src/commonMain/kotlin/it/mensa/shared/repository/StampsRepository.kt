package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.endpoints.StampsApi
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.db.StampUser
import it.mensa.shared.model.StampModel
import it.mensa.shared.model.StampUserExpand
import it.mensa.shared.model.StampUserModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

class StampsRepository(
    private val api: StampsApi,
    private val db: MensaDatabase,
    private val json: Json,
    private val auth: AuthRepository,
) {
    /** Flow of the user's collected stamps — what the Tableport screen shows. */
    fun observeAll(): Flow<List<StampUserModel>> =
        db.stampUserQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    /** Refresh the user's `stamp_users` records (expand=stamp). */
    suspend fun refresh(filter: String? = null, @Suppress("UNUSED_PARAMETER") sort: String = "-created") {
        val userId = auth.currentUser.value?.id
        val effectiveFilter = when {
            !filter.isNullOrBlank() -> filter
            !userId.isNullOrBlank() -> "user='$userId'"
            else -> null
        }
        val items = api.getUserStamps(filter = effectiveFilter)
        db.transaction {
            db.stampUserQueries.deleteAll()
            // Also mirror the catalog so detail screens can look stamps up offline.
            db.stampQueries.deleteAll()
            items.forEach { su ->
                val stamp = su.stampRecord
                if (stamp != null && stamp.id.isNotEmpty()) {
                    db.stampQueries.insertOrReplace(
                        id = stamp.id,
                        description = stamp.description,
                        image = stamp.image,
                        createdAt = stamp.created.toEpochMilliseconds(),
                        updatedAt = Clock.System.now().toEpochMilliseconds(),
                    )
                }
                db.stampUserQueries.insertOrReplace(
                    id = su.id,
                    userId = su.user,
                    stampId = su.stampId,
                    acquiredAt = su.created.toEpochMilliseconds(),
                    expandedStampJson = stamp?.let {
                        json.encodeToString(StampModel.serializer(), it)
                    } ?: "{}",
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
    }

    suspend fun getById(id: String): StampUserModel? =
        db.stampUserQueries.selectById(id).awaitAsOneOrNull()?.toModel()

    /** Lookup a catalog stamp by id from the local mirror. */
    suspend fun getStampById(id: String): StampModel? =
        db.stampQueries.selectById(id).awaitAsOneOrNull()?.toModel()

    /** Verify a QR scan: fetch the catalog stamp by id+code from the server. */
    suspend fun verify(id: String, code: String): StampModel? =
        api.getStamp(id = id, code = code)

    /** Claim a stamp for the current user (after a successful QR verify). */
    suspend fun claim(stampId: String, code: String) {
        val userId = auth.currentUser.value?.id ?: error("Not authenticated")
        api.addStamp(stampId = stampId, code = code, userId = userId)
        refresh()
    }

    private fun StampUser.toModel(): StampUserModel {
        val stamp = runCatching {
            if (expandedStampJson.isNotEmpty() && expandedStampJson != "{}") {
                json.decodeFromString(StampModel.serializer(), expandedStampJson)
            } else null
        }.getOrNull()
        return StampUserModel(
            id = id,
            created = Instant.fromEpochMilliseconds(acquiredAt),
            updated = Instant.fromEpochMilliseconds(updatedAt),
            stampId = stampId,
            user = userId,
            expand = stamp?.let { StampUserExpand(stamp = it) },
        )
    }
}
