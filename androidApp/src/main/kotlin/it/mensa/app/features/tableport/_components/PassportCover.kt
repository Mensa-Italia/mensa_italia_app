package it.mensa.app.features.tableport._components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
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
 * PassportCover — front cover of the virtual passport.
 *
 * M3 Expressive treatments:
 *   - shape morph on tap (24dp → 20dp radius via springShape)
 *   - subtle Y-rotation overshoot on "open" tap (springHeroOvershoot)
 *   - perspective via graphicsLayer.cameraDistance
 *
 * Visual identity: deep navy cobalt leather, restrained serif gold lettering,
 * a single cyan inner accent rule to thread the brand color into the
 * otherwise-classical document.
 */
@Composable
fun PassportCover(
    width: Dp,
    height: Dp,
    isOpen: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotationY by animateFloatAsState(
        targetValue = if (isOpen) -4f else 0f,
        animationSpec = MensaMotion.springHeroOvershoot,
        label = "coverRotationY",
    )

    // Corner morph on press — 24dp resting, 20dp pressed.
    val cornerRadius by animateDpAsState(
        targetValue = if (isOpen) 20.dp else 24.dp,
        animationSpec = MensaMotion.springShape,
        label = "coverCorner",
    )

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .graphicsLayer {
                this.rotationY = rotationY
                cameraDistance = 8f * density
            }
            .clip(RoundedCornerShape(cornerRadius))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // ── Leather base — radial cobalt gradient ───────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gradient = Brush.radialGradient(
                colors = listOf(
                    PassportPalette.coverHi,
                    PassportPalette.coverMid,
                    PassportPalette.coverDeep,
                ),
                center = Offset(size.width * 0.32f, size.height * 0.22f),
                radius = maxOf(size.width, size.height) * 0.95f,
            )
            drawRect(brush = gradient)

            // Leather grain — diagonal hairlines
            val step = 3f
            var y = -size.height
            while (y < size.height * 2) {
                drawLine(
                    color = Color.Black.copy(alpha = 0.05f),
                    start = Offset(-10f, y),
                    end = Offset(size.width + 10f, y - size.height * 0.6f),
                    strokeWidth = 0.5f,
                )
                y += step
            }

            // Top-left highlight bloom — gives 3D
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
                    center = Offset(0f, 0f),
                    radius = size.width * 0.65f,
                ),
            )

            // Spine shadow on the left edge
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(PassportPalette.coverDeep, Color.Transparent),
                    startX = 0f,
                    endX = 16.dp.toPx(),
                ),
            )
        }

        // ── Gold lettering layout ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            // Top kicker — "REPUBBLICA ITALIANA"-style label
            Text(
                text = "MENSA ITALIA",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 4.sp,
                color = PassportPalette.goldHi.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )

            // Center seal — circle + monogram, surrounded by a thin cyan halo
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier.size(120.dp),
                ) {
                    // Outer gold ring
                    drawCircle(
                        brush = Brush.linearGradient(
                            colors = listOf(PassportPalette.goldHi, PassportPalette.goldDeep),
                        ),
                        radius = size.minDimension / 2f,
                        style = Stroke(width = 1.5f),
                    )
                    // Inner thinner ring
                    drawCircle(
                        color = PassportPalette.gold.copy(alpha = 0.55f),
                        radius = size.minDimension / 2f - 8.dp.toPx(),
                        style = Stroke(width = 0.8f),
                    )
                    // Tiny cyan brand halo — barely perceptible, ties to design system
                    drawCircle(
                        color = PassportPalette.ringAccent.copy(alpha = 0.20f),
                        radius = size.minDimension / 2f - 16.dp.toPx(),
                        style = Stroke(width = 0.6f),
                    )
                }
                Text(
                    text = "M",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 58.sp,
                    color = PassportPalette.goldHi,
                    textAlign = TextAlign.Center,
                )
            }

            // Bottom block — title + sub
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "PASSAPORTO",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    letterSpacing = 6.sp,
                    color = PassportPalette.goldHi,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "DEI TIMBRI",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 5.sp,
                    color = PassportPalette.gold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "—  SCORRI PER APRIRE  —",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 9.sp,
                    letterSpacing = 3.sp,
                    color = PassportPalette.gold.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        // ── Gold inner rule frame ──────────────────────────────────────────
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PassportPalette.goldHi,
                        PassportPalette.gold,
                        PassportPalette.goldDeep,
                    ),
                ),
                topLeft = Offset(14.dp.toPx(), 14.dp.toPx()),
                size = Size(
                    width = size.width - 28.dp.toPx(),
                    height = size.height - 28.dp.toPx(),
                ),
                cornerRadius = CornerRadius(16.dp.toPx()),
                style = Stroke(width = 1.2f),
            )
        }
    }
}
