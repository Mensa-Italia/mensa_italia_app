package it.mensa.app.services.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CacheBitmapLoader
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.MoreExecutors
import it.mensa.app.support.MensaAuthInterceptor
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

/**
 * PlaybackService — Media3 MediaSessionService for background audio playback.
 *
 * Hosts the [MediaSession] and manages the [ExoPlayer] instance.
 * Media3 automatically handles:
 *  - Foreground notification with MediaStyle (play/pause/prev/next actions)
 *  - Lock screen controls
 *  - Bluetooth/headset media button events
 *  - Audio focus negotiation
 *  - Wear OS notification
 *
 * Declared in AndroidManifest.xml with:
 *   foregroundServiceType="mediaPlayback"
 *   intent-filter: androidx.media3.session.MediaSessionService
 *   Permission: FOREGROUND_SERVICE_MEDIA_PLAYBACK
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .setUsage(C.USAGE_MEDIA)
            .build()

        // Our files are only served to authenticated requests: with the default
        // data source the backend answered 404 and nothing played. OkHttp reads
        // the token at every request (seeks included) and drops it on the 307
        // to S3. The artwork on the notification/lock screen goes through the
        // same client.
        val httpClient = OkHttpClient.Builder()
            .addInterceptor(MensaAuthInterceptor())
            .build()
        val dataSourceFactory = OkHttpDataSource.Factory(httpClient)

        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        // PendingIntent that brings the app back to foreground when the user
        // taps the notification.
        val sessionActivityIntent = PendingIntent.getActivity(
            this,
            0,
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val bitmapLoader = CacheBitmapLoader(
            DataSourceBitmapLoader(
                MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor()),
                dataSourceFactory,
            ),
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityIntent)
            .setBitmapLoader(bitmapLoader)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return
        // Stop service when task is removed if player is not playing or in an
        // idle / ended state. This mirrors iOS behaviour where audio stops when
        // the app is force-quit.
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
