package it.mensa.app.services.audio

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * AudioPlayerController — singleton observable bridge between the UI and Media3.
 *
 * ## Usage from other features (Quid, Podcasts, …):
 * ```kotlin
 * val controller = koinInject<AudioPlayerController>()
 * controller.play(AudioTrack(
 *     id = "quid-narration-abc123",
 *     title = "Titolo articolo",
 *     subtitle = "Quid · letto da Giulia",
 *     artworkUrl = "https://…/cover.jpg",
 *     mediaUrl  = "https://…/audio.mp3",
 *     kind      = AudioTrackKind.Narration,
 * ))
 * ```
 *
 * ## Lifecycle wiring (MainActivity):
 * - `onStart` → `audioPlayerController.bind(this)`
 * - `onStop`  → `audioPlayerController.unbind()`
 *
 * ## Public API:
 * - `play(track)`, `enqueue(track)`, `playQueue(tracks, startIndex)`
 * - `togglePlayPause()`, `seek(seconds)`, `seekBy(delta)`
 * - `next()`, `previous()`, `clear()`
 * - `presentFullPlayer()`, `dismissFullPlayer()`
 * - StateFlows: `currentTrack`, `isPlaying`, `currentTime`, `duration`, `progress`,
 *   `queue`, `hasNext`, `hasPrevious`, `isPresentingFullPlayer`
 */
class AudioPlayerController(private val context: Context) {

    // ─── Coroutine scope ──────────────────────────────────────────────────────
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // ─── State ────────────────────────────────────────────────────────────────

    private val _currentTrack = MutableStateFlow<AudioTrack?>(null)
    val currentTrack: StateFlow<AudioTrack?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentTime = MutableStateFlow(0f)
    /** Current playback position in seconds */
    val currentTime: StateFlow<Float> = _currentTime.asStateFlow()

    private val _duration = MutableStateFlow(0f)
    /** Track duration in seconds (resolved from stream) */
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private val _progress = MutableStateFlow(0f)
    /** Playback progress 0.0–1.0 */
    val progress: StateFlow<Float> = _progress.asStateFlow()

    private val _queue = MutableStateFlow<List<AudioTrack>>(emptyList())
    /** Full playback queue (current track + upNext) */
    val queue: StateFlow<List<AudioTrack>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)

    private val _hasNext = MutableStateFlow(false)
    val hasNext: StateFlow<Boolean> = _hasNext.asStateFlow()

    private val _hasPrevious = MutableStateFlow(false)
    val hasPrevious: StateFlow<Boolean> = _hasPrevious.asStateFlow()

    private val _isPresentingFullPlayer = MutableStateFlow(false)
    /** True when the full-screen now-playing sheet should be visible */
    val isPresentingFullPlayer: StateFlow<Boolean> = _isPresentingFullPlayer.asStateFlow()

    // ─── MediaController connection ───────────────────────────────────────────

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var positionPollingJob: Job? = null

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val idx = controller?.currentMediaItemIndex ?: return
            val q = _queue.value
            if (idx in q.indices) {
                _currentTrack.value = q[idx]
                _currentIndex.value = idx
                updateNavState()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                // End of queue: rewind to start but don't call clear() — keep
                // mini-player visible so user can restart (mirrors iOS behaviour).
                controller?.seekTo(0, 0)
                controller?.pause()
            }
        }
    }

    /**
     * Bind MediaController to PlaybackService. Call from Activity.onStart.
     * Safe to call multiple times — noop when already connected.
     */
    fun bind(context: Context) {
        if (controller != null) return
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java),
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener(
            {
                val ctrl = runCatching { controllerFuture?.get() }.getOrNull() ?: return@addListener
                controller = ctrl
                ctrl.addListener(playerListener)
                // Restore state after reconnect (e.g. process restart)
                syncStateFromController(ctrl)
                startPositionPolling()
            },
            { runnable -> runnable.run() },
        )
    }

    /**
     * Unbind MediaController. Call from Activity.onStop.
     * The PlaybackService and playback continue in the background.
     */
    fun unbind() {
        positionPollingJob?.cancel()
        positionPollingJob = null
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }

    // ─── Playback commands ────────────────────────────────────────────────────

    /**
     * Play a single track, replacing the current queue.
     * If the same track is already loaded, resumes without reloading.
     */
    fun play(track: AudioTrack) {
        if (track.id == _currentTrack.value?.id) {
            controller?.play()
            return
        }
        _queue.value = listOf(track)
        _currentIndex.value = 0
        _currentTrack.value = track
        updateNavState()
        val ctrl = controller ?: return
        ctrl.setMediaItem(track.toMediaItem())
        ctrl.prepare()
        ctrl.play()
    }

    /**
     * Append a track to the end of the queue.
     * If nothing is playing, starts playback immediately.
     */
    fun enqueue(track: AudioTrack) {
        val existing = _queue.value
        if (existing.isEmpty()) {
            play(track)
            return
        }
        _queue.value = existing + track
        updateNavState()
        controller?.addMediaItem(track.toMediaItem())
    }

    /**
     * Replace the entire queue and start playing from [startIndex].
     */
    fun playQueue(tracks: List<AudioTrack>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.value = tracks
        _currentIndex.value = clampedIndex
        _currentTrack.value = tracks[clampedIndex]
        updateNavState()
        val ctrl = controller ?: return
        ctrl.setMediaItems(tracks.map { it.toMediaItem() }, clampedIndex, 0L)
        ctrl.prepare()
        ctrl.play()
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    /** Pause playback. Alias for callers that need explicit pause. */
    fun pause() {
        controller?.pause()
        _isPlaying.value = false
    }

    /** Resume playback. Alias for callers that need explicit resume. */
    fun resume() {
        controller?.play()
    }

    /** Seek to an absolute position in seconds */
    fun seek(seconds: Float) {
        val clampedMs = (seconds * 1000L).toLong().coerceAtLeast(0L)
        controller?.seekTo(clampedMs)
        _currentTime.value = seconds
        val dur = _duration.value
        if (dur > 0f) _progress.value = (seconds / dur).coerceIn(0f, 1f)
    }

    /** Skip forward/backward by [delta] seconds (positive = forward, negative = backward) */
    fun seekBy(delta: Float) {
        val newPos = (_currentTime.value + delta).coerceAtLeast(0f)
        seek(newPos)
    }

    fun next() {
        if (!_hasNext.value) return
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        // iOS behaviour: if more than 3s in, restart current track; else go to prev
        if (_currentTime.value > 3f) {
            seek(0f)
            return
        }
        if (_hasPrevious.value) {
            controller?.seekToPreviousMediaItem()
        } else {
            seek(0f)
        }
    }

    /**
     * Stops playback, clears the queue, dismisses the full player.
     * Mini-player disappears because currentTrack becomes null.
     */
    fun clear() {
        controller?.stop()
        controller?.clearMediaItems()
        _currentTrack.value = null
        _queue.value = emptyList()
        _currentIndex.value = 0
        _isPlaying.value = false
        _progress.value = 0f
        _currentTime.value = 0f
        _duration.value = 0f
        _hasNext.value = false
        _hasPrevious.value = false
        _isPresentingFullPlayer.value = false
    }

    fun presentFullPlayer() {
        if (_currentTrack.value != null) _isPresentingFullPlayer.value = true
    }

    fun dismissFullPlayer() {
        _isPresentingFullPlayer.value = false
    }

    // ─── Private helpers ──────────────────────────────────────────────────────

    private fun AudioTrack.toMediaItem(): MediaItem {
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(subtitle)
            .setAlbumTitle(subtitle)
            .apply {
                artworkUrl?.let { setArtworkUri(Uri.parse(it)) }
                durationHint?.let { setDurationMs(it) }
            }
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(mediaUrl)
            .setMediaMetadata(meta)
            .build()
    }

    private fun syncStateFromController(ctrl: MediaController) {
        _isPlaying.value = ctrl.isPlaying
        val idx = ctrl.currentMediaItemIndex
        if (_queue.value.isNotEmpty() && idx in _queue.value.indices) {
            _currentTrack.value = _queue.value[idx]
            _currentIndex.value = idx
            updateNavState()
        }
    }

    private fun updateNavState() {
        val q = _queue.value
        val idx = _currentIndex.value
        _hasNext.value = idx < q.lastIndex
        _hasPrevious.value = idx > 0
    }

    /** Position polling every 500 ms — same cadence as iOS. */
    private fun startPositionPolling() {
        positionPollingJob?.cancel()
        positionPollingJob = scope.launch {
            while (true) {
                val ctrl = controller
                if (ctrl != null && ctrl.isPlaying) {
                    val posMs = ctrl.currentPosition
                    val durMs = ctrl.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L
                    val posSec = posMs / 1000f
                    val durSec = durMs / 1000f
                    _currentTime.value = posSec
                    _duration.value = durSec
                    _progress.value = if (durSec > 0f) (posSec / durSec).coerceIn(0f, 1f) else 0f
                }
                delay(500L)
            }
        }
    }
}
