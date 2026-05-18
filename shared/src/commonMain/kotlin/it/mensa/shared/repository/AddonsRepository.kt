package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import it.mensa.shared.api.endpoints.AddonAccessData
import it.mensa.shared.api.endpoints.AddonsApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.AddonModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class AddonsRepository(
    private val api: AddonsApi,
    private val db: MensaDatabase,
    @Suppress("unused") private val json: Json,
) {
    fun observeAll(): Flow<List<AddonModel>> =
        db.addonQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    fun observeOne(id: String): Flow<AddonModel?> =
        db.addonQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toModel() }

    suspend fun refresh() {
        val items = api.list()
        db.transaction {
            db.addonQueries.deleteAll()
            items.forEach { a ->
                db.addonQueries.insertOrReplace(
                    id = a.id,
                    name = a.name,
                    description = a.description,
                    icon = a.icon,
                    version = a.version,
                    url = a.url,
                    requiredPower = a.requiredPower.toLong(),
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
    }

    suspend fun firstSnapshot(): List<AddonModel> = observeAll().first()

    suspend fun getById(id: String): AddonModel? =
        db.addonQueries.selectById(id).awaitAsOneOrNull()?.toModel()

    suspend fun getAccessData(addonId: String): AddonAccessData =
        api.getAccessData(addonId)
}
