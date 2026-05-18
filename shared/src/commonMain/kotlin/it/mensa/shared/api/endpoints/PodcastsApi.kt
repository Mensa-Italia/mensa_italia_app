package it.mensa.shared.api.endpoints

import it.mensa.shared.api.ApiConfig
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.Podcast
import it.mensa.shared.model.PodcastEpisode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── PocketBase DTOs ───────────────────────────────────────────────────────────

@Serializable
private data class PbPodcast(
    val id: String = "",
    @SerialName("youtube_playlist_id") val youtubePlaylistId: String = "",
    val title: String = "",
    val description: String = "",
    val image: String = "",
    @SerialName("episodes_count") val episodesCount: Int = 0,
    @SerialName("last_synced_at") val lastSyncedAt: String = "",
    @SerialName("last_sync_error") val lastSyncError: String = "",
    val created: String = "",
    val updated: String = "",
)

@Serializable
private data class PbPodcastEpisode(
    val id: String = "",
    val podcast: String = "",
    @SerialName("youtube_video_id") val youtubeVideoId: String = "",
    val title: String = "",
    val description: String = "",
    val audio: String = "",
    val image: String = "",
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
    @SerialName("published_at") val publishedAt: String = "",
    val created: String = "",
    val updated: String = "",
)

// ── Mappers ──────────────────────────────────────────────────────────────────

private fun PbPodcast.toPodcast(): Podcast = Podcast(
    id = id,
    title = title,
    description = description,
    imageUrl = image.takeIf { it.isNotBlank() }
        ?.let { "${ApiConfig.BASE_URL}/api/files/podcasts/$id/$it" },
    episodesCount = episodesCount,
    lastSyncedAt = lastSyncedAt.takeIf { it.isNotBlank() },
)

private fun PbPodcastEpisode.toEpisode(): PodcastEpisode = PodcastEpisode(
    id = id,
    podcastId = podcast,
    title = title,
    description = description,
    audioUrl = audio.takeIf { it.isNotBlank() }
        ?.let { "${ApiConfig.BASE_URL}/api/files/podcast_episodes/$id/$it" },
    imageUrl = image.takeIf { it.isNotBlank() }
        ?.let { "${ApiConfig.BASE_URL}/api/files/podcast_episodes/$id/$it" },
    durationSeconds = durationSeconds,
    publishedAt = publishedAt,
)

// ── API ───────────────────────────────────────────────────────────────────────

class PodcastsApi(private val pb: PocketBaseClient) {

    suspend fun listPodcasts(): List<Podcast> =
        pb.fullList<PbPodcast>("podcasts", sort = "-created")
            .map { it.toPodcast() }

    suspend fun listEpisodes(
        podcastId: String,
        page: Int = 1,
        perPage: Int = 50,
    ): List<PodcastEpisode> =
        pb.list<PbPodcastEpisode>(
            collection = "podcast_episodes",
            page = page,
            perPage = perPage,
            filter = "(podcast=\"$podcastId\")",
            sort = "-published_at",
        ).items.map { it.toEpisode() }
}
