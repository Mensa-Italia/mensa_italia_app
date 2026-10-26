package it.mensa.app.features.tableport._components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import it.mensa.app.support.tr
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import it.mensa.app.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.ui.theme.MensaMotion

/**
 * PassportCover — the front cover of the virtual passport, edge to edge.
 *
 * Cobalt leather, pebbled and stitched round the edge, with the Mensa mark
 * and the lettering hot-stamped in gold foil. It doesn't move by itself:
 * [PassportCoverTurn] turns it open like a book's. [contentPadding] keeps
 * the foil clear of the bars it runs under. Mirrors iOS `PassportCover`.
 */
@Composable
fun PassportCover(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val width = maxWidth
        // ── Leather: cobalt lit from the upper left, pebbled, darker where it
        // wraps the board. The grain is built once (drawWithCache) and the
        // cover only turns in 3D afterwards.
        Box(
            Modifier
                .fillMaxSize()
                .drawWithCache {
                    // The grain, drawn once into a tile and repeated: drawn
                    // whole, tens of thousands of bumps went out every frame.
                    val grain = ShaderBrush(
                        ImageShader(leatherTile(density), TileMode.Repeated, TileMode.Repeated),
                    )
                    val base = Brush.radialGradient(
                        colors = listOf(PassportPalette.coverHi, PassportPalette.coverMid, PassportPalette.coverDeep),
                        center = Offset(size.width * 0.35f, size.height * 0.25f),
                        radius = maxOf(size.width, size.height) * 0.95f,
                    )
                    onDrawBehind {
                        drawRect(base)
                        drawRect(grain)
                        // Where the leather wraps the board, it darkens.
                        val edge = 30.dp.toPx()
                        drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent), endX = edge))
                        drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), startX = size.width - edge))
                        drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent), endY = edge))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)), startY = size.height - edge))
                        // A soft sheen across the top left.
                        drawRect(
                            Brush.radialGradient(
                                listOf(Color.White.copy(alpha = 0.12f), Color.Transparent),
                                center = Offset.Zero,
                                radius = size.width * 0.7f,
                            ),
                        )
                        // Stitching, in from the edge and round the display's
                        // own corners.
                        val inset = 14.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFF5C6B9E).copy(alpha = 0.85f),
                            topLeft = Offset(inset, inset),
                            size = Size(size.width - inset * 2, size.height - inset * 2),
                            cornerRadius = CornerRadius(34.dp.toPx()),
                            style = Stroke(
                                width = 1.1.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 3.5.dp.toPx())),
                            ),
                        )
                        // Blind-embossed frame: pressed in, no colour of its own.
                        val frame = 30.dp.toPx()
                        val frameSize = Size(size.width - frame * 2, size.height - frame * 2)
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.10f),
                            topLeft = Offset(frame, frame + 1.dp.toPx()),
                            size = frameSize,
                            cornerRadius = CornerRadius(22.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        drawRoundRect(
                            color = Color.Black.copy(alpha = 0.35f),
                            topLeft = Offset(frame, frame),
                            size = frameSize,
                            cornerRadius = CornerRadius(22.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx()),
                        )
                        // Spine, where the cover bends.
                        drawRect(Brush.horizontalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent), endX = 16.dp.toPx()))
                    }
                },
        )

        // ── Hot-stamped gold foil: the Mensa mark and the lettering ─────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 40.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(R.drawable.tessera_logo_mark),
                contentDescription = null,
                modifier = Modifier
                    .size(width * 0.42f)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        // The mark's own pixels keep only the foil.
                        drawRect(GoldFoil, blendMode = BlendMode.SrcIn)
                    },
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "MENSA ITALIA",
                style = foilText(22.sp, FontWeight.Black, 7.sp),
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "TABLEPORT",
                style = foilText(15.sp, FontWeight.Bold, 9.sp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = tr("passport.swipe_to_open", "· SWIPE TO OPEN ·"),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 10.sp,
                letterSpacing = 3.sp,
                color = PassportPalette.gold.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
        }
    }
}

/**
 * The inside of the cover, seen as it turns past upright: cobalt cloth, the
 * grain fainter than the leather's. Mirrors iOS `PassportCoverLining`.
 */
@Composable
fun PassportCoverLining(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .drawWithCache {
                val grain = ShaderBrush(ImageShader(leatherTile(density), TileMode.Repeated, TileMode.Repeated))
                val base = Brush.linearGradient(
                    listOf(PassportPalette.coverMid, PassportPalette.coverDeep),
                    start = Offset(size.width, 0f),
                    end = Offset(0f, size.height),
                )
                onDrawBehind {
                    drawRect(base)
                    drawRect(grain, alpha = 0.6f)
                }
            },
    )
}

/** Gold foil: a metallic sweep catching the light along the diagonal. */
private val GoldFoil = Brush.linearGradient(
    0f to PassportPalette.goldHi,
    0.35f to PassportPalette.gold,
    0.5f to PassportPalette.goldHi.copy(alpha = 0.95f),
    0.8f to PassportPalette.goldDeep,
    1f to PassportPalette.gold,
)

/** Serif lettering in gold foil, pressed slightly into the leather. */
private fun foilText(size: TextUnit, weight: FontWeight, tracking: TextUnit) = TextStyle(
    fontFamily = FontFamily.Serif,
    fontWeight = weight,
    fontSize = size,
    letterSpacing = tracking,
    textAlign = TextAlign.Center,
    brush = GoldFoil,
    shadow = Shadow(Color.Black.copy(alpha = 0.55f), Offset(0f, -1f), 1f),
)

/**
 * Pebbled leather as a seamless tile: small irregular bumps packed edge to
 * edge, each shaded on its lower right and lit on its upper left. Drawn once
 * per screen density.
 */
private val leatherTiles = mutableMapOf<Float, ImageBitmap>()

private fun leatherTile(density: Float): ImageBitmap = leatherTiles.getOrPut(density) {
    val side = 220f * density
    val bitmap = ImageBitmap(side.toInt(), side.toInt())
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    val shade = Path()
    val light = Path()
    var seed = 0x5DEECE66DUL
    fun next(): Float {
        seed = seed * 6364136223846793005UL + 1442695040888963407UL
        return (seed shr 40).toFloat() / (1UL shl 24).toFloat()
    }
    repeat((220f * 220f / 5f).toInt()) {
        val x = next() * side
        val y = next() * side
        val w = (1.1f + next() * 1.6f) * density
        val h = w * (0.75f + next() * 0.4f)
        // Bumps crossing an edge are drawn on the opposite one too, so the
        // tile repeats without seams.
        for (dx in floatArrayOf(0f, -side, side)) for (dy in floatArrayOf(0f, -side, side)) {
            val left = x + dx
            val top = y + dy
            if (left + w < 0f || left > side || top + h < 0f || top > side) continue
            shade.addOval(Rect(Offset(left + 0.35f * density, top + 0.45f * density), Size(w, h)))
            light.addOval(Rect(Offset(left, top), Size(w, h)))
        }
    }
    canvas.drawPath(shade, Paint().apply { color = Color.Black.copy(alpha = 0.11f) })
    canvas.drawPath(light, Paint().apply { color = Color.White.copy(alpha = 0.025f) })
    bitmap
}
