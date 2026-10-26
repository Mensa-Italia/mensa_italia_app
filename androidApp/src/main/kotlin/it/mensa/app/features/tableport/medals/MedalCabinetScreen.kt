package it.mensa.app.features.tableport.medals

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.tr
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private object Brass {
    val hi = Color(0xFFF2D68F)
    val mid = Color(0xFFB8914A)
    val lo = Color(0xFF6B4F1F)
}

private object Stone {
    val hi = Color(0xFF575757)
    val mid = Color(0xFF363636)
    val lo = Color(0xFF1C1C1C)
}

private val SpotlightWarm = Color(0xFFFFF2D1)

/** A dim gallery: dark walls, a warm wash from the ceiling. */
private val Museum = Brush.verticalGradient(
    listOf(Color(0xFF1A1B21), Color(0xFF0D0E12), Color(0xFF06080A)),
)

/**
 * MedalCabinetScreen — the medal collection as a museum room: each medal
 * floats in its own glass case, on a stone pedestal with a brass plaque,
 * under a spotlight. Earned medals are lit; the rest wait in the dark as
 * dull silhouettes. Tapping one brings it forward ([MedalShowcase]).
 * Mirrors iOS `MedalCabinetView`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedalCabinetScreen(onNavigateBack: () -> Unit) {
    val state = rememberMedals()
    val medals = state.medals
    var selected by remember { mutableStateOf<Medal?>(null) }
    val navBars = WindowInsets.navigationBars.asPaddingValues()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Museum)
            .drawWithContent {
                // The ceiling light's warm wash.
                drawRect(
                    Brush.radialGradient(
                        listOf(SpotlightWarm.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width / 2f, 0f),
                        radius = 520.dp.toPx(),
                    ),
                )
                drawContent()
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            // No title: the room speaks for itself.
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("app.back", "Indietro"),
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalArrangement = Arrangement.spacedBy(36.dp),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 8.dp,
                    bottom = 40.dp + navBars.calculateBottomPadding(),
                ),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Header(medals)
                }
                items(medals, key = { it.id }) { medal ->
                    MedalDisplayCase(medal = medal, onClick = { selected = medal })
                }
            }
        }

        if (medals.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.failed) {
                    Text(
                        text = tr("medals.load_error", "Non riesco a leggere le medaglie. Riprova più tardi."),
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }
        }

        AnimatedVisibility(
            visible = selected != null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            // Keeps showing the medal while it fades out.
            val medal = remember(selected) { selected } ?: return@AnimatedVisibility
            MedalShowcase(medal = medal, onDismiss = { selected = null })
        }
    }
}

@Composable
private fun Header(medals: List<Medal>) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "${medals.count { it.isEarned }} / ${medals.size}",
            // The app's own face; the brass is what ties it to the room.
            style = TextStyle(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 46.sp,
                brush = Brush.verticalGradient(listOf(Brass.hi, Brass.mid)),
                shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 2f), 0f),
            ),
        )
        Text(
            text = tr("medals.count_caption", "Medaglie in teca").uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.5.sp,
            color = Color.White.copy(alpha = 0.5f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.padding(top = 4.dp)) {
            MedalTier.entries.forEach { tier ->
                val tierLabel = tier.label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.semantics { contentDescription = tierLabel },
                ) {
                    MedalArtwork(tier = tier, icon = Icons.Filled.Star, size = 18.dp)
                    Text(
                        text = "${medals.count { it.tier == tier && it.isEarned }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }
    }
}

// ─── Case ────────────────────────────────────────────────────────────────────

@Composable
private fun MedalDisplayCase(medal: Medal, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(
        modifier = Modifier
            .scale(if (pressed) 0.97f else 1f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = medal.title },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(168.dp)
                .padding(horizontal = 8.dp),
        ) {
            if (medal.isEarned) {
                // Light pooling down from the top of the case: soft all round
                // without a blur.
                Box(
                    Modifier
                        .matchParentSize()
                        .drawWithContent {
                            drawRect(
                                Brush.radialGradient(
                                    listOf(SpotlightWarm.copy(alpha = 0.26f), Color.Transparent),
                                    center = Offset(size.width / 2f, 0f),
                                    radius = size.height * 0.85f,
                                ),
                            )
                        },
                )
            }
            FloatingMedal(medal = medal, size = 84.dp, modifier = Modifier.matchParentSize())
            GlassCase(Modifier.matchParentSize())
        }
        MedalPedestal(plaque = medal.title)
    }
}

/** Light falling from a narrow source above onto the pedestal. */
@Composable
private fun Spotlight(modifier: Modifier = Modifier, alpha: Float = 0.24f) {
    // Feathered sides drawn as a gradient, in place of a blur.
    Canvas(
        modifier
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Black, Color.Transparent)),
                    blendMode = BlendMode.DstIn,
                )
            },
    ) {
        val w = size.width
        val path = Path().apply {
            moveTo(w / 2f - w * 0.1f, 0f)
            lineTo(w / 2f + w * 0.1f, 0f)
            lineTo(w, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, Brush.verticalGradient(listOf(SpotlightWarm.copy(alpha = alpha), Color.Transparent)))
    }
}

/**
 * The medal hovering above the pedestal, bobbing slowly, its shadow on the
 * floor of the case shrinking as it rises.
 */
@Composable
private fun FloatingMedal(medal: Medal, size: Dp, modifier: Modifier = Modifier) {
    // So no two medals bob in step.
    val phase = remember(medal.id) { (medal.id.sumOf { it.code } % 628) / 100f }
    val time = rememberInfiniteTransition(label = "bob").animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(4_500, easing = LinearEasing), RepeatMode.Restart),
        label = "bobTime",
    )
    // The animated value is read only inside graphicsLayer blocks: each
    // frame moves the layers without recomposing (or redrawing) the medal.
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        MedalArtwork(
            tier = medal.tier,
            icon = medal.icon,
            size = size,
            locked = !medal.isEarned,
            modifier = Modifier
                .graphicsLayer { translationY = -6.dp.toPx() * sin(time.value + phase) }
                .shadow(8.dp, CircleShape, clip = false),
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .padding(bottom = 2.dp)
                .width(size * 0.8f)
                .height(12.dp)
                .graphicsLayer { scaleX = 1f - 0.06f * sin(time.value + phase) }
                .background(
                    Brush.radialGradient(
                        listOf(Color.Black.copy(alpha = if (medal.isEarned) 0.55f else 0.35f), Color.Transparent),
                    ),
                ),
        )
    }
}

/** The glass box: faintly tinted, lit along its edges, with reflections. */
@Composable
private fun GlassCase(modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f), Color.White.copy(alpha = 0.06f)),
                ),
            )
            .drawWithContent {
                drawContent()
                val w = size.width
                val h = size.height
                // A broad streak and a thin one, across the pane.
                rotate(22f) {
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.16f), Color.Transparent),
                            startX = w * 0.21f,
                            endX = w * 0.43f,
                        ),
                        topLeft = Offset(w * 0.21f, -h * 0.3f),
                        size = Size(w * 0.22f, h * 1.6f),
                    )
                    drawRect(
                        Color.White.copy(alpha = 0.10f),
                        topLeft = Offset(w * 0.54f, -h * 0.3f),
                        size = Size(w * 0.04f, h * 1.6f),
                    )
                }
                // The top pane, seen edge-on.
                drawRect(
                    Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(12.dp.toPx(), 2.dp.toPx()),
                    size = Size(w - 24.dp.toPx(), 1.5.dp.toPx()),
                )
            }
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.18f), Color.White.copy(alpha = 0.08f)),
                ),
                shape,
            ),
    )
}

// ─── Pedestal ────────────────────────────────────────────────────────────────

/** Dark stone: a lip the case stands on, the block with its plaque, a base. */
@Composable
private fun MedalPedestal(plaque: String, modifier: Modifier = Modifier, scale: Float = 1f) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = (4 * scale).dp)
                .height((9 * scale).dp)
                .clip(RoundedCornerShape((3 * scale).dp))
                .background(Brush.verticalGradient(listOf(Stone.hi, Stone.mid))),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = (12 * scale).dp)
                .height((46 * scale).dp)
                .background(Brush.verticalGradient(listOf(Stone.mid, Stone.lo)))
                .drawWithContent {
                    drawContent()
                    // Rounded sides: light on the left face, dark on the right.
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.07f), Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                        ),
                    )
                    // The lip's shadow on the block.
                    drawRect(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent),
                            endY = 6.dp.toPx() * scale,
                        ),
                        size = Size(size.width, 6.dp.toPx() * scale),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            BrassPlaque(text = plaque, scale = scale, modifier = Modifier.padding(horizontal = (14 * scale).dp))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height((9 * scale).dp)
                .shadow((8 * scale).dp, RoundedCornerShape((3 * scale).dp))
                .background(Brush.verticalGradient(listOf(Stone.mid, Stone.lo))),
        )
    }
}

/** An engraved brass plate with two screws. */
@Composable
private fun BrassPlaque(text: String, scale: Float, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape((2 * scale).dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height((20 * scale).dp)
            .shadow((2 * scale).dp, shape)
            .background(Brush.verticalGradient(listOf(Brass.hi, Brass.mid)), shape)
            .border(0.5.dp, Brass.lo.copy(alpha = 0.6f), shape)
            .padding(horizontal = (4 * scale).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Screw(scale)
        Text(
            text = text.uppercase(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = TextStyle(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = (8.5f * scale).sp,
                letterSpacing = (1f * scale).sp,
                color = Brass.lo,
                // Engraved: the lower lip of each letter catches the light.
                shadow = Shadow(Brass.hi.copy(alpha = 0.9f), Offset(0f, 1.2f * scale), 0f),
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = (4 * scale).dp),
        )
        Screw(scale)
    }
}

@Composable
private fun Screw(scale: Float) {
    Box(
        Modifier
            .size((3.5f * scale).dp)
            .background(Brush.radialGradient(listOf(Brass.hi, Brass.lo)), CircleShape),
    )
}

// ─── Showcase ────────────────────────────────────────────────────────────────

/**
 * One medal taken out of the cabinet: large, on its pedestal under the
 * spotlight, turned by dragging to show the engraved reverse.
 */
@Composable
private fun MedalShowcase(medal: Medal, onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    val angle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Museum)
            // Swallows taps so the cabinet below doesn't get them.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                // Settles on whichever face is nearer.
                                val target = (angle.value / 180f).roundToInt() * 180f
                                scope.launch { angle.animateTo(target, spring(dampingRatio = 0.72f, stiffness = 120f)) }
                            },
                        ) { _, dragAmount ->
                            scope.launch { angle.snapTo(angle.value + dragAmount / density * 0.8f) }
                        }
                    }
                    .semantics { contentDescription = medal.title },
                contentAlignment = Alignment.BottomCenter,
            ) {
                if (medal.isEarned) {
                    Spotlight(Modifier.matchParentSize().padding(horizontal = 20.dp), alpha = 0.32f)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SpinningMedal(
                        angle = angle.value,
                        medal = medal,
                        size = 200.dp,
                        reverseCaption = medal.tier.label.uppercase(),
                        modifier = Modifier.padding(top = 40.dp, bottom = 26.dp),
                    )
                    Box(
                        Modifier
                            .padding(bottom = 6.dp)
                            .size(130.dp, 12.dp)
                            .then(if (Build.VERSION.SDK_INT >= 31) Modifier.blur(6.dp) else Modifier)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                    )
                    MedalPedestal(plaque = medal.title, scale = 1.5f, modifier = Modifier.width(260.dp))
                }
            }

            Spacer(Modifier.height(26.dp))
            Text(
                text = medal.tier.label.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = medal.tier.metal.hi,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = medal.title,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = medal.detail,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            if (medal.isEarned) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Brass.hi, modifier = Modifier.size(18.dp))
                    Text(
                        text = tr("medals.earned", "Ottenuta"),
                        fontWeight = FontWeight.SemiBold,
                        color = Brass.hi,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
            // How far it is to the next metal; nothing more at the top.
            val nextTier = medal.nextTier
            val nextThreshold = medal.nextThreshold
            if (nextTier != null && nextThreshold != null) {
                Column(Modifier.widthIn(max = 260.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    LinearProgressIndicator(
                        progress = { medal.progress },
                        color = nextTier.metal.mid,
                        trackColor = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = tr("medals.next", "Prossimo traguardo: %1\$@ a %2\$d")
                            .replace("%1\$@", nextTier.label)
                            .replace("%2\$d", "$nextThreshold"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Text(
                        text = tr("medals.progress", "%1\$d su %2\$d")
                            .replace("%1\$d", "${medal.value}")
                            .replace("%2\$d", "$nextThreshold"),
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.45f),
                    )
                }
            } else if (medal.isEarned) {
                Text(
                    text = tr("medals.max_tier", "Livello massimo raggiunto"),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White.copy(alpha = 0.55f),
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = tr("medals.spin_hint", "Trascina per girarla"),
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.35f),
            )
        }

        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(12.dp)
                .background(Color.White.copy(alpha = 0.12f), CircleShape),
        ) {
            Icon(Icons.Filled.Close, contentDescription = tr("app.close", "Chiudi"), tint = Color.White)
        }
    }
}

/**
 * The medal turned about its vertical axis: the reverse shows once it's
 * past edge-on, and the coin's rim shows its thickness on the way round.
 */
@Composable
private fun SpinningMedal(
    angle: Float,
    medal: Medal,
    size: Dp,
    reverseCaption: String,
    modifier: Modifier = Modifier,
) {
    val radians = angle * PI.toFloat() / 180f
    val showsBack = cos(radians) < 0f
    val metal = medal.tier.metal
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(size * 0.07f * abs(sin(radians)))
                .height(size * 0.96f)
                .background(Brush.verticalGradient(listOf(metal.lo, metal.mid, metal.lo)), CircleShape),
        )
        MedalArtwork(
            tier = medal.tier,
            icon = medal.icon,
            size = size,
            locked = !medal.isEarned,
            sheen = sin(radians),
            reverse = if (showsBack) MedalReverse(medal.title, reverseCaption) else null,
            modifier = Modifier
                .graphicsLayer {
                    rotationY = angle
                    cameraDistance = 14f * density
                    // The reverse is drawn mirrored, so it reads right once turned.
                    scaleX = if (showsBack) -1f else 1f
                }
                .shadow(14.dp, CircleShape, clip = false),
        )
    }
}
