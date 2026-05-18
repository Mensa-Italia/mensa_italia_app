@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.model.ReceiptModel
import it.mensa.shared.repository.ReceiptsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebReceipts internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: ReceiptsRepository get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (receipts: Array<MensaWebReceipt>) -> Unit): () -> Unit {
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

    fun getById(id: String): Promise<MensaWebReceipt?> = scope.promise {
        sdk.awaitReady()
        repo.getById(id)?.toJs()
    }

    fun pdfUrl(id: String): Promise<String?> = scope.promise {
        sdk.awaitReady()
        runCatching { repo.getReceiptUrl(id) }.getOrNull()
    }
}

/**
 * `kind` is derived from the freeform `description` field — PocketBase doesn't
 * categorize receipts on the server. The classification mirrors iOS's
 * `ReceiptKind` heuristics so the UI can pick the right icon/label without
 * re-implementing the rule per platform.
 */
@JsExport
data class MensaWebReceipt(
    val id: String,
    val kind: String,
    val description: String,
    val amountCents: Double,
    val dateMs: Double,
    val stripeCode: String,
    val status: String,
)

internal fun ReceiptModel.toJs(): MensaWebReceipt = MensaWebReceipt(
    id = id,
    kind = classifyKind(description),
    description = description ?: "",
    amountCents = amount.toDouble(),
    dateMs = created.toEpochMilliseconds().toDouble(),
    stripeCode = stripeCode,
    status = status,
)

private fun classifyKind(description: String?): String {
    val d = description?.lowercase().orEmpty()
    return when {
        d.contains("donazione") || d.contains("donation") -> "donation"
        d.contains("rinnovo") || d.contains("renewal") -> "renewal"
        d.contains("acquisto") || d.contains("purchase") || d.contains("ordine") -> "purchase"
        d.isBlank() -> "other"
        else -> "other"
    }
}
