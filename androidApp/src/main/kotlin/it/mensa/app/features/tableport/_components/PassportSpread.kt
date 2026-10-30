package it.mensa.app.features.tableport._components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.components.ColumnSplit
import it.mensa.shared.model.StampUserModel

/** Stamps on a page, as in [PassportPage]. */
private const val STAMPS_PER_PAGE = 6

/** Least width, in dp, either page of a spread may have (iOS: 240pt). */
val PassportSpreadMinPageWidth = 240.dp

/**
 * The open passport on a wide screen: two facing pages of stamps, bound in
 * the middle where [split] falls (the hinge of an unfolded foldable, else the
 * halfway gutter). Mirrors iOS `PassportSpread`.
 *
 * Spread `n` shows pages `2n` (left) and `2n + 1` (right); an odd last page
 * faces a blank one. The spreads are leafed through with a horizontal swipe,
 * where iOS curls the page. The paper of both pages runs under the hinge and
 * [contentInsets] (top bar, scan button); the stamps and page numbers keep
 * clear of them.
 *
 * [onSpreadChange] reports the spread that is open, for the prefetch, which
 * warms the spreads around it twelve stamps at a time.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PassportSpread(
    stamps: List<StampUserModel>,
    stampsRevealed: Boolean,
    onTapStamp: (StampUserModel) -> Unit,
    split: ColumnSplit,
    contentInsets: PaddingValues,
    onSpreadChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageCount = maxOf((stamps.size + STAMPS_PER_PAGE - 1) / STAMPS_PER_PAGE, 1)
    val spreadCount = (pageCount + 1) / 2
    val pagerState = rememberPagerState { spreadCount }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect { onSpreadChange(it) }
    }

    BoxWithConstraints(modifier) {
        // Each paper reaches to the middle of the gap, so together they cover
        // the hinge; the contents keep half the gap clear of the spine.
        val leftWidth: Dp = split.leftWidth + split.gap / 2
        val rightWidth: Dp = maxWidth - leftWidth
        val spineMargin = split.gap / 2
        val pageHeight = maxHeight
        val top = contentInsets.calculateTopPadding()
        val bottom = contentInsets.calculateBottomPadding()
        val leftInsets = PaddingValues(top = top, bottom = bottom, end = spineMargin)
        val rightInsets = PaddingValues(top = top, bottom = bottom, start = spineMargin)

        HorizontalPager(state = pagerState, beyondViewportPageCount = 1) { spread ->
            Row {
                PassportPage(
                    stamps = stamps,
                    pageIndex = spread * 2,
                    totalPages = pageCount,
                    totalStamps = stamps.size,
                    stampsRevealed = stampsRevealed,
                    width = leftWidth,
                    height = pageHeight,
                    onTapStamp = onTapStamp,
                    spineAtEnd = true,
                    insets = leftInsets,
                    cornerRadius = 0.dp,
                )
                PassportPage(
                    stamps = stamps,
                    pageIndex = spread * 2 + 1,
                    totalPages = pageCount,
                    totalStamps = stamps.size,
                    stampsRevealed = stampsRevealed,
                    width = rightWidth,
                    height = pageHeight,
                    onTapStamp = onTapStamp,
                    blank = spread * 2 + 1 >= pageCount,
                    insets = rightInsets,
                    cornerRadius = 0.dp,
                )
            }
        }
    }
}
