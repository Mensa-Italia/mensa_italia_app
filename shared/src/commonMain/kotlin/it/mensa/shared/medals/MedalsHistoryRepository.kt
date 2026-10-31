package it.mensa.shared.medals

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.MedalHistoryModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The signed-in member's seasonal medals in the seasons already closed, read
 * from `medals_history` (own rows only). In memory, tied to the user it was
 * read for; the rows are also kept in SQLite (KeyValue, `medals.history.<userId>`),
 * so after a cold start the annate show at once and the network only brings
 * them up to date.
 */
class MedalsHistoryRepository(
    private val pb: PocketBaseClient,
    private val auth: AuthRepository,
    private val db: MensaDatabase,
    private val json: Json,
) {
    private val cache = MutableStateFlow<Pair<String, List<MedalHistoryModel>>?>(null)
    private val serializer = ListSerializer(MedalHistoryModel.serializer())

    /** The past seasons, once read for the current user; empty until then. */
    fun observe(): Flow<MedalHistory> =
        combine(cache, auth.currentUser) { cached, user ->
            val meId = if (MedalFixtures.enabled) MedalFixtures.ME_ID else user?.id
            if (cached != null && meId != null && cached.first == meId) MedalHistory.build(cached.second) else MedalHistory.EMPTY
        }

    /** Rereads the history. Throws on network errors; the last read stays. */
    @Throws(Exception::class)
    suspend fun refresh() {
        if (MedalFixtures.enabled) {
            cache.value = MedalFixtures.ME_ID to MedalFixtures.history()
            return
        }
        val userId = auth.currentUser.value?.id ?: return
        primeFromDisk(userId)
        val rows: List<MedalHistoryModel> = pb.fullList(COLLECTION, filter = "user = \"${userId.replace("\"", "")}\"", sort = "-season,medal")
        cache.value = userId to rows
        runCatching { db.keyValueQueries.insertOrReplace(key = cacheKey(userId), value_ = json.encodeToString(serializer, rows)) }
    }

    /** Publishes the rows saved for [userId], if any, while nothing newer is in memory. */
    private suspend fun primeFromDisk(userId: String) {
        if (cache.value?.first == userId) return
        val saved = runCatching {
            db.keyValueQueries.selectById(cacheKey(userId)).awaitAsOneOrNull()?.value_
                ?.let { json.decodeFromString(serializer, it) }
        }.getOrNull() ?: return
        if (cache.value?.first != userId) cache.value = userId to saved
    }

    private fun cacheKey(userId: String) = "$CACHE_KEY_PREFIX$userId"

    private companion object {
        const val COLLECTION = "medals_history"
        const val CACHE_KEY_PREFIX = "medals.history."
    }
}
