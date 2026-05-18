@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.model.TicketModel
import it.mensa.shared.repository.TicketsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlinx.datetime.Clock
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebTickets internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: TicketsRepository get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (tickets: Array<MensaWebTicket>) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.observeAll().collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return { job.cancel() }
    }

    fun refresh(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.refresh()
    }

    fun getById(id: String): Promise<MensaWebTicket?> = scope.promise {
        sdk.awaitReady()
        repo.getById(id)?.toJs()
    }
}

/**
 * `status` is derived locally because PocketBase doesn't ship a server-computed
 * one for the `tickets` collection. The rule mirrors the iOS view-model:
 *  - no deadline → `"active"`
 *  - deadline in the future → `"active"`
 *  - deadline already passed → `"expired"`
 */
@JsExport
data class MensaWebTicket(
    val id: String,
    val name: String,
    val description: String,
    val status: String,
    val qrPayload: String,
    val deadlineMs: Double,
    val internalRef: String,
    val createdMs: Double,
    val linkUrl: String,
    val customerData: String,
)

internal fun TicketModel.toJs(): MensaWebTicket {
    val deadlineMs = deadline?.toEpochMilliseconds() ?: 0L
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val status = when {
        deadline == null -> "active"
        deadlineMs >= nowMs -> "active"
        else -> "expired"
    }
    return MensaWebTicket(
        id = id,
        name = name ?: "",
        description = description ?: "",
        status = status,
        qrPayload = qr ?: "",
        deadlineMs = deadlineMs.toDouble(),
        internalRef = internalRefId ?: "",
        createdMs = created.toEpochMilliseconds().toDouble(),
        linkUrl = link ?: "",
        customerData = customerData ?: "",
    )
}
