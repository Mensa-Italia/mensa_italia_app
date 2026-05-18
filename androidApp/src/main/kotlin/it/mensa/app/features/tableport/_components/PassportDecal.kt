package it.mensa.app.features.tableport._components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.components.CachedAsyncImage

// ─── Passport palette ─────────────────────────────────────────────────────────
//
// Tableport visual identity: premium navy document (cover) + warm parchment
// (interior). Gold accents on the cover remain serif/postal — the brand
// expression lives in the cyan inner-ring on stamps and the brand-blue hero
// surrounding the book.

object PassportPalette {
    // Cover — fixed brand navy for the passport document identity.
    val coverDeep   = Color(0xFF071A3A)
    val coverMid    = Color(0xFF0F2C66)
    val coverHi     = Color(0xFF184295)  // brand primary blue

    // Gold — restrained metallic; only for cover lettering and inner rule.
    val gold        = Color(0xFFC9A96A)
    val goldDeep    = Color(0xFF8E7036)
    val goldHi      = Color(0xFFF2D89C)

    // Parchment interior — warm cream, NOT yellow.
    val parchment     = Color(0xFFF6EFDC)
    val parchmentEdge = Color(0xFFDFD3B5)

    // Stamp accents
    val stampInk    = Color(0xFF8A1F1F)  // cancel mark — postal red, desaturated
    val ringAccent  = Color(0xFF6AC9F0)  // collected-stamp inner ring (brand cyan)
}

/**
 * PassportDecal — a single postage-stamp tile inside a passport page.
 *
 * Visual identity: perforated parchment chip with a cyan inner ring marking
 * a "collected" stamp. Optional cancel marks read as authentic postal ink.
 *
 * @param imageUrl optional stamp artwork URL
 * @param size tile size in dp
 * @param rotation small randomized rotation for hand-pasted feel
 * @param showsCancel when true overlays postal cancel marks
 */
@Composable
fun PassportDecal(
    imageUrl: String?,
    size: Dp,
    rotation: Float,
    showsCancel: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .rotate(rotation),
        contentAlignment = Alignment.Center,
    ) {
        // Perforated parchment tile
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPerforatedTile()
        }

        // Stamp artwork (or fallback icon)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.16f),
            contentAlignment = Alignment.Center,
        ) {
            if (!imageUrl.isNullOrEmpty()) {
                CachedAsyncImage(
                    model = imageUrl,
                    contentDescription = "timbro",
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Verified,
                    contentDescription = null,
                    tint = PassportPalette.coverDeep.copy(alpha = 0.45f),
                    modifier = Modifier.size(size * 0.36f),
                )
            }
        }

        // Cyan inner ring — brand presence on every collected stamp.
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(size * 0.10f),
        ) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PassportPalette.ringAccent.copy(alpha = 0.55f),
                        PassportPalette.gold.copy(alpha = 0.45f),
                    ),
                ),
                cornerRadius = CornerRadius(6f),
                style = Stroke(width = 1.1f),
            )
        }

        // Cancel marks (postal ink)
        if (showsCancel) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
            ) {
                val lines = 4
                val canvasSize = this.size
                val spacing = canvasSize.height / (lines + 1)
                for (i in 1..lines) {
                    val y = i * spacing
                    val path = Path().apply {
                        moveTo(0f, y - 3f)
                        quadraticTo(canvasSize.width / 2f, y - 9f, canvasSize.width, y + 3f)
                    }
                    drawPath(
                        path,
                        PassportPalette.stampInk.copy(alpha = 0.45f),
                        style = Stroke(width = 1.1f),
                    )
                }
            }
        }
    }
}

/**
 * Draws a perforated postage tile: parchment fill with rounded corners,
 * subtle outline, and small notches simulating the perforation toothing.
 */
private fun DrawScope.drawPerforatedTile() {
    val parchment = PassportPalette.parchment
    val edge = PassportPalette.parchmentEdge

    // Base tile with subtle linear gradient
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(parchment, edge.copy(alpha = 0.65f), parchment),
        ),
        cornerRadius = CornerRadius(6f),
    )

    // Perforation notches around the edge for "stamp" silhouette
    val toothCount = 14
    val toothRadius = (size.minDimension / toothCount) * 0.32f
    val stepX = size.width / toothCount
    val stepY = size.height / toothCount

    for (i in 0 until toothCount) {
        val cx = stepX * (i + 0.5f)
        val cy = stepY * (i + 0.5f)
        // Top + bottom
        drawCircle(
            color = Color.White,
            radius = toothRadius,
            center = Offset(cx, 0f),
        )
        drawCircle(
            color = Color.White,
            radius = toothRadius,
            center = Offset(cx, size.height),
        )
        // Left + right
        drawCircle(
            color = Color.White,
            radius = toothRadius,
            center = Offset(0f, cy),
        )
        drawCircle(
            color = Color.White,
            radius = toothRadius,
            center = Offset(size.width, cy),
        )
    }

    // Soft outline for tile silhouette
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.10f),
        cornerRadius = CornerRadius(6f),
        style = Stroke(width = 0.8f),
    )

    // Top-left highlight for paper feel
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(size.width * 0.2f, size.height * 0.15f),
            radius = size.minDimension * 0.6f,
        ),
        size = Size(size.width, size.height),
    )
}
