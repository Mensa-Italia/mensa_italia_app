package it.mensa.shared.repository

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.endpoints.NotificationsApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.NotificationModel
import it.mensa.shared.sse.RealtimeClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class NotificationsRepository(
    private val api: NotificationsApi,
    private val db: MensaDatabase,
    private val json: Json,
    private val realtimeClient: RealtimeClient,
) {
    fun observeAll(): Flow<List<NotificationModel>> =
        db.notificationQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel(json) } }

    suspend fun refresh() {
        // Strategia: tutte le non-lette (qualunque siano) + solo le ultime
        // 20 lette. Evita di scaricare uno storico potenzialmente enorme
        // (e di accumulare cruft nel DB) mantenendo l'utente sempre al
        // corrente di cio' che richiede la sua attenzione.
        val unread = api.listUnread()
        val recentRead = api.listRecentSeen(limit = 20)
        val items = unread + recentRead
        db.transaction {
            db.notificationQueries.deleteAll()
            items.forEach { n ->
                db.notificationQueries.insertOrReplace(
                    id = n.id,
                    tr = n.tr,
                    trNamedParamsJson = json.encodeToString(
                        kotlinx.serialization.serializer<Map<String, String>>(),
                        n.trNamedParams
                    ),
                    dataJson = n.data?.let { json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), it) },
                    seen = n.seen?.toEpochMilliseconds(),
                    createdAt = n.created.toEpochMilliseconds(),
                    updatedAt = Clock.System.now().toEpochMilliseconds(),
                )
            }
        }
    }

    /**
     * Marks notification as seen on the backend and updates local cache.
     */
    suspend fun markSeen(id: String) {
        val updated = api.see(id)
        db.notificationQueries.insertOrReplace(
            id = updated.id,
            tr = updated.tr,
            trNamedParamsJson = json.encodeToString(
                kotlinx.serialization.serializer<Map<String, String>>(),
                updated.trNamedParams
            ),
            dataJson = updated.data?.let { json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), it) },
            seen = updated.seen?.toEpochMilliseconds(),
            createdAt = updated.created.toEpochMilliseconds(),
            updatedAt = Clock.System.now().toEpochMilliseconds(),
        )
    }

    /**
     * Marks all currently unseen notifications as seen.
     *
     * Optimistic and best-effort: iterates over the locally cached unseen notifications and
     * calls [NotificationsApi.see] for each one with bounded concurrency. Per-notification
     * failures are swallowed so that a single failing call does not prevent the others from
     * being marked. Each successful response updates the local cache (mirroring [markSeen]).
     */
    suspend fun markAllSeen() {
        val unseenIds = db.notificationQueries.selectAll().awaitAsList()
            .filter { it.seen == null }
            .map { it.id }
        if (unseenIds.isEmpty()) return

        val semaphore = Semaphore(permits = 8)
        coroutineScope {
            unseenIds.map { id ->
                async(Dispatchers.Default) {
                    semaphore.withPermit {
                        runCatching {
                            val updated = api.see(id)
                            db.notificationQueries.insertOrReplace(
                                id = updated.id,
                                tr = updated.tr,
                                trNamedParamsJson = json.encodeToString(
                                    kotlinx.serialization.serializer<Map<String, String>>(),
                                    updated.trNamedParams
                                ),
                                dataJson = updated.data?.let {
                                    json.encodeToString(JsonObject.serializer(), it)
                                },
                                seen = updated.seen?.toEpochMilliseconds(),
                                createdAt = updated.created.toEpochMilliseconds(),
                                updatedAt = Clock.System.now().toEpochMilliseconds(),
                            )
                        }
                    }
                }
            }.awaitAll()
        }
    }

    /**
     * Deletes a notification from the backend and removes it from the local cache.
     */
    suspend fun removeOne(id: String) {
        api.delete(id)
        db.notificationQueries.deleteById(id)
    }

    /**
     * Avvia la sottoscrizione SSE realtime per user_notifications.
     * Gli eventi create/update aggiornano la cache locale; delete rimuove il record.
     * Chiama .cancel() sul Job restituito per terminare.
     */
    fun observeRealtime(scope: CoroutineScope): Job {
        return scope.launch(Dispatchers.Default) {
            realtimeClient.observe(setOf("user_notifications/*")).collect { event ->
                val msg = event.message
                when (msg.action) {
                    "create", "update" -> {
                        runCatching {
                            val notif = json.decodeFromJsonElement(
                                NotificationModel.serializer(),
                                msg.record
                            )
                            db.notificationQueries.insertOrReplace(
                                id = notif.id,
                                tr = notif.tr,
                                trNamedParamsJson = json.encodeToString(
                                    kotlinx.serialization.serializer<Map<String, String>>(),
                                    notif.trNamedParams
                                ),
                                dataJson = notif.data?.let {
                                    json.encodeToString(JsonObject.serializer(), it)
                                },
                                seen = notif.seen?.toEpochMilliseconds(),
                                createdAt = notif.created.toEpochMilliseconds(),
                                updatedAt = Clock.System.now().toEpochMilliseconds(),
                            )
                        }
                    }
                    "delete" -> {
                        runCatching {
                            val id = msg.record.jsonObject["id"]
                                ?.jsonPrimitive?.content.orEmpty()
                            if (id.isNotEmpty()) db.notificationQueries.deleteById(id)
                        }
                    }
                }
            }
        }
    }
}
