package it.mensa.shared.medals

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.api.endpoints.RegSociApi
import it.mensa.shared.auth.AuthRepository
import it.mensa.shared.config.FeatureFlags
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.time.Season
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * The medals of the region the signed-in member is in now, read from
 * `leaderboard_region_medals`. The medals belong to the region: the member's
 * region comes from their registry row (`members_registry.area`), and a
 * member who moves simply sees the other cabinet, with no state of their own
 * to carry over. In memory, tied to the user it was read for; the region key
 * and the rows are also kept in SQLite (KeyValue), so after a cold start the
 * cabinet shows at once and the network only brings it up to date.
 *
 * Off unless the `region_medals_enabled` flag is on ([observeAvailable]).
 */
class RegionMedalsRepository(
    private val pb: PocketBaseClient,
    private val auth: AuthRepository,
    private val regSoci: RegSociApi,
    private val db: MensaDatabase,
    private val json: Json,
    private val flags: FeatureFlags,
    private val currentSeason: () -> Int = { Season.current() },
) {
    /** What was read for [userId]: their region's key (null when they have none) and its rows (null until read). */
    private data class Held(val userId: String, val key: String?, val rows: List<RegionMedalModel>?)

    private val held = MutableStateFlow<Held?>(null)

    /**
     * Whether the cabinet's region page exists: the flag is on and the member's
     * region is known. False while the region is still being resolved.
     */
    fun observeAvailable(): Flow<Boolean> =
        combine(held, auth.currentUser, flags.flags) { held, user, current ->
            val meId = meId(user?.id)
            when {
                RegionMedalFixtures.enabled -> true
                meId == null || held == null || held.userId != meId -> false
                else -> current.regionMedals && held.key != null
            }
        }

    /** The region's medals, once read for the current user; null until then. */
    fun observe(): Flow<RegionMedalsState?> =
        combine(held, auth.currentUser) { held, user ->
            val meId = meId(user?.id)
            val rows = held?.takeIf { it.userId == meId && it.key != null }?.rows ?: return@combine null
            RegionMedalsState.build(rows, currentSeason(), json, meId)
        }

    /** Rereads the region and its medals. Throws on network errors; the last read stays. */
    @Throws(Exception::class)
    suspend fun refresh() {
        if (RegionMedalFixtures.enabled) {
            held.value = Held(MedalFixtures.ME_ID, RegionMedalFixtures.KEY, RegionMedalFixtures.rows(currentSeason()))
            return
        }
        if (!flags.regionMedalsEnabled) return
        val userId = auth.currentUser.value?.id ?: return
        primeFromDisk(userId)
        val key = resolveKey(userId)
        if (key == null) {
            held.value = Held(userId, null, null)
            return
        }
        val rows: List<RegionMedalModel> = pb.fullList(COLLECTION, filter = "region_key = \"${key.replace("\"", "")}\"", sort = "-season,medal")
        held.value = Held(userId, key, rows)
        runCatching {
            db.keyValueQueries.insertOrReplace(
                key = rowsKey(key),
                value_ = json.encodeToString(ListSerializer(RegionMedalModel.serializer()), rows),
            )
        }
    }

    /**
     * The key of the member's region: from their registry row, or the one
     * saved when the network doesn't answer. Null when they live abroad.
     */
    private suspend fun resolveKey(userId: String): String? {
        val fresh = try {
            RegionKey.of(regSoci.get(userId).area)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            val saved = readKey(userId)
            if (saved != null) return saved.ifEmpty { null }
            throw e
        }
        runCatching { db.keyValueQueries.insertOrReplace(key = keyKey(userId), value_ = fresh.orEmpty()) }
        return fresh
    }

    /** Publishes the region key and rows saved for [userId], if any, while nothing newer is in memory. */
    private suspend fun primeFromDisk(userId: String) {
        if (held.value?.userId == userId) return
        val key = readKey(userId)?.ifEmpty { null } ?: return
        val rows = runCatching {
            db.keyValueQueries.selectById(rowsKey(key)).awaitAsOneOrNull()?.value_
                ?.let { json.decodeFromString(ListSerializer(RegionMedalModel.serializer()), it) }
        }.getOrNull()
        if (held.value?.userId != userId) held.value = Held(userId, key, rows)
    }

    /** The saved key: "" for a member known to have no region, null when nothing is saved. */
    private suspend fun readKey(userId: String): String? =
        runCatching { db.keyValueQueries.selectById(keyKey(userId)).awaitAsOneOrNull()?.value_ }.getOrNull()

    private fun meId(userId: String?): String? = if (RegionMedalFixtures.enabled) MedalFixtures.ME_ID else userId

    private fun keyKey(userId: String) = "$CACHE_KEY_PREFIX.key.$userId"
    private fun rowsKey(key: String) = "$CACHE_KEY_PREFIX.$key"

    private companion object {
        const val COLLECTION = "leaderboard_region_medals"
        const val CACHE_KEY_PREFIX = "region_medals"
    }
}
