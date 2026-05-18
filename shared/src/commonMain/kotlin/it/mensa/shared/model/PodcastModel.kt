package it.mensa.shared.model

data class Podcast(
    val id: String,
    val title: String,
    val description: String,
    val imageUrl: String?,      // full URL built from PB file field
    val episodesCount: Int,
    val lastSyncedAt: String?,
)

data class PodcastEpisode(
    val id: String,
    val podcastId: String,
    val title: String,
    val description: String,
    val audioUrl: String?,      // full URL built from PB file field
    val imageUrl: String?,      // full URL, episode-level override
    val durationSeconds: Int,
    val publishedAt: String,
)
