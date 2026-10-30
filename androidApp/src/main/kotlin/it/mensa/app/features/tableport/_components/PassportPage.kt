package it.mensa.app.features.tableport._components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.model.StampUserModel

private const val STAMPS_PER_PAGE = 6

/**
 * PassportPage — interior leaf with a 3×2 grid of stamps.
 *
 * Visual identity: warm parchment surface, gold rule inset, a giant
 * watermark "M", classic serif typography for the header/footer marginalia.
 * Stamps animate in with stagger using springHeroOvershoot.
 *
 * A page of an open two-page spread (`PassportSpread`) is the same leaf with
 * its binding on the other side ([spineAtEnd], for the left page), its paper
 * running under the hinge, bars and button while the contents keep clear of
 * them ([insets]), and no corners ([cornerRadius] 0) where it fills the
 * screen. Upright it lays its six stamps two across and three down, as iOS
 * `SinglePassportPage`; lying on its side, three across and two down.
 * [blank] is the page after the last stamps: paper and nothing else.
 */
@Composable
fun PassportPage(
    stamps: List<StampUserModel>,
    pageIndex: Int,
    totalPages: Int,
    totalStamps: Int,
    stampsRevealed: Boolean,
    width: Dp,
    height: Dp,
    onTapStamp: (StampUserModel) -> Unit,
    modifier: Modifier = Modifier,
    spineAtEnd: Boolean = false,
    blank: Boolean = false,
    insets: PaddingValues = PaddingValues(),
    cornerRadius: Dp = 22.dp,
) {
    val chunk = stamps
        .drop(pageIndex * STAMPS_PER_PAGE)
        .take(STAMPS_PER_PAGE)
    val insetStart = insets.calculateLeftPadding(LayoutDirection.Ltr)
    val insetEnd = insets.calculateRightPadding(LayoutDirection.Ltr)
    val insetTop = insets.calculateTopPadding()
    val insetBottom = insets.calculateBottomPadding()
    // Two across on an upright page, three on one lying on its side. Sized to
    // fit both ways: by width alone, three rows outgrow a tall, narrow page.
    val columns = if (width > height * 1.15f) 3 else 2
    val rows = STAMPS_PER_PAGE / columns
    val stampSize = minOf(
        (width - insetStart - insetEnd - 52.dp - 10.dp * (columns - 1)) / columns,
        (height - insetTop - insetBottom - 108.dp - 12.dp * (rows - 1)) / rows,
    ).coerceAtLeast(0.dp)

    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(cornerRadius))
            .drawBehind {
                // Parchment base with subtle 3-stop gradient (centre lighter than edges)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            PassportPalette.parchment,
                            PassportPalette.parchmentEdge.copy(alpha = 0.85f),
                            PassportPalette.parchment,
                        ),
                    ),
                )
                // Diagonal hairlines — paper grain
                val lineSpacing = 8.dp.toPx()
                var x = -size.height
                while (x < size.width + size.height) {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.025f),
                        start = Offset(x, 0f),
                        end = Offset(x + size.height, size.height),
                        strokeWidth = 0.8f,
                    )
                    x += lineSpacing
                }
            },
    ) {
        // Deterministic dot grain + gold rule + spine shadow
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width.toInt().coerceAtLeast(1)
            val h = size.height.toInt().coerceAtLeast(1)
            var seed = 0x9E3779B97F4A7C15UL + (pageIndex.toULong() * 1_000_003UL)
            for (i in 0 until 220) {
                seed = seed * 6364136223846793005UL + 1442695040888963407UL
                val px = (seed % w.toULong()).toFloat()
                seed = seed * 6364136223846793005UL + 1442695040888963407UL
                val py = (seed % h.toULong()).toFloat()
                seed = seed * 6364136223846793005UL + 1442695040888963407UL
                val r = (seed % 3UL).toFloat() * 0.45f
                drawCircle(color = Color.Black.copy(alpha = 0.04f), radius = r, center = Offset(px, py))
            }

            // Gold rule inset
            drawRoundRect(
                color = PassportPalette.gold.copy(alpha = 0.55f),
                topLeft = Offset((12.dp + insetStart).toPx(), (12.dp + insetTop).toPx()),
                size = Size(
                    size.width - (24.dp + insetStart + insetEnd).toPx(),
                    size.height - (24.dp + insetTop + insetBottom).toPx(),
                ),
                cornerRadius = CornerRadius(14.dp.toPx()),
                style = Stroke(width = 0.9f),
            )

            // Spine shadow
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.22f), Color.Transparent),
                    startX = if (spineAtEnd) size.width else 0f,
                    endX = if (spineAtEnd) size.width - 22.dp.toPx() else 22.dp.toPx(),
                ),
            )
        }

        // Watermark "M"
        Text(
            text = "M",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Black,
            fontSize = (minOf(width.value, height.value) * 0.75f).sp,
            color = PassportPalette.coverDeep.copy(alpha = 0.04f),
            modifier = Modifier.align(Alignment.Center),
        )

        // Header + footer marginalia
        if (!blank) Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 26.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = tr("tableport.page_collection_label", "COLLEZIONE"),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 3.sp,
                    color = PassportPalette.coverDeep.copy(alpha = 0.55f),
                )
                Text(
                    text = "N° %03d".format(totalStamps),
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 2.sp,
                    color = PassportPalette.coverDeep.copy(alpha = 0.55f),
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "— ${pageIndex + 1} / ${maxOf(totalPages, 1)} —",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 9.sp,
                letterSpacing = 2.sp,
                color = PassportPalette.coverDeep.copy(alpha = 0.45f),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp),
            )
        }

        if (blank) {
            // Paper only.
        } else if (chunk.isEmpty()) {
            EmptyPageState(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(insets)
                    .padding(horizontal = 32.dp),
            )
        } else {
            StampGrid(
                stamps = chunk,
                stampsRevealed = stampsRevealed,
                pageIndex = pageIndex,
                columns = columns,
                stampSize = stampSize,
                onTapStamp = onTapStamp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(insets)
                    .padding(horizontal = 26.dp, vertical = 54.dp),
            )
        }
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyPageState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // Gold-ringed parchment circle to echo the postal seal
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(72.dp)) {
                drawCircle(
                    color = PassportPalette.gold.copy(alpha = 0.55f),
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 1.4f),
                )
                drawCircle(
                    color = PassportPalette.gold.copy(alpha = 0.30f),
                    radius = size.minDimension / 2f - 5.dp.toPx(),
                    style = Stroke(width = 0.8f),
                )
            }
            Icon(
                imageVector = Icons.Outlined.Verified,
                contentDescription = null,
                tint = PassportPalette.coverDeep.copy(alpha = 0.40f),
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = tr("tableport.empty_title", "Nessun timbro ancora"),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            letterSpacing = 1.sp,
            color = PassportPalette.coverDeep.copy(alpha = 0.75f),
            textAlign = TextAlign.Center,
        )
        Text(
            text = tr("tableport.empty_body", "Scansiona un QR per iniziare la collezione."),
            fontFamily = FontFamily.Serif,
            fontSize = 11.sp,
            color = PassportPalette.coverDeep.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Stamp grid ──────────────────────────────────────────────────────────────

@Composable
private fun StampGrid(
    stamps: List<StampUserModel>,
    stampsRevealed: Boolean,
    pageIndex: Int,
    columns: Int,
    stampSize: Dp,
    onTapStamp: (StampUserModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        stamps.chunked(columns).forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                row.forEachIndexed { colIdx, stamp ->
                    val idx = rowIdx * columns + colIdx
                    val seed = deterministicSeed(stamp.id)
                    val rotation = ((seed.angle - 0.5) * 10).toFloat()
                    val showCancel = (pageIndex + idx) % 4 == 0

                    // Scale-in with overshoot — the stamp lands on the page.
                    val scaleAnim by animateFloatAsState(
                        targetValue = if (stampsRevealed) 1f else 0f,
                        animationSpec = MensaMotion.springHeroOvershoot,
                        label = "stampScale_$idx",
                    )

                    val imageUrl = stamp.stampRecord?.let { r ->
                        if (r.image.isNotEmpty()) {
                            FilesUrl.build(
                                collection = "stamp",
                                recordId = r.id,
                                filename = r.image,
                                thumb = StampThumb.GRID,
                            )
                        } else null
                    }

                    PassportDecal(
                        imageUrl = imageUrl,
                        size = stampSize,
                        rotation = rotation,
                        showsCancel = showCancel,
                        // `onTapStamp` arrivava fin qui attraverso tre livelli
                        // di composable e poi non veniva mai chiamato: il
                        // dettaglio del timbro, che su iOS esiste da sempre, su
                        // Android era irraggiungibile.
                        modifier = Modifier
                            .scale(scaleAnim)
                            .clickable { onTapStamp(stamp) },
                    )
                }
                // Fill empty cells so the row remains aligned
                repeat(columns - row.size) {
                    EmptyStampSlot(size = stampSize)
                }
            }
        }
    }
}

/** Empty slot — dashed circle, signals "future stamp here". */
@Composable
private fun EmptyStampSlot(size: Dp) {
    Canvas(modifier = Modifier.size(size)) {
        val strokeWidth = 1.4.dp.toPx()
        val dashLength = 6.dp.toPx()
        val gapLength = 4.dp.toPx()
        val radius = (this.size.minDimension / 2f) - strokeWidth

        drawCircle(
            color = PassportPalette.coverDeep.copy(alpha = 0.20f),
            radius = radius,
            style = Stroke(
                width = strokeWidth,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(dashLength, gapLength),
                    phase = 0f,
                ),
            ),
        )
    }
}

internal data class Seed(val angle: Double, val dx: Double, val dy: Double)

internal fun deterministicSeed(id: String): Seed {
    var h: ULong = 1469598103934665603UL
    for (b in id.encodeToByteArray()) {
        h = h xor b.toULong()
        h *= 1099511628211UL
    }
    val a = (h and 0xFFFFUL).toDouble() / 0xFFFF.toDouble()
    val b = ((h shr 16) and 0xFFFFUL).toDouble() / 0xFFFF.toDouble()
    val c = ((h shr 32) and 0xFFFFUL).toDouble() / 0xFFFF.toDouble()
    return Seed(angle = a, dx = b - 0.5, dy = c - 0.5)
}
