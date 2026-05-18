@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.model.SigModel
import it.mensa.shared.repository.SigDraft
import it.mensa.shared.repository.SigsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class SigCreatePayload(
    val name: String,
    val link: String,
    val groupType: String,
    val description: String,
    /** PocketBase filename (no URL). Non-empty only when the image was already uploaded via
     *  a separate multipart call. Leave empty ("") to keep no image or current image. */
    val image: String,
)

@JsExport
class SigUpdatePayload(
    val name: String,
    val link: String,
    val groupType: String,
    val description: String,
    /** PocketBase filename (no URL). Non-empty only when the image was already uploaded. */
    val image: String,
)

@JsExport
class MensaWebSigs internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: SigsRepository get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (sigs: Array<MensaWebSig>) -> Unit): () -> Unit {
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

    fun getById(id: String): Promise<MensaWebSig?> = scope.promise {
        sdk.awaitReady()
        repo.getById(id)?.toJs()
    }

    fun create(payload: SigCreatePayload): Promise<MensaWebSig> = scope.promise {
        sdk.awaitReady()
        repo.create(
            SigDraft(
                name = payload.name,
                link = payload.link,
                groupType = payload.groupType,
                description = payload.description,
                imageBytes = null,
                imageFilename = payload.image.ifBlank { null },
            )
        ).toJs()
    }

    fun update(id: String, payload: SigUpdatePayload): Promise<MensaWebSig> = scope.promise {
        sdk.awaitReady()
        repo.update(
            id,
            SigDraft(
                name = payload.name,
                link = payload.link,
                groupType = payload.groupType,
                description = payload.description,
                imageBytes = null,
                imageFilename = payload.image.ifBlank { null },
            )
        ).toJs()
    }

    fun delete(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.delete(id)
    }
}

@JsExport
data class MensaWebSig(
    val id: String,
    val name: String,
    val link: String,
    val description: String,
    val groupType: String,
    /** Full public URL for use in <img> tags. Empty string if no image. */
    val coverUrl: String,
    /** Raw PocketBase filename (no URL) — use this to pre-populate edit forms. */
    val image: String,
)

internal fun SigModel.toJs(): MensaWebSig {
    val base = MensaSdk.apiBaseUrl()
    val cover = if (image.isNotBlank()) "$base/api/files/sigs/$id/$image" else ""
    return MensaWebSig(
        id = id,
        name = name,
        link = link,
        description = description,
        groupType = groupType,
        coverUrl = cover,
        image = image,
    )
}
