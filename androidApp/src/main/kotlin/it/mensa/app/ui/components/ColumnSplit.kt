package it.mensa.app.ui.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/**
 * Where a screen parts into two columns, in its own space: [leftWidth] is
 * the left column, [gap] what lies between the two (the hinge, or a gutter).
 * The right column takes the rest.
 */
@Immutable
data class ColumnSplit(val leftWidth: Dp, val gap: Dp)

/** Narrower than this, without a hinge, a screen is one column, as on a phone. */
val TwoColumnMinWidth = 600.dp

private val Gutter = 16.dp

/**
 * Lays [content] out with the split of the space it is given: the hinge of
 * an unfolded foldable crossing it, else a gutter down the middle when it is
 * at least [minWidth] wide, else null for one column. Mirrors iOS
 * `GeometryProxy.verticalHinge` and the halfway fallback of `TodayView` /
 * `LeaderboardView`.
 */
@Composable
fun ColumnSplitBox(
    modifier: Modifier = Modifier,
    minWidth: Dp = TwoColumnMinWidth,
    content: @Composable (ColumnSplit?) -> Unit,
) {
    val hinge = rememberVerticalHinge()
    val density = LocalDensity.current
    // The hinge comes in window coordinates; this box may not start at the
    // window's edge (beside the navigation rail, say).
    var originX by remember { mutableFloatStateOf(0f) }
    BoxWithConstraints(modifier.onGloballyPositioned { originX = it.positionInWindow().x }) {
        val width = constraints.maxWidth.toFloat()
        val atHinge = hinge?.let { h ->
            val left = h.left - originX
            val right = h.right - originX
            if (left <= 0f || right >= width) return@let null
            with(density) {
                // A hinge with no gap of its own (Pixel Fold) still gets a gutter.
                val gap = maxOf(right - left, Gutter.toPx())
                val center = (left + right) / 2
                ColumnSplit(leftWidth = (center - gap / 2).toDp(), gap = gap.toDp())
            }
        }
        val split = atHinge ?: if (maxWidth >= minWidth) ColumnSplit((maxWidth - Gutter) / 2, Gutter) else null
        content(split)
    }
}

/**
 * The vertical hinge of an unfolded foldable, in window pixels; null on
 * single-screen devices and while folded or turned so the hinge runs across.
 */
@Composable
fun rememberVerticalHinge(): Rect? {
    val activity = LocalContext.current.findActivity() ?: return null
    val layoutInfo = remember(activity) { WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity) }
    val info by layoutInfo.collectAsState(initial = null)
    return info?.displayFeatures
        ?.filterIsInstance<FoldingFeature>()
        ?.firstOrNull { it.orientation == FoldingFeature.Orientation.VERTICAL }
        ?.bounds
        ?.toComposeRect()
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
