package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import it.mensa.shared.api.endpoints.ReceiptsApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.ReceiptModel
import it.mensa.shared.sse.RealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ReceiptsRepository(
    private val api: ReceiptsApi,
    private val db: MensaDatabase,
    private val json: Json,
    private val realtimeClient: RealtimeClient,
) {
    fun observeAll(): Flow<List<ReceiptModel>> =
        db.receiptQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    fun observeOne(id: String): Flow<ReceiptModel?> =
        db.receiptQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toModel() }

    suspend fun refresh() {
        val items = api.list()
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            db.receiptQueries.deleteAll()
            items.forEach { upsertNoTx(it, now) }
        }
    }

    suspend fun firstSnapshot(): List<ReceiptModel> = observeAll().first()

    suspend fun getById(id: String): ReceiptModel? =
        db.receiptQueries.selectById(id).awaitAsOneOrNull()?.toModel()

    suspend fun getReceiptUrl(id: String): String = api.getReceiptUrl(id)

    fun observeRealtime(scope: CoroutineScope): Job =
        scope.launch(Dispatchers.Default) {
            realtimeClient.subscribe("payments").collect { event ->
                val msg = event.message
                when (msg.action) {
                    "create", "update" -> runCatching {
                        val r = json.decodeFromJsonElement(ReceiptModel.serializer(), msg.record)
                        upsertNoTx(r, Clock.System.now().toEpochMilliseconds())
                    }
                    "delete" -> runCatching {
                        val id = msg.record.jsonObject["id"]?.jsonPrimitive?.content.orEmpty()
                        if (id.isNotEmpty()) db.receiptQueries.deleteById(id)
                    }
                }
            }
        }

    private suspend fun upsertNoTx(r: ReceiptModel, now: Long) {
        db.receiptQueries.insertOrReplace(
            id = r.id,
            user = r.user,
            description = r.description,
            stripeCode = r.stripeCode,
            status = r.status,
            amount = r.amount.toLong(),
            createdAt = r.created.toEpochMilliseconds(),
            updatedAt = now,
        )
    }
}
