@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.model.search.SearchHit
import it.mensa.shared.repository.SearchRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform

@JsExport
class MensaWebSearch internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: SearchRepository get() = KoinPlatform.getKoin().get()

    /**
     * Subscribe to the debounced search state. Callback signature is
     * `(state, hits)` where `state` is one of:
     *  - "idle"     — empty input / no results yet
     *  - "loading"  — debounced search request in flight
     *  - "success"  — `hits` is populated with the flat list of matches
     *  - "error"    — `hits` is empty
     *
     * The hit list flattens every result-type bucket from the server response
     * (events, deals, users, ...) — the consumer can group by `MensaWebSearchHit.type`.
     */
    fun subscribeState(
        callback: (state: String, hits: Array<MensaWebSearchHit>) -> Unit,
    ): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.state.collect { st ->
                when (st) {
                    is SearchRepository.State.Idle -> callback("idle", emptyArray())
                    is SearchRepository.State.Loading -> callback("loading", emptyArray())
                    is SearchRepository.State.Success -> {
                        val hits = st.response.results.flatMap { (type, list) ->
                            list.map { it.toJs(type) }
                        }.toTypedArray()
                        callback("success", hits)
                    }
                    is SearchRepository.State.Error -> callback("error", emptyArray())
                }
            }
        }
        return { job.cancel() }
    }

    fun update(query: String) {
        // Fire-and-forget: SearchRepository's MutableStateFlow is thread-safe and
        // the debounce pipeline picks the new value up on the next 300ms tick.
        repo.update(query)
    }

    fun clear() {
        repo.clear()
    }
}

@JsExport
data class MensaWebSearchHit(
    val type: String,    // "event" | "deal" | "user" | "document" | "boutique" | "sig" | "addon"
    val id: String,
    val label: String,   // title
    val sublabel: String, // subtitle
    val imageUrl: String,
    val url: String,     // deep_link
    val score: Double,
)

internal fun SearchHit.toJs(type: String): MensaWebSearchHit = MensaWebSearchHit(
    type = type,
    id = id,
    label = title,
    sublabel = subtitle,
    imageUrl = image,
    url = deepLink,
    score = score,
)
