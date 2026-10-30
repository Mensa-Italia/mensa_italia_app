package it.mensa.app.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Narrower than this, an index with a page beside it stays the pushing,
 * phone form (unless a hinge splits the screen anyway). Mirrors iOS
 * `collapsesSidebarWhenNarrow`, which keeps a regular-width window that is
 * still too narrow for a sidebar (iPhone Duo upright, ~670pt) in its
 * pushing form. Counted on the content area, so beside the navigation rail.
 */
private val IndexDetailMinWidth = 680.dp

/** Widest the index grows without a hinge to stop at: a sidebar, not half the screen. */
private val IndexMaxWidth = 360.dp

/** Route the detail pane starts on, before any section is open. */
private const val PaneEmptyRoute = "pane/empty"

/**
 * An index with the chosen page beside it, like the iOS `NavigationSplitView`
 * of Discover, Profile and Search (Settings-style).
 *
 * Where [ColumnSplitBox] finds room for two columns, [index] is the left one
 * and [detail] the right; elsewhere only [collapsed] is shown, the phone's
 * pushing form, which is untouched. At a hinge the index fills the left
 * screen; without one it is a sidebar at most [IndexMaxWidth] wide, set off
 * by a hairline.
 */
@Composable
fun IndexDetailLayout(
    collapsed: @Composable () -> Unit,
    index: @Composable () -> Unit,
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hinge = rememberVerticalHinge()
    ColumnSplitBox(modifier = modifier.fillMaxSize(), minWidth = IndexDetailMinWidth) { split ->
        if (split == null) {
            collapsed()
        } else {
            val indexWidth = if (hinge != null) split.leftWidth else minOf(split.leftWidth, IndexMaxWidth)
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.width(indexWidth).fillMaxHeight()) { index() }
                Box(Modifier.width(split.gap).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    // A hinge is its own separator.
                    if (hinge == null) VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                Box(Modifier.weight(1f).fillMaxHeight()) { detail() }
            }
        }
    }
}

/**
 * The detail column of an [IndexDetailLayout]: a `NavHost` of its own on
 * [navController], so whatever the index opens is the very destination a
 * phone pushes (usually [drillNavGraphs][it.mensa.app.navigation.drillNavGraphs])
 * and keeps its own back stack. Open a page with [openInPane]; the index
 * highlights from [paneRoot].
 *
 * Mirrors the iOS detail `NavigationStack` with `.id(selection)`: opening
 * another section drops what was pushed inside the previous one.
 *
 * @param initialRoute section to open when the pane appears empty (iOS opens
 *   the first section when there is room); null leaves [empty] up.
 * @param empty shown while nothing is open.
 */
@Composable
fun IndexDetailPane(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    initialRoute: String? = null,
    empty: @Composable () -> Unit = {},
    builder: NavGraphBuilder.() -> Unit,
) {
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        NavHost(
            navController = navController,
            startDestination = PaneEmptyRoute,
            enterTransition = { fadeIn(tween(220)) },
            exitTransition = { fadeOut(tween(120)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = { fadeOut(tween(120)) },
        ) {
            composable(PaneEmptyRoute) { empty() }
            builder()
        }
    }
    // The graph is set by the NavHost above, so navigating from here is safe.
    LaunchedEffect(navController, initialRoute) {
        if (initialRoute != null && navController.currentPaneRoot() == null) {
            navController.openInPane(initialRoute)
        }
    }
}

/**
 * Opens [route] as the root of the detail pane, dropping what the section
 * open before had pushed. Opening the section that is already open returns
 * to its root.
 */
fun NavController.openInPane(route: String) {
    if (currentPaneRoot() == route) {
        popBackStack(route, inclusive = false)
    } else {
        navigate(route) { popUpTo(PaneEmptyRoute) }
    }
}

/**
 * Route of the section open in the detail pane (its root, not whatever is
 * pushed above it), or null while the pane is empty. Observed, for the index
 * to highlight its selection.
 */
@Composable
fun NavController.paneRoot(): String? {
    val stack by currentBackStack.collectAsState()
    return stack.paneRootRoute()
}

private fun NavController.currentPaneRoot(): String? = currentBackStack.value.paneRootRoute()

private fun List<NavBackStackEntry>.paneRootRoute(): String? =
    firstOrNull { it.destination !is NavGraph && it.destination.route != PaneEmptyRoute }?.destination?.route

