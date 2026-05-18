package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import it.mensa.shared.api.endpoints.BoutiqueApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.BoutiqueModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class BoutiqueRepository(
    private val api: BoutiqueApi,
    private val db: MensaDatabase,
    private val json: Json,
) {
    fun observeAll(): Flow<List<BoutiqueModel>> =
        db.boutiqueQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel(json) } }

    fun observeOne(id: String): Flow<BoutiqueModel?> =
        db.boutiqueQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toModel(json) }

    suspend fun refresh() {
        val items = api.list()
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            db.boutiqueQueries.deleteAll()
            items.forEach { b ->
                db.boutiqueQueries.insertOrReplace(
                    id = b.id,
                    uid = b.uid,
                    name = b.name,
                    description = b.description,
                    imagesJson = json.encodeToString(
                        ListSerializer(String.serializer()),
                        b.image,
                    ),
                    amount = b.amount.toLong(),
                    alternativeOf = b.alternativeOf,
                    createdAt = b.created.toEpochMilliseconds(),
                    updatedAt = now,
                )
            }
        }
    }

    suspend fun firstSnapshot(): List<BoutiqueModel> = observeAll().first()

    suspend fun getById(id: String): BoutiqueModel? =
        db.boutiqueQueries.selectById(id).awaitAsOneOrNull()?.toModel(json)
}
