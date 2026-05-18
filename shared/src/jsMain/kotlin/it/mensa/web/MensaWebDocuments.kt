@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.model.DocumentElaboratedModel
import it.mensa.shared.model.DocumentModel
import it.mensa.shared.repository.DocumentsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebDocuments internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: DocumentsRepository get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (documents: Array<MensaWebDocument>) -> Unit): () -> Unit {
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

    fun getById(id: String): Promise<MensaWebDocument?> = scope.promise {
        sdk.awaitReady()
        repo.getById(id)?.toJs()
    }

    fun getElaborated(elaboratedId: String): Promise<MensaWebDocumentSummary?> = scope.promise {
        sdk.awaitReady()
        repo.getElaborated(elaboratedId)?.toJs()
    }
}

@JsExport
data class MensaWebDocument(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val dateMs: Double,
    val pdfUrl: String,
    val elaboratedId: String,
    val uploadedBy: String,
)

@JsExport
data class MensaWebDocumentSummary(
    val id: String,
    val documentId: String,
    val markdown: String,
)

internal fun DocumentModel.toJs(): MensaWebDocument {
    val base = MensaSdk.apiBaseUrl()
    val pdf = if (file.isNotBlank()) "$base/api/files/documents/$id/$file" else ""
    return MensaWebDocument(
        id = id,
        title = name,
        description = description ?: "",
        category = category,
        dateMs = created.toEpochMilliseconds().toDouble(),
        pdfUrl = pdf,
        elaboratedId = elaborated,
        uploadedBy = uploadedBy,
    )
}

internal fun DocumentElaboratedModel.toJs(): MensaWebDocumentSummary = MensaWebDocumentSummary(
    id = id,
    documentId = document,
    markdown = iaResume,
)
