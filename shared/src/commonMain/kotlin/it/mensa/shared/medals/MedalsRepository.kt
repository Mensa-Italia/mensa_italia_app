package it.mensa.shared.medals

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.MedalStatsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json

/**
 * The signed-in member's medals: `view_medals` read and run through
 * [MedalRules]. In memory, tied to the user it was read for, so another
 * account never sees the previous one's medals; the last counters read are
 * also kept in SQLite (KeyValue, one row per user), so after a cold start
 * the medals show at once and the network only brings them up to date.
 */
class MedalsRepository(
    private val pb: PocketBaseClient,
    private val auth: AuthRepository,
    private val db: MensaDatabase,
    private val json: Json,
) {
    private val cache = MutableStateFlow<Pair<String, List<MedalStatus>>?>(null)

    /** The medals, once read for the current user; empty until then. */
    fun observe(): Flow<List<MedalStatus>> =
        combine(cache, auth.currentUser) { cached, user ->
            val meId = if (MedalFixtures.enabled) MedalFixtures.ME_ID else user?.id
            if (cached != null && meId != null && cached.first == meId) cached.second else emptyList()
        }

    /** Rereads the counters. Throws on network errors; the last read stays. */
    @Throws(Exception::class)
    suspend fun refresh(): List<MedalStatus> {
        if (MedalFixtures.enabled) {
            return MedalRules.evaluate(MedalFixtures.stats).also { cache.value = MedalFixtures.ME_ID to it }
        }
        val userId = auth.currentUser.value?.id ?: return emptyList()
        primeFromDisk(userId)
        val stats: MedalStatsModel = pb.getOne(COLLECTION, userId)
        val medals = MedalRules.evaluate(stats)
        cache.value = userId to medals
        runCatching { db.keyValueQueries.insertOrReplace(key = cacheKey(userId), value_ = json.encodeToString(MedalStatsModel.serializer(), stats)) }
        return medals
    }

    /** Publishes the counters saved for [userId], if any, while nothing newer is in memory. */
    private suspend fun primeFromDisk(userId: String) {
        if (cache.value?.first == userId) return
        val saved = runCatching {
            db.keyValueQueries.selectById(cacheKey(userId)).awaitAsOneOrNull()?.value_
                ?.let { json.decodeFromString(MedalStatsModel.serializer(), it) }
        }.getOrNull() ?: return
        if (cache.value?.first != userId) cache.value = userId to MedalRules.evaluate(saved)
    }

    private fun cacheKey(userId: String) = "$CACHE_KEY_PREFIX$userId"

    private companion object {
        const val COLLECTION = "view_medals"
        const val CACHE_KEY_PREFIX = "medals.stats."
    }
}
