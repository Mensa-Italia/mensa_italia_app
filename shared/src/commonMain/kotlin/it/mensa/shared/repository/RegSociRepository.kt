package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import it.mensa.shared.api.endpoints.RegSociApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.RegSociModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

class RegSociRepository(
    private val api: RegSociApi,
    private val db: MensaDatabase,
    private val json: Json,
) {
    fun observeAll(): Flow<List<RegSociModel>> =
        db.regSociQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel(json) } }

    fun observeOne(id: String): Flow<RegSociModel?> =
        db.regSociQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toModel(json) }

    suspend fun refresh() {
        refreshAndReturn()
    }

    /**
     * Refresh + return the freshly-downloaded list. The diff (which members
     * changed in data vs. in image) is computed by [refreshAndDiff]; callers
     * that just want the data should use this variant.
     */
    suspend fun refreshAndReturn(): List<RegSociModel> {
        val items = api.list()
        db.transaction {
            db.regSociQueries.deleteAll()
            items.forEach { upsertNoTx(it) }
        }
        return items
    }

    /**
     * Refresh + return both the fresh list AND the previous (id → hashes)
     * snapshot, atomically captured before the upsert. Used by the Spotlight
     * sync engine to decide per-member what changed (data only, image only,
     * both, nothing, brand-new) without re-querying the DB.
     */
    suspend fun refreshAndDiff(): RegSociRefreshResult {
        val items = api.list()
        // Snapshot OUTSIDE the transaction so we can suspend on awaitAsList;
        // the brief window between snapshot and upsert is harmless here
        // (RegSoci is single-writer: the only mutator is this method).
        val previous: Map<String, HashPair> = buildMap {
            db.regSociQueries.selectAllHashes().awaitAsList().forEach {
                put(it.id, HashPair(it.dataHash, it.imageHash))
            }
        }
        db.transaction {
            db.regSociQueries.deleteAll()
            items.forEach { upsertNoTx(it) }
        }
        return RegSociRefreshResult(members = items, previousHashes = previous)
    }

    data class HashPair(val dataHash: String?, val imageHash: String?)

    data class RegSociRefreshResult(
        val members: List<RegSociModel>,
        val previousHashes: Map<String, HashPair>,
    )

    suspend fun searchByName(query: String): List<RegSociModel> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()
        val items = api.searchByName(trimmed)
        db.transaction {
            items.forEach { upsertNoTx(it) }
        }
        return items
    }

    suspend fun firstSnapshot(): List<RegSociModel> = observeAll().first()

    suspend fun getById(id: String): RegSociModel? {
        runCatching { val r = api.get(id); upsertNoTx(r); return r }
        return db.regSociQueries.selectById(id).awaitAsOneOrNull()?.toModel(json)
    }

    private suspend fun upsertNoTx(m: RegSociModel) {
        db.regSociQueries.insertOrReplace(
            id = m.id,
            uid = 0L,
            name = m.name,
            image = m.image,
            city = m.city,
            birthdate = m.birthdate?.toEpochMilliseconds(),
            state = m.state,
            fullDataJson = json.encodeToString(JsonObject.serializer(), m.fullData),
            fullProfileLink = m.fullProfileLink,
            nameToSearch = m.name.lowercase(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
            dataHash = m.dataHash,
            imageHash = m.imageHash,
        )
    }
}
