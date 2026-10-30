package it.mensa.app.features.discover

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import it.mensa.app.navigation.drillNavGraphs
import it.mensa.app.navigation.toRoute
import it.mensa.app.ui.components.IndexDetailLayout
import it.mensa.app.ui.components.IndexDetailPane
import it.mensa.app.ui.components.openInPane
import it.mensa.app.ui.components.paneRoot

/**
 * DiscoverTab — the Discover tab, on a phone or beside a page.
 *
 * Narrow, it is [DiscoverScreen] alone and every section is pushed on the
 * shell's `NavController` ([onCategoryClick], as before). With room for two
 * columns the list stays on the left and the chosen section opens on the
 * right, in a detail pane with a back stack of its own; the pane opens on
 * Events, like Settings opens on its first section.
 *
 * Mirrors iOS `DiscoverView` (`NavigationSplitView`, default `.events`).
 */
@Composable
fun DiscoverTab(
    onCategoryClick: (DiscoverCategory) -> Unit,
    onSearchTap: () -> Unit,
) {
    val pane = rememberNavController()
    IndexDetailLayout(
        collapsed = {
            DiscoverScreen(onCategoryClick = onCategoryClick, onSearchTap = onSearchTap)
        },
        index = {
            DiscoverScreen(
                onCategoryClick = { category -> category.toRoute()?.let(pane::openInPane) },
                onSearchTap = onSearchTap,
                selectedRoute = pane.paneRoot(),
            )
        },
        detail = {
            IndexDetailPane(
                navController = pane,
                initialRoute = DiscoverCategory.Events.toRoute(),
            ) { drillNavGraphs(pane) }
        },
    )
}
