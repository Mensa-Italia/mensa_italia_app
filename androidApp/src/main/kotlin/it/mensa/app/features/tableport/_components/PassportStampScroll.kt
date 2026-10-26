package it.mensa.app.features.tableport._components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.model.StampUserModel
import kotlinx.coroutines.launch

private const val STAMPS_PER_PAGE = 6

/** How far, in dp, the stamps may trail the scroll. */
private const val MAX_DRIFT_DP = 26f

/**
 * PassportStampScroll — a phone's passport: every stamp in one scroll, in
 * order, on a single parchment page. Paging six at a time made a long
 * collection a chore to leaf through. Mirrors iOS `PassportStampScroll`.
 *
 * While it scrolls the stamps float: each trails the motion a little more
 * the farther it is from the middle of the view, so the gaps between them
 * stretch with the speed and settle back, wobbling, when the scroll stops.
 *
 * The stamps run the whole height of the screen, under the top bar and the
 * scan button; [contentPadding] only sets where the list starts and ends.
 * They fade into the paper at the bottom so they don't crowd the button.
 *
 * [onPageChange] reports the six-stamp page the scroll is at, for the
 * prefetch.
 */
@Composable
fun PassportStampScroll(
    stamps: List<StampUserModel>,
    stampsRevealed: Boolean,
    onTapStamp: (StampUserModel) -> Unit,
    onPageChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
) {
    val gridState = rememberLazyGridState()
    val drift = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val maxDrift = with(LocalDensity.current) { MAX_DRIFT_DP.dp.toPx() }

    // The scroll's speed, frame by frame, sets how far the stamps trail.
    val driftFromScroll = remember(maxDrift) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val target = (-consumed.y * 1.4f).coerceIn(-maxDrift, maxDrift)
                scope.launch {
                    drift.animateTo(target, spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium))
                }
                return Offset.Zero
            }
        }
    }

    // Let go with a looser spring when the scroll stops: they bob into line.
    LaunchedEffect(gridState.isScrollInProgress) {
        if (!gridState.isScrollInProgress) {
            drift.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
        }
    }

    LaunchedEffect(gridState) {
        // Item 0 is the header.
        snapshotFlow { (gridState.firstVisibleItemIndex - 1).coerceAtLeast(0) / STAMPS_PER_PAGE }
            .collect { onPageChange(it) }
    }

    val layoutDirection = LocalLayoutDirection.current
    val topPadding = contentPadding.calculateTopPadding()
    val bottomPadding = contentPadding.calculateBottomPadding()

    BoxWithConstraints(
        modifier = modifier.drawBehind {
            // The paper, edge to edge.
            drawRect(PassportPalette.parchment)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        PassportPalette.parchment,
                        PassportPalette.parchmentEdge.copy(alpha = 0.85f),
                        PassportPalette.parchment,
                    ),
                ),
            )
        },
    ) {
        val sidePadding = 22.dp
        val spacing = 16.dp
        val startPadding = contentPadding.calculateStartPadding(layoutDirection) + sidePadding
        val endPadding = contentPadding.calculateEndPadding(layoutDirection) + sidePadding
        val stampSize = (maxWidth - startPadding - endPadding - spacing) / 2
        // The scan button's area and a little above it.
        val fadeHeight = bottomPadding + 80.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            state = gridState,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(
                start = startPadding,
                end = endPadding,
                top = topPadding + 12.dp,
                bottom = bottomPadding + 24.dp,
            ),
            modifier = Modifier
                .fillMaxSize()
                // Offscreen, so the fade below masks the stamps, not the paper.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = fadeHeight.toPx().coerceAtMost(size.height)
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to Color.Black,
                            0.55f to Color.Black.copy(alpha = 0.25f),
                            1f to Color.Transparent,
                            startY = size.height - fade,
                            endY = size.height,
                        ),
                        topLeft = Offset(0f, size.height - fade),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .nestedScroll(driftFromScroll),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Header(totalStamps = stamps.size)
            }
            itemsIndexed(stamps, key = { _, stamp -> stamp.id }) { index, stamp ->
                val seed = remember(stamp.id) { deterministicSeed(stamp.id) }
                val phase = (seed.dx + 0.5).toFloat()
                val reveal by animateFloatAsState(
                    targetValue = if (stampsRevealed) 1f else 0f,
                    animationSpec = MensaMotion.springHeroOvershoot,
                    label = "stampReveal",
                )
                val imageUrl = stamp.stampRecord?.let { r ->
                    if (r.image.isNotEmpty()) {
                        FilesUrl.build(
                            collection = "stamp",
                            recordId = r.id,
                            filename = r.image,
                            thumb = StampThumb.GRID,
                        )
                    } else {
                        null
                    }
                }
                PassportDecal(
                    imageUrl = imageUrl,
                    size = stampSize,
                    rotation = ((seed.angle - 0.5) * 10).toFloat(),
                    showsCancel = index % 4 == 0,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = reveal
                            scaleY = reveal
                            // Grid item index: the header comes first.
                            val info = gridState.layoutInfo
                            val item = info.visibleItemsInfo.firstOrNull { it.index == index + 1 }
                            if (item != null) {
                                val viewport = (info.viewportEndOffset - info.viewportStartOffset)
                                    .coerceAtLeast(1)
                                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                                // -0.5 at the top of the view, 0 in the middle, 0.5 at the bottom.
                                val position = (item.offset.y + item.size.height / 2f - viewportCenter) / viewport
                                val reach = 0.8f + 0.4f * phase
                                translationY = drift.value * position * 2f * reach
                                rotationZ = drift.value / density * 0.08f * (phase - 0.5f)
                            }
                        }
                        .clickable { onTapStamp(stamp) },
                )
            }
        }
    }
}

@Composable
private fun Header(totalStamps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
}
