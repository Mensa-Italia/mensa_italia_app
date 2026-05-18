package it.mensa.app.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.services.audio.AudioTrack
import it.mensa.app.ui.theme.LightPrimary
import org.koin.compose.koinInject

/**
 * MiniAudioPlayer — compact 56dp player bar above the NavigationBar.
 *
 * Replica of iOS MiniAudioPlayer.swift:
 * - Glass effect surface
 * - Artwork 40x40 rounded 6dp
 * - Title (semibold) + subtitle (caption)
 * - Play/pause button
 * - Morphing right button: paused→X, playing single→+15s, playing multi→next
 * - Progress hairline at the bottom (separate observation to avoid full re-render)
 * - Tap body → presentFullPlayer()
 *
 * Invisible when currentTrack == null.
 */
@Composable
fun MiniAudioPlayer(
    modifier: Modifier = Modifier,
    controller: AudioPlayerController = koinInject(),
) {
    val currentTrack by controller.currentTrack.collectAsStateWithLifecycle()
    val track = currentTrack ?: return

    MiniAudioPlayerContent(
        track = track,
        controller = controller,
        modifier = modifier,
    )
}

@Composable
private fun MiniAudioPlayerContent(
    track: AudioTrack,
    controller: AudioPlayerController,
    modifier: Modifier = Modifier,
) {
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val queue by controller.queue.collectAsStateWithLifecycle()
    val hasNext by controller.hasNext.collectAsStateWithLifecycle()

    val shape = RoundedCornerShape(16.dp)
    val colorScheme = MaterialTheme.colorScheme

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                    shape = shape,
                )
                .clickable { controller.presentFullPlayer() },
            shape = shape,
            color = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
            tonalElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Artwork
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colorScheme.surfaceVariant),
                ) {
                    if (track.artworkUrl != null) {
                        CachedAsyncImage(
                            model = track.artworkUrl,
                            contentDescription = null,
                            modifier = Modifier.matchParentSize(),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.Center),
                            tint = colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                // Title + subtitle
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = colorScheme.onSurface,
                    )
                    if (track.subtitle.isNotBlank()) {
                        Text(
                            text = track.subtitle,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // Play / pause
                IconButton(onClick = { controller.togglePlayPause() }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausa" else "Riproduci",
                        modifier = Modifier.size(22.dp),
                        tint = colorScheme.onSurface,
                    )
                }

                // Morphing right button
                val rightButtonState = when {
                    !isPlaying -> RightButtonState.Close
                    queue.size > 1 -> RightButtonState.Next
                    else -> RightButtonState.Skip15
                }

                AnimatedContent(
                    targetState = rightButtonState,
                    transitionSpec = {
                        (fadeIn() + scaleIn(initialScale = 0.85f)) togetherWith
                            (fadeOut() + scaleOut(targetScale = 0.85f))
                    },
                    label = "mini_player_right_btn",
                ) { state ->
                    when (state) {
                        RightButtonState.Close -> IconButton(onClick = { controller.clear() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Chiudi riproduzione",
                                modifier = Modifier.size(18.dp),
                                tint = colorScheme.onSurfaceVariant,
                            )
                        }

                        RightButtonState.Skip15 -> IconButton(onClick = { controller.seekBy(15f) }) {
                            Icon(
                                imageVector = Icons.Filled.FastForward,
                                contentDescription = "Avanti 15 secondi",
                                modifier = Modifier.size(20.dp),
                                tint = colorScheme.onSurface,
                            )
                        }

                        RightButtonState.Next -> IconButton(
                            onClick = { controller.next() },
                            enabled = hasNext,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SkipNext,
                                contentDescription = "Traccia successiva",
                                modifier = Modifier.size(20.dp),
                                tint = if (hasNext) colorScheme.onSurface
                                else colorScheme.onSurface.copy(alpha = 0.3f),
                            )
                        }
                    }
                }
            }
        }

        // Progress hairline — separate composable that reads progress to avoid
        // re-rendering the whole bar on every 500 ms tick.
        MiniProgressHairline(
            controller = controller,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
        )
    }
}

@Composable
private fun MiniProgressHairline(
    controller: AudioPlayerController,
    modifier: Modifier = Modifier,
) {
    val progress by controller.progress.collectAsStateWithLifecycle()

    Box(
        modifier = modifier.height(2.dp),
    ) {
        // Track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.Black.copy(alpha = 0.10f)),
        )
        // Filled portion
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                .height(2.dp)
                .background(LightPrimary),
        )
    }
}

private enum class RightButtonState { Close, Skip15, Next }
