package it.mensa.shared.sse

import io.ktor.client.HttpClient
import io.ktor.client.plugins.sse.serverSentEventsSession
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class RealtimeMessage(
    val action: String,       // "create" | "update" | "delete"
    val record: JsonElement   // raw record JSON — deserializzabile dal caller
)

data class RealtimeEvent(
    val topic: String,        // collection o "collection/recordId"
    val message: RealtimeMessage
)

class RealtimeClient(
    private val client: HttpClient,
    private val json: Json
) {
    /**
     * Apre lo stream SSE, registra subscriptions e ritorna un Flow di eventi.
     * Il Flow è cold: alla cancellazione chiude lo stream.
     * Riconnessione con backoff esponenziale (max 30s) in caso di errore.
     */
    /**
     * Convenience helper: subscribe to a single PocketBase collection (or single record
     * within a collection when [recordId] is supplied). Wraps [observe] with the
     * canonical PB topic syntax "collection/recordId" or "collection/" plus wildcard.
     */
    fun subscribe(collection: String, recordId: String? = null): Flow<RealtimeEvent> {
        val topic = if (recordId.isNullOrEmpty()) "$collection/*" else "$collection/$recordId"
        return observe(setOf(topic))
    }

    fun observe(subscriptions: Set<String>): Flow<RealtimeEvent> = flow {
        var attempt = 0
        while (currentCoroutineContext().isActive) {
            try {
                val result = connect()
                val clientId = result.first
                val eventFlow = result.second
                attempt = 0
                registerSubscriptions(clientId, subscriptions)
                eventFlow.collect { emit(it) }
                // Arrivare qui significa che il server ha chiuso senza errore.
                // Prima non succedeva mai — lo SharedFlow non terminava — e
                // senza questa pausa il `while` riaprirebbe la connessione
                // all'istante, in un ciclo stretto: `attempt` viene azzerato a
                // ogni connect riuscito, quindi il backoff non entrerebbe mai.
                delay(1_000L)
            } catch (ce: CancellationException) {
                throw ce
            } catch (e: Throwable) {
                attempt++
                val backoffMs = minOf(30_000L, 500L * (1L shl minOf(attempt, 6)))
                delay(backoffMs)
            }
        }
    }.flowOn(Dispatchers.Default)

    private suspend fun connect(): Pair<String, Flow<RealtimeEvent>> {
        val deferredClientId = CompletableDeferred<String>()
        // Un Channel e non un MutableSharedFlow: un canale si chiude *con una
        // causa*, e questa e' l'unica via per cui un errore dello stream possa
        // raggiungere il collector di `observe`. Uno SharedFlow non finisce
        // mai, quindi anche solo ingoiare l'eccezione qui lascerebbe
        // `eventFlow.collect` appeso per sempre su uno stream morto.
        val events = Channel<RealtimeEvent>(
            capacity = 64,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        val session = client.serverSentEventsSession(urlString = "/api/realtime")
        val streamScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        val job = streamScope.launch {
            try {
                session.incoming.collect { sseEvent ->
                    val type = sseEvent.event ?: "message"
                    val data = sseEvent.data.orEmpty()
                    if (type == "PB_CONNECT") {
                        runCatching {
                            val connectPayload = json.parseToJsonElement(data)
                            val cid = connectPayload.jsonObject["clientId"]
                                ?.jsonPrimitive?.content.orEmpty()
                            deferredClientId.complete(cid)
                        }.onFailure { deferredClientId.completeExceptionally(it) }
                    } else {
                        // Solo il parsing e' tollerante: un payload malformato
                        // si salta. `trySend` resta fuori dal runCatching, per
                        // non trasformare una cancellazione in un no-op.
                        val msg = runCatching {
                            json.decodeFromString(RealtimeMessage.serializer(), data)
                        }.getOrNull()
                        if (msg != null) events.trySend(RealtimeEvent(topic = type, message = msg))
                    }
                }
                // Lo stream e' finito senza errori: il server ha chiuso.
                events.close()
            } catch (ce: CancellationException) {
                events.close(ce)
                throw ce
            } catch (t: Throwable) {
                // QUESTO catch e' il motivo per cui l'app non muore piu'.
                //
                // Questa e' una coroutine radice di uno scope staccato, senza
                // CoroutineExceptionHandler: un'eccezione che esce di qui non
                // viene vista dal `catch` di `observe` (sta su un'altra
                // coroutine), va all'handler globale e su Android quello e'
                // KillApplicationHandler. Bastava una `SSEClientException:
                // Software caused connection abort` — cioe' una connessione
                // mobile che cade — per chiudere l'app in produzione.
                //
                // Chiudendo il canale con la causa, l'errore arriva dove
                // doveva arrivare fin dall'inizio: al collector di `observe`,
                // che riconnette con backoff.
                deferredClientId.completeExceptionally(t)
                events.close(t)
            } finally {
                streamScope.cancel()
            }
        }

        val clientId = withTimeoutOrNull(10_000L) { deferredClientId.await() }
            ?: run {
                job.cancel()
                streamScope.cancel()
                events.close()
                error("PB realtime: missing clientId after timeout")
            }

        return clientId to events.receiveAsFlow()
            .onCompletion { job.cancel(); streamScope.cancel() }
    }

    private suspend fun registerSubscriptions(clientId: String, subs: Set<String>) {
        if (subs.isEmpty()) return
        client.post("/api/realtime") {
            contentType(ContentType.Application.Json)
            setBody(buildJsonBody(clientId, subs))
        }
    }

    private fun buildJsonBody(clientId: String, subs: Set<String>): String {
        val arr = subs.joinToString(",") { "\"${it}\"" }
        return "{\"clientId\":\"$clientId\",\"subscriptions\":[$arr]}"
    }
}
