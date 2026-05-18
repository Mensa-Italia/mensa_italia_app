package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import it.mensa.shared.api.endpoints.TicketsApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.TicketModel
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

class TicketsRepository(
    private val api: TicketsApi,
    private val db: MensaDatabase,
    private val json: Json,
    private val realtimeClient: RealtimeClient,
) {
    fun observeAll(): Flow<List<TicketModel>> =
        db.ticketQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    fun observeOne(id: String): Flow<TicketModel?> =
        db.ticketQueries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.Default)
            .map { it?.toModel() }

    suspend fun refresh() {
        val items = api.list()
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            db.ticketQueries.deleteAll()
            items.forEach { upsertNoTx(it, now) }
        }
    }

    suspend fun firstSnapshot(): List<TicketModel> = observeAll().first()

    suspend fun getById(id: String): TicketModel? =
        db.ticketQueries.selectById(id).awaitAsOneOrNull()?.toModel()

    /**
     * Subscribes to `tickets` realtime updates and write-through into the DB.
     */
    fun observeRealtime(scope: CoroutineScope): Job =
        scope.launch(Dispatchers.Default) {
            realtimeClient.subscribe("tickets").collect { event ->
                val msg = event.message
                when (msg.action) {
                    "create", "update" -> runCatching {
                        val t = json.decodeFromJsonElement(TicketModel.serializer(), msg.record)
                        upsertNoTx(t, Clock.System.now().toEpochMilliseconds())
                    }
                    "delete" -> runCatching {
                        val id = msg.record.jsonObject["id"]?.jsonPrimitive?.content.orEmpty()
                        if (id.isNotEmpty()) db.ticketQueries.deleteById(id)
                    }
                }
            }
        }

    private suspend fun upsertNoTx(t: TicketModel, now: Long) {
        db.ticketQueries.insertOrReplace(
            id = t.id,
            userId = t.userId,
            name = t.name,
            description = t.description,
            link = t.link,
            qr = t.qr,
            internalRefId = t.internalRefId,
            customerData = t.customerData,
            deadline = t.deadline?.toEpochMilliseconds(),
            createdAt = t.created.toEpochMilliseconds(),
            updatedAt = now,
        )
    }
}
