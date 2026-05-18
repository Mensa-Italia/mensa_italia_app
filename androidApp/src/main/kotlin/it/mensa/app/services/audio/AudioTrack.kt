package it.mensa.app.services.audio

/**
 * AudioTrack — data model for a playable media item.
 *
 * Feature-agnostic descriptor. Any feature (Quid narrations, Podcasts,
 * future addons) constructs an AudioTrack from its own domain models and
 * hands it to AudioPlayerController. The service knows nothing about feature semantics.
 *
 * Recommended id pattern: "<feature>-<entity>-<id>" to avoid collisions.
 */
data class AudioTrack(
    /** Stable identifier — collision-resistant across features */
    val id: String,
    val title: String,
    /** Free-form subtitle composed by the caller, e.g. "Quid · letto da Giulia" */
    val subtitle: String,
    /** URL string for artwork image (nullable) */
    val artworkUrl: String? = null,
    /** URL string for media stream/file */
    val mediaUrl: String,
    /** Content kind */
    val kind: AudioTrackKind = AudioTrackKind.Generic,
    /** Originating feature sourceId (e.g. PocketBase record id) */
    val sourceId: String? = null,
    /**
     * Optional deep link URL ("mensa://...") to navigate back to the originating
     * screen from the full-screen now-playing view.
     */
    val sourceUrl: String? = null,
    /**
     * Hint for total duration in milliseconds. Used for MediaMetadata before the
     * player resolves the actual duration from the stream. Null if unknown.
     */
    val durationHint: Long? = null,
)

enum class AudioTrackKind {
    Podcast,
    Narration,
    Generic,
}
