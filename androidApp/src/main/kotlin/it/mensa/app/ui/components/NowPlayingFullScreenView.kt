package it.mensa.app.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import android.content.Context
import android.media.AudioManager
import androidx.activity.compose.BackHandler
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.services.audio.AudioTrack
import org.koin.compose.koinInject
import kotlin.math.max
import kotlin.math.min

/**
 * NowPlayingFullScreenView — full-screen now-playing modal.
 *
 * Replica of iOS NowPlayingFullScreenView.swift:
 * - Black backdrop + blurred artwork background + gradient overlay
 * - Pull indicator capsule
 * - Top bar: dismiss + subtitle + options menu
 * - Artwork square with scale spring (0.92 paused → 1.0 playing)
 * - Title + subtitle block
 * - Custom scrubber with thumb scale on drag
 * - Timestamps: current / -remaining (tabular digits)
 * - Transport controls: backward 15s (or prev) / play-pause / forward 15s (or next)
 * - Secondary actions: system volume slider / queue list
 *
 * Implemented as a plain full-screen overlay in the Activity hierarchy (last
 * sibling in MainAppShell) with a BackHandler — a Dialog window can't extend
 * behind the status bar / display cutout.
 */
@Composable
fun NowPlayingFullScreenView(
    controller: AudioPlayerController = koinInject(),
    onDismiss: () -> Unit,
) {
    val currentTrack by controller.currentTrack.collectAsStateWithLifecycle()
    val track = currentTrack ?: return

    // Overlay full-screen nella gerarchia dell'Activity, NON un Dialog: la
    // finestra dei Dialog non si estende mai davvero dietro status bar e
    // cutout (resta una banda chiara in alto), mentre la finestra
    // dell'Activity è già edge-to-edge. Il chiamante (MainAppShell) lo emette
    // come ultimo sibling, quindi copre l'intera shell.
    BackHandler(onBack = onDismiss)
    NowPlayingContent(
        track = track,
        controller = controller,
        onDismiss = onDismiss,
    )
}

@Composable
private fun NowPlayingContent(
    track: AudioTrack,
    controller: AudioPlayerController,
    onDismiss: () -> Unit,
) {
    val isPlaying by controller.isPlaying.collectAsStateWithLifecycle()
    val currentTime by controller.currentTime.collectAsStateWithLifecycle()
    val duration by controller.duration.collectAsStateWithLifecycle()
    val queue by controller.queue.collectAsStateWithLifecycle()
    val hasNext by controller.hasNext.collectAsStateWithLifecycle()
    val hasPrevious by controller.hasPrevious.collectAsStateWithLifecycle()

    var showQueue by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Blocca ogni input verso la shell sottostante: da overlay (non
            // più Dialog) senza questo i tocchi nelle aree vuote filtrerebbero
            // ai contenuti sotto.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        // Blurred artwork backdrop
        BlurredArtworkBackdrop(
            artworkUrl = track.artworkUrl,
            modifier = Modifier.fillMaxSize(),
        )

        // Gradient overlay (matching iOS: black 0 → 0.35 → 0.75)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0f),
                        0.4f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.75f),
                    )
                ),
        )

        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 0.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pull indicator / drag handle
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 5.dp)
                    .background(
                        Color.White.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(50),
                    ),
            )

            // Top bar
            TopBar(
                track = track,
                onDismiss = onDismiss,
                onStop = {
                    controller.clear()
                    onDismiss()
                },
                showOptionsMenu = showOptionsMenu,
                onToggleMenu = { showOptionsMenu = !showOptionsMenu },
                onDismissMenu = { showOptionsMenu = false },
            )

            Spacer(Modifier.weight(1f))

            // Artwork
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                val artworkSize = min(maxWidth, maxHeight * 0.42f)
                val artworkScale by animateFloatAsState(
                    targetValue = if (isPlaying) 1f else 0.92f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow,
                    ),
                    label = "artwork_scale",
                )
                ArtworkView(
                    track = track,
                    maxSide = artworkSize,
                    modifier = Modifier.scale(artworkScale),
                )
            }

            Spacer(Modifier.weight(1f))

            // Title block
            TitleBlock(
                track = track,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Custom scrubber
            AudioScrubber(
                duration = max(duration.toDouble(), 0.01),
                currentTime = currentTime.toDouble(),
                onSeek = { newSecs -> controller.seek(newSecs.toFloat()) },
                modifier = Modifier.fillMaxWidth(),
            )

            // Timestamps
            TimestampsRow(
                currentTime = currentTime,
                duration = duration,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            )

            Spacer(Modifier.height(12.dp))

            // Transport controls
            TransportControls(
                isPlaying = isPlaying,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                isMultiTrack = queue.size > 1,
                onPrev = { controller.previous() },
                onTogglePlay = { controller.togglePlayPause() },
                onNext = { controller.next() },
                onSkipBack = { controller.seekBy(-15f) },
                onSkipForward = { controller.seekBy(15f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
            )

            Spacer(Modifier.height(20.dp))

            // Secondary actions
            SecondaryActions(
                queueCount = queue.size,
                onQueueTap = { showQueue = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 48.dp),
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showQueue) {
        NowPlayingQueueSheet(
            controller = controller,
            onDismiss = { showQueue = false },
        )
    }
}

// ─── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun BlurredArtworkBackdrop(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        if (artworkUrl != null) {
            CachedAsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(60.dp),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF184295), Color(0xFF061F2E))
                        )
                    ),
            )
        }
        // Additional darkening for readability (API < 31 fallback since blur may be less effective)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
        )
    }
}

@Composable
private fun TopBar(
    track: AudioTrack,
    onDismiss: () -> Unit,
    onStop: () -> Unit,
    showOptionsMenu: Boolean,
    onToggleMenu: () -> Unit,
    onDismissMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Dismiss button
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .size(36.dp)
                .background(Color.White.copy(alpha = 0.08f), CircleShape),
        ) {
            Icon(
                imageVector = Icons.Filled.KeyboardArrowDown,
                contentDescription = "Chiudi",
                tint = Color.White,
                modifier = Modifier.size(22.dp),
            )
        }

        // Center subtitle
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "In riproduzione da",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
            Text(
                text = track.subtitle,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
        }

        // Options menu
        Box {
            IconButton(
                onClick = onToggleMenu,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.08f), CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.MoreVert,
                    contentDescription = "Altre opzioni",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = showOptionsMenu,
                onDismissRequest = onDismissMenu,
            ) {
                DropdownMenuItem(
                    text = { Text("Interrompi riproduzione") },
                    onClick = {
                        onDismissMenu()
                        onStop()
                    },
                )
            }
        }
    }
}

@Composable
private fun ArtworkView(
    track: AudioTrack,
    maxSide: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    val artworkShape = RoundedCornerShape(12.dp)
    // Aspect ratio reale dell'immagine (come su iOS, che usa scaledToFit):
    // gli artwork landscape dei podcast non vengono croppati a quadrato.
    // Finché non è noto si parte da 1:1.
    var imageRatio by remember(track.id) { mutableFloatStateOf(1f) }
    Box(
        modifier = modifier
            .width(if (imageRatio >= 1f) maxSide else maxSide * imageRatio)
            .aspectRatio(imageRatio)
            .shadow(elevation = 24.dp, shape = artworkShape)
            .clip(artworkShape)
            .border(
                width = 0.5.dp,
                color = Color.White.copy(alpha = 0.06f),
                shape = artworkShape,
            ),
    ) {
        if (track.artworkUrl != null) {
            CachedAsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onState = { state ->
                    val size = (state as? coil3.compose.AsyncImagePainter.State.Success)
                        ?.painter?.intrinsicSize
                    if (size != null && size.height > 0f) {
                        imageRatio = size.width / size.height
                    }
                },
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0xFF184295), Color(0xFF061F2E))
                        )
                    ),
            )
        }
    }
}

@Composable
private fun TitleBlock(
    track: AudioTrack,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            color = Color.White,
            maxLines = 2,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = track.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            maxLines = 1,
        )
    }
}

@Composable
private fun TimestampsRow(
    currentTime: Float,
    duration: Float,
    modifier: Modifier = Modifier,
) {
    val remaining = max(0f, duration - currentTime)
    // Cifre tabulari (tnum) invece del Monospace di sistema: stessa stabilità
    // di larghezza senza l'aspetto "spaziato" da terminale.
    val timeStyle = MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        fontFeatureSettings = "tnum",
    )
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = formatTime(currentTime),
            style = timeStyle,
            color = Color.White.copy(alpha = 0.6f),
        )
        Text(
            text = "-${formatTime(remaining)}",
            style = timeStyle,
            color = Color.White.copy(alpha = 0.6f),
        )
    }
}

@Composable
private fun TransportControls(
    isPlaying: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    isMultiTrack: Boolean,
    onPrev: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onSkipBack: () -> Unit,
    onSkipForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // Left: prev or -15s
        if (isMultiTrack) {
            IconButton(
                onClick = onPrev,
                enabled = hasPrevious,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipPrevious,
                    contentDescription = "Traccia precedente",
                    tint = Color.White.copy(alpha = if (hasPrevious) 1f else 0.3f),
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            IconButton(onClick = onSkipBack) {
                Skip15Glyph(
                    forward = false,
                    contentDescription = "Indietro di 15 secondi",
                )
            }
        }

        // Play / pause — large circle
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(Color.White, CircleShape)
                .pointerInput(Unit) { detectTapGestures { onTogglePlay() } },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausa" else "Riproduci",
                tint = Color.Black,
                // Il triangolo del play ha baricentro ottico spostato a
                // sinistra: 2dp di offset lo ricentra nel cerchio.
                modifier = Modifier
                    .size(38.dp)
                    .offset(x = if (isPlaying) 0.dp else 2.dp),
            )
        }

        // Right: next or +15s
        if (isMultiTrack) {
            IconButton(
                onClick = onNext,
                enabled = hasNext,
            ) {
                Icon(
                    imageVector = Icons.Filled.SkipNext,
                    contentDescription = "Traccia successiva",
                    tint = Color.White.copy(alpha = if (hasNext) 1f else 0.3f),
                    modifier = Modifier.size(32.dp),
                )
            }
        } else {
            IconButton(onClick = onSkipForward) {
                Skip15Glyph(
                    forward = true,
                    contentDescription = "Avanti di 15 secondi",
                )
            }
        }
    }
}

/**
 * Glifo "salta 15s" in stile `goforward.15`/`gobackward.15` di iOS: la
 * freccia circolare di Replay (specchiata per il forward) con il numero
 * al centro. Material non ha la variante da 15 secondi (solo 5/10/30).
 */
@Composable
private fun Skip15Glyph(
    forward: Boolean,
    contentDescription: String,
) {
    Box(contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Filled.Replay,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier
                .size(32.dp)
                .graphicsLayer { if (forward) scaleX = -1f },
        )
        Text(
            text = "15",
            color = Color.White,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = 2.dp),
        )
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SecondaryActions(
    queueCount: Int,
    onQueueTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Volume di sistema (STREAM_MUSIC), speculare allo slider MPVolumeView
        // su iOS. Slider sottile bianco tra le due icone speaker.
        val context = LocalContext.current
        val audioManager = remember {
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        }
        val maxVolume = remember {
            audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        }
        var volume by remember {
            mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
            // Thumb e track custom: il thumb M3 di default (maniglia verticale)
            // stona con lo stile minimale bianco dello scrubber sopra.
            Slider(
                value = volume,
                onValueChange = {
                    volume = it
                    audioManager.setStreamVolume(
                        AudioManager.STREAM_MUSIC,
                        it.roundToInt(),
                        0,
                    )
                },
                valueRange = 0f..maxVolume.toFloat(),
                thumb = {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .shadow(elevation = 3.dp, shape = CircleShape)
                            .background(Color.White, CircleShape),
                    )
                },
                track = { state ->
                    val range = state.valueRange.endInclusive - state.valueRange.start
                    val fraction = if (range > 0f) {
                        ((state.value - state.valueRange.start) / range).coerceIn(0f, 1f)
                    } else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.25f)),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .height(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White),
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.height(8.dp))

        // Coda: sempre tappabile (anche con un solo elemento) come su iOS.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onQueueTap) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.List,
                        contentDescription = "Coda di riproduzione",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                    if (queueCount > 1) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$queueCount",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                }
            }
        }
    }
}

// ─── Custom scrubber ─────────────────────────────────────────────────────────

@Composable
private fun AudioScrubber(
    duration: Double,
    currentTime: Double,
    onSeek: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }

    val displayedFraction = if (isScrubbing) {
        (scrubValue / max(duration.toFloat(), 0.01f)).coerceIn(0f, 1f)
    } else {
        (currentTime.toFloat() / max(duration.toFloat(), 0.01f)).coerceIn(0f, 1f)
    }

    val thumbScale by animateFloatAsState(
        targetValue = if (isScrubbing) 1.4f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "scrubber_thumb_scale",
    )

    val trackHeight = if (isScrubbing) 8.dp else 4.dp
    val thumbRadius = 7.dp

    BoxWithConstraints(
        modifier = modifier
            .height(36.dp)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val trackWidth = maxWidth
        val thumbCenterX = thumbRadius + (trackWidth - thumbRadius * 2) * displayedFraction

        // Track background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(Color.White.copy(alpha = 0.2f))
                .align(Alignment.Center),
        )

        // Filled track (up to thumb)
        Box(
            modifier = Modifier
                .width(thumbCenterX)
                .height(trackHeight)
                .clip(RoundedCornerShape(50))
                .background(Color.White)
                .align(Alignment.CenterStart),
        )

        // Thumb — cresce quando si trascina, come su iOS.
        Box(
            modifier = Modifier
                .offset(x = thumbCenterX - thumbRadius)
                .size(thumbRadius * 2)
                .scale(thumbScale)
                .shadow(elevation = 4.dp, shape = CircleShape)
                .background(Color.White, CircleShape),
        )

        // Touch area: drag per lo scrubbing continuo, tap secco per saltare
        // direttamente a una posizione.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        val pct = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((pct * duration.toFloat()).toDouble())
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isScrubbing = true
                            val pct = (offset.x / size.width).coerceIn(0f, 1f)
                            scrubValue = pct * duration.toFloat()
                        },
                        onDragEnd = {
                            onSeek(scrubValue.toDouble())
                            isScrubbing = false
                        },
                        onDragCancel = { isScrubbing = false },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            val x = change.position.x
                            val pct = (x / size.width).coerceIn(0f, 1f)
                            scrubValue = pct * duration.toFloat()
                        },
                    )
                },
        )
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun formatTime(seconds: Float): String {
    if (!seconds.isFinite() || seconds < 0) return "0:00"
    val total = seconds.toInt()
    val m = total / 60
    val s = total % 60
    return "%d:%02d".format(m, s)
}
