@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.model.Podcast
import it.mensa.shared.model.PodcastEpisode
import it.mensa.shared.repository.PodcastsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebPodcasts internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: PodcastsRepository get() = KoinPlatform.getKoin().get()

    fun subscribePodcasts(callback: (podcasts: Array<MensaWebPodcast>) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.observePodcasts().collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return { job.cancel() }
    }

    fun refreshPodcasts(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.refreshPodcasts()
    }

    /**
     * Subscribes to the per-podcast episodes flow. Eagerly triggers a network
     * fetch so the in-memory cache is populated before the first emission;
     * failures are swallowed (the empty-list emission is acceptable UX).
     */
    fun subscribeEpisodes(
        podcastId: String,
        callback: (episodes: Array<MensaWebPodcastEpisode>) -> Unit,
    ): () -> Unit {
        val refresh: Job = scope.launch {
            sdk.awaitReady()
            runCatching { repo.refreshEpisodes(podcastId) }
        }
        val sub: Job = scope.launch {
            sdk.awaitReady()
            repo.observeEpisodes(podcastId).collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return {
            refresh.cancel()
            sub.cancel()
        }
    }
}

@JsExport
data class MensaWebPodcast(
    val id: String,
    val title: String,
    val description: String,
    val coverUrl: String,
    val episodeCount: Int,
    val totalDurationSec: Int,
)

@JsExport
data class MensaWebPodcastEpisode(
    val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String,
    val coverUrl: String,
    val durationSec: Int,
    val publishedMs: Double,
)

internal fun Podcast.toJs(): MensaWebPodcast = MensaWebPodcast(
    id = id,
    title = title,
    description = description,
    coverUrl = imageUrl ?: "",
    episodeCount = episodesCount,
    // PB doesn't track an aggregate duration; we'd need to sum the episode list,
    // which is fetched separately. Surface 0 so the UI can render "—".
    totalDurationSec = 0,
)

internal fun PodcastEpisode.toJs(): MensaWebPodcastEpisode = MensaWebPodcastEpisode(
    id = id,
    podcastId = podcastId,
    title = title,
    description = description,
    audioUrl = audioUrl ?: "",
    coverUrl = imageUrl ?: "",
    durationSec = durationSeconds,
    // `publishedAt` is the raw PB ISO string; parse to epoch-ms for JS use.
    publishedMs = parseIsoOrZero(publishedAt),
)

private fun parseIsoOrZero(iso: String): Double {
    if (iso.isBlank()) return 0.0
    // PocketBase emits "YYYY-MM-DD HH:mm:ss.SSSZ" (space, not T). Normalise
    // to ISO-8601 before delegating to kotlinx-datetime.
    val normalised = iso.replace(' ', 'T').let {
        if (it.endsWith("Z") || it.contains('+') || it.matches(Regex(".*[+-]\\d{2}:\\d{2}\$"))) it else "${it}Z"
    }
    return runCatching {
        kotlinx.datetime.Instant.parse(normalised).toEpochMilliseconds().toDouble()
    }.getOrDefault(0.0)
}
