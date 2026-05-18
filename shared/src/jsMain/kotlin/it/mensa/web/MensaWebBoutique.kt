@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.model.BoutiqueModel
import it.mensa.shared.repository.BoutiqueRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebBoutique internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: BoutiqueRepository get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (products: Array<MensaWebBoutiqueProduct>) -> Unit): () -> Unit {
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

    fun getById(id: String): Promise<MensaWebBoutiqueProduct?> = scope.promise {
        sdk.awaitReady()
        repo.getById(id)?.toJs()
    }
}

@JsExport
data class MensaWebBoutiqueProduct(
    val id: String,
    val name: String,
    val description: String,
    val priceCents: Double,
    val imageUrl: String,
    val imageUrls: Array<String>,
    val orderUrl: String,
    val alternativeOf: String,
)

internal fun BoutiqueModel.toJs(): MensaWebBoutiqueProduct {
    val base = MensaSdk.apiBaseUrl()
    val urls = image.filter { it.isNotBlank() }
        .map { "$base/api/files/boutique/$id/$it" }
    return MensaWebBoutiqueProduct(
        id = id,
        name = name,
        description = description,
        // PocketBase stores `amount` in cents already (the iOS UI divides by 100
        // for display); pass-through here.
        priceCents = amount.toDouble(),
        imageUrl = urls.firstOrNull() ?: "",
        imageUrls = urls.toTypedArray(),
        // PocketBase has no dedicated "order_url"; the iOS app links to the
        // member portal page constructed by the host. Surface empty here so
        // the bridge layer can decide policy.
        orderUrl = "",
        alternativeOf = alternativeOf,
    )
}
