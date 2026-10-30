package it.mensa.app.features.tableport.medals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.tr
import it.mensa.app.ui.theme.StageLight
import kotlin.math.PI
import kotlin.math.sin

private val CaseShape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
private val PlinthShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)

/** Dark velvet inside the case. */
private val Velvet = Brush.verticalGradient(listOf(Color(0xFF1A1F2E), Color(0xFF0D1018)))

/** Lacquered black of the plinth, lit from above. */
private val Lacquer = Brush.verticalGradient(listOf(Color(0xFF2B303D), Color(0xFF141720)))

/** Grey trim of a case whose medal isn't earned yet. */
private val DullTrim = listOf(Color(0xFF2E323C), Color(0xFF5A5F6B), Color(0xFF2E323C))

/**
 * A medal in its glass case, on a lacquered plinth: earned medals hang in a
 * light the colour of their metal, with a metal trim and their metal
 * engraved under the name; the others wait in the dark with how far along
 * they are. Mirrors iOS `MedalCase`.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MedalCase(medal: Medal, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
        label = "casePress",
    )
    val metal = medal.tier.metal
    val state = if (medal.isEarned) tr("medals.earned", "Ottenuta") else tr("medals.locked", "Non ancora ottenuta")
    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${medal.title}, $state" },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(172.dp)
                .clip(CaseShape)
                .background(Velvet)
                .drawBehind { caseLight(if (medal.isEarned) metal.hi else null) },
        ) {
            FloatingMedal(medal = medal, size = 86.dp, modifier = Modifier.fillMaxWidth().height(172.dp))
            GlassPane(Modifier.matchParentSize())
        }
        // The metal trim between case and plinth.
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .background(
                    Brush.horizontalGradient(
                        if (medal.isEarned) listOf(metal.lo, metal.hi, metal.lo) else DullTrim,
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(10.dp, PlinthShape)
                .clip(PlinthShape)
                .background(Lacquer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = medal.title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = if (medal.isEarned) Color.White else Color.White.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(5.dp))
            val nextThreshold = medal.nextThreshold
            when {
                medal.isEarned -> Text(
                    text = medal.tier.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        brush = Brush.horizontalGradient(listOf(metal.hi, metal.mid)),
                    ),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                )
                !medal.isHidden && nextThreshold != null -> LinearWavyProgressIndicator(
                    progress = { medal.progress },
                    color = metal.mid,
                    trackColor = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                )
                else -> Text(
                    text = tr("medals.secret.short", "Segreta").uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.35f),
                )
            }
        }
    }
}

/**
 * The case's lighting: a cone from a small lamp in its roof and the pool it
 * leaves round the medal, in the metal's colour once earned; a faint grey
 * glow otherwise.
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.caseLight(light: Color?) {
    val w = size.width
    val h = size.height
    val tint = light ?: Color.White
    val strength = if (light != null) 1f else 0.25f
    val cone = Path().apply {
        moveTo(w * 0.42f, 0f)
        lineTo(w * 0.58f, 0f)
        lineTo(w * 0.86f, h)
        lineTo(w * 0.14f, h)
        close()
    }
    drawPath(cone, Brush.verticalGradient(listOf(tint.copy(alpha = 0.20f * strength), Color.Transparent)))
    drawRect(
        Brush.radialGradient(
            listOf(tint.copy(alpha = 0.22f * strength), Color.Transparent),
            center = Offset(w / 2f, h * 0.48f),
            radius = w * 0.55f,
        ),
    )
    // The lamp itself.
    drawRect(
        Brush.radialGradient(
            listOf(StageLight.copy(alpha = 0.55f * strength), Color.Transparent),
            center = Offset(w / 2f, 0f),
            radius = w * 0.18f,
        ),
    )
}

/**
 * The medal hovering in its case, bobbing slowly, its shadow on the floor
 * shrinking as it rises. No two medals bob in step.
 */
@Composable
private fun FloatingMedal(medal: Medal, size: Dp, modifier: Modifier = Modifier) {
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
                .shadow(10.dp, CircleShape, clip = false),
        )
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .padding(bottom = 6.dp)
                .width(size * 0.8f)
                .height(12.dp)
                .graphicsLayer { scaleX = 1f - 0.08f * sin(time.value + phase) }
                .drawBehind {
                    drawOval(
                        Brush.radialGradient(
                            listOf(Color.Black.copy(alpha = 0.6f), Color.Transparent),
                            center = center,
                            radius = this.size.width / 2f,
                        ),
                    )
                },
        )
    }
}

/** The glass front: faintly tinted, lit along its edges, with reflections. */
@Composable
private fun GlassPane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.09f), Color.White.copy(alpha = 0.02f), Color.White.copy(alpha = 0.05f)),
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
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.13f), Color.Transparent),
                            startX = w * 0.18f,
                            endX = w * 0.40f,
                        ),
                        topLeft = Offset(w * 0.18f, -h * 0.3f),
                        size = Size(w * 0.22f, h * 1.6f),
                    )
                    drawRect(
                        Color.White.copy(alpha = 0.08f),
                        topLeft = Offset(w * 0.52f, -h * 0.3f),
                        size = Size(w * 0.035f, h * 1.6f),
                    )
                }
                // The roof pane, seen edge-on.
                drawRect(
                    Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(16.dp.toPx(), 2.dp.toPx()),
                    size = Size(w - 32.dp.toPx(), 1.5.dp.toPx()),
                )
            }
            .border(
                1.dp,
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.16f), Color.White.copy(alpha = 0.06f)),
                ),
                CaseShape,
            ),
    )
}
