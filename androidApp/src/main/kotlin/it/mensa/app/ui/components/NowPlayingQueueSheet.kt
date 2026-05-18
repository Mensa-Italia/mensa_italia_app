package it.mensa.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.services.audio.AudioTrack
import it.mensa.app.ui.theme.LightPrimary

/**
 * NowPlayingQueueSheet — ModalBottomSheet showing the current track + upNext list.
 *
 * Replica of iOS QueueListSheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingQueueSheet(
    controller: AudioPlayerController,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val queue by controller.queue.collectAsStateWithLifecycle()
    val currentTrack by controller.currentTrack.collectAsStateWithLifecycle()
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val currentIndex = queue.indexOfFirst { it.id == currentTrack?.id }.coerceAtLeast(0)
    val upNext = if (currentIndex + 1 <= queue.lastIndex) queue.subList(currentIndex + 1, queue.size) else emptyList()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { /* custom drag indicator in header */ },
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Coda di riproduzione",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) {
                    Text("Fine")
                }
            }
            HorizontalDivider()

            LazyColumn {
                // In riproduzione
                currentTrack?.let { track ->
                    item {
                        Text(
                            text = "In riproduzione",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                        QueueTrackRow(
                            track = track,
                            isCurrent = true,
                            isPlaying = isPlaying,
                        )
                    }
                }

                // UpNext
                if (upNext.isNotEmpty()) {
                    item {
                        Text(
                            text = "In coda · ${upNext.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(upNext, key = { it.id }) { track ->
                        QueueTrackRow(
                            track = track,
                            isCurrent = false,
                            isPlaying = false,
                        )
                    }
                }

                item { Spacer(Modifier.padding(bottom = 32.dp)) }
            }
        }
    }
}

@Composable
private fun QueueTrackRow(
    track: AudioTrack,
    isCurrent: Boolean,
    isPlaying: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Artwork
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            if (track.artworkUrl != null) {
                CachedAsyncImage(
                    model = track.artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (isCurrent) LightPrimary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            Text(
                text = track.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }

        // Waveform animation indicator for current playing track
        if (isCurrent) {
            Icon(
                imageVector = Icons.Filled.GraphicEq,
                contentDescription = if (isPlaying) "In riproduzione" else "In pausa",
                modifier = Modifier.size(18.dp),
                tint = LightPrimary,
            )
        }
    }
}
