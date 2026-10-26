package it.mensa.app.features.tableport.medals

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Shadow

/** The engraving on a medal's reverse. */
data class MedalReverse(val title: String, val caption: String)

/**
 * A struck metal medal: brushed rim with a milled edge, a domed field and
 * the emblem embossed in it. [sheen] (−1…1) moves the light across it.
 * Mirrors iOS `MedalArtwork`.
 */
@Composable
fun MedalArtwork(
    tier: MedalTier,
    icon: ImageVector,
    size: Dp,
    modifier: Modifier = Modifier,
    locked: Boolean = false,
    sheen: Float = 0f,
    reverse: MedalReverse? = null,
) {
    val metal = if (locked) LockedMetal else tier.metal
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2f
            // Rim, brushed: the light catches it in bands.
            rotate(sheen * 50f) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        listOf(metal.hi, metal.mid, metal.lo, metal.mid, metal.hi, metal.mid, metal.lo, metal.mid, metal.hi),
                    ),
                )
            }
            // Milled edge.
            val edge = r * 0.07f
            drawCircle(
                color = metal.lo.copy(alpha = 0.55f),
                radius = r - edge / 2f,
                style = Stroke(
                    width = edge,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx() * 0.6f, 1.dp.toPx())),
                ),
            )
            // Domed field.
            val field = r * 0.82f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(metal.hi, metal.mid, metal.lo),
                    center = Offset(this.size.width * (0.36f - sheen * 0.18f), this.size.height * 0.30f),
                    radius = r * 1.24f,
                ),
                radius = field,
            )
            // Bevel where the field meets the rim, and an inner border.
            drawCircle(
                brush = Brush.linearGradient(listOf(metal.lo, metal.hi)),
                radius = field,
                style = Stroke(width = (r * 0.028f).coerceAtLeast(1f)),
            )
            drawCircle(
                brush = Brush.linearGradient(listOf(metal.hi.copy(alpha = 0.9f), metal.lo.copy(alpha = 0.8f))),
                radius = r * 0.68f,
                style = Stroke(width = (r * 0.016f).coerceAtLeast(0.6f)),
            )
        }

        if (reverse != null) {
            Reverse(reverse, metal, size)
        } else {
            // Embossed: lit on the upper left, shaded on the lower right.
            val iconSize = size * 0.36f
            val step = size * 0.008f
            Icon(icon, null, tint = metal.hi, modifier = Modifier.size(iconSize).offset(-step, -step))
            Icon(icon, null, tint = Color.Black.copy(alpha = 0.35f), modifier = Modifier.size(iconSize).offset(step, step))
            Icon(icon, null, tint = lerp(metal.mid, metal.lo, 0.55f), modifier = Modifier.size(iconSize))
        }

        // Specular highlight, sliding with the light.
        Canvas(Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val center = Offset(w * (0.36f + sheen * 0.24f), h * 0.30f)
            rotate(-32f, center) {
                drawOval(
                    brush = Brush.radialGradient(
                        listOf(Color.White.copy(alpha = 0.6f), Color.Transparent),
                        center = center,
                        radius = w * 0.3f,
                    ),
                    topLeft = Offset(center.x - w * 0.3f, center.y - h * 0.15f),
                    size = Size(w * 0.6f, h * 0.3f),
                    blendMode = BlendMode.Screen,
                )
            }
        }
    }
}

@Composable
private fun Reverse(reverse: MedalReverse, metal: MetalRamp, size: Dp) {
    // Engraved: cut into the metal, lit on the lower edge.
    val engraved = TextStyle(
        fontFamily = FontFamily.Serif,
        color = metal.lo,
        textAlign = TextAlign.Center,
        shadow = Shadow(color = metal.hi.copy(alpha = 0.9f), offset = Offset(0.6f, 1f), blurRadius = 0f),
    )
    val unit = size.value
    Column(
        modifier = Modifier.width(size * 0.6f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(size * 0.03f),
    ) {
        Text("MENSA ITALIA", style = engraved.copy(fontSize = (unit * 0.065f).sp, fontWeight = FontWeight.Black, letterSpacing = (unit * 0.012f).sp))
        Box(Modifier.width(size * 0.3f).height(1.dp).drawWithContent { drawRect(metal.lo) })
        Text(reverse.title.uppercase(), style = engraved.copy(fontSize = (unit * 0.085f).sp, fontWeight = FontWeight.Bold), maxLines = 2)
        Text(reverse.caption, style = engraved.copy(fontSize = (unit * 0.06f).sp, fontWeight = FontWeight.SemiBold))
    }
}

private val Rainbow = listOf(
    Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00), Color(0xFF34C759),
    Color(0xFF32ADE6), Color(0xFF007AFF), Color(0xFFAF52DE), Color(0xFFFF3B30),
)

/**
 * The medal button's icon, its colours cycling through the rainbow: it's
 * meant to be noticed. Mirrors iOS `RainbowMedalIcon`.
 */
@Composable
fun RainbowMedalIcon(contentDescription: String?, modifier: Modifier = Modifier) {
    val turn by rememberInfiniteTransition(label = "rainbow").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(5_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rainbowTurn",
    )
    Icon(
        imageVector = Icons.Filled.MilitaryTech,
        contentDescription = contentDescription,
        modifier = modifier
            .size(24.dp)
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                // The glyph's own pixels keep only the rainbow, turning.
                rotate(turn) {
                    drawCircle(
                        brush = Brush.sweepGradient(Rainbow),
                        radius = this.size.maxDimension,
                        blendMode = BlendMode.SrcIn,
                    )
                }
            },
    )
}
