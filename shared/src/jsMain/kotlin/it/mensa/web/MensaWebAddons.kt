@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.MensaSdk
import it.mensa.shared.model.AddonModel
import it.mensa.shared.repository.AddonsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebAddons internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: AddonsRepository get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (addons: Array<MensaWebAddon>) -> Unit): () -> Unit {
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
}

@JsExport
data class MensaWebAddon(
    val id: String,
    val name: String,
    val description: String,
    val iconUrl: String,
    val version: String,
    val url: String,
    val requiredPower: Int,
)

internal fun AddonModel.toJs(): MensaWebAddon {
    val base = MensaSdk.apiBaseUrl()
    val iconUrl = if (icon.isNotBlank()) "$base/api/files/addons/$id/$icon" else ""
    return MensaWebAddon(
        id = id,
        name = name,
        description = description,
        iconUrl = iconUrl,
        version = version,
        url = url,
        requiredPower = requiredPower,
    )
}
