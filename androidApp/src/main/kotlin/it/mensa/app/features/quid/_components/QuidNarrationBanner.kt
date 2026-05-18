package it.mensa.app.features.quid._components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.services.audio.AudioTrack
import it.mensa.app.services.audio.AudioTrackKind
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.model.QuidArticleAudio
import org.koin.compose.koinInject

/**
 * QuidNarrationBanner — Apple Podcasts-style narration banner.
 */
@Composable
fun QuidNarrationBanner(
    audio: QuidArticleAudio,
    articleId: Long,
    articleTitle: String,
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    audioController: AudioPlayerController = koinInject(),
) {
    val currentTrack by audioController.currentTrack.collectAsState()
    val isPlaying by audioController.isPlaying.collectAsState()
    val progress by audioController.progress.collectAsState()

    val trackId = quidTrackId(articleId)
    val isThisLoaded = currentTrack?.id == trackId
    val isThisPlaying = isThisLoaded && isPlaying
    val displayProgress = if (isThisLoaded) progress else 0f

    val formattedDuration = run {
        val dur = audio.durationSeconds
        val m = dur / 60
        val s = dur % 60
        "%d:%02d".format(m, s)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artwork thumbnail
                if (artworkUrl != null) {
                    CachedAsyncImage(
                        model = artworkUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.GraphicEq,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                // Text labels
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    Text(
                        text = tr("addons.quid.audio.listen", fallback = "Ascolta"),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "letto da ${audio.voice} · $formattedDuration",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }

                // Play/pause button
                IconButton(
                    onClick = {
                        if (isThisLoaded) {
                            if (isPlaying) audioController.pause() else audioController.resume()
                        } else {
                            val track = buildAudioTrack(audio, articleId, articleTitle, artworkUrl)
                            audioController.play(track)
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                            CircleShape,
                        ),
                ) {
                    Icon(
                        imageVector = if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isThisPlaying)
                            tr("addons.quid.audio.pause", fallback = "Pausa")
                        else
                            tr("addons.quid.audio.play", fallback = "Riproduci"),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(displayProgress.coerceIn(0f, 1f))
                        .height(2.dp)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }
    }
}

// ─── Factory helpers ──────────────────────────────────────────────────────────

fun quidTrackId(articleId: Long): String = "quid-article-$articleId"

fun buildAudioTrack(
    audio: QuidArticleAudio,
    articleId: Long,
    articleTitle: String,
    artworkUrl: String?,
): AudioTrack = AudioTrack(
    id = quidTrackId(articleId),
    title = articleTitle,
    subtitle = "Quid · letto da ${audio.voice}",
    artworkUrl = artworkUrl,
    mediaUrl = audio.audioUrl,
    kind = AudioTrackKind.Narration,
    sourceId = audio.id,
    sourceUrl = "mensa://quid-article/$articleId",
    durationHint = audio.durationSeconds.toLong() * 1000L,
)
