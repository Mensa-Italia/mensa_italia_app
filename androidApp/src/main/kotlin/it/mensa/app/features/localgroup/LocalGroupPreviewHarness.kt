package it.mensa.app.features.localgroup

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.mensa.app.navigation.drillNavGraphs
import it.mensa.app.ui.shell.MainAppShell

/**
 * Debug harness: the "Gruppo locale" pages on [it.mensa.shared.localgroup.LocalGroupFixtures],
 * mounted without the login (`--es mensa_local_group_fixture 1`, plus
 * `--es mensa_local_group_admin 1` for the admin area):
 * - `local_group_preview`: the whole shell, opened on the tab (the bar shows it);
 * - `local_group_admin_preview`, `local_group_members_preview`,
 *   `local_group_stats_preview`, `local_group_events_preview`,
 *   `local_group_new_members_preview`, and for the board (admin fixture)
 *   `local_group_offices_preview`, `local_group_association_preview`: that page in a
 *   NavHost with every drill graph, so its rows still navigate.
 * Reached only through [it.mensa.app.support.LaunchHarness], inert on release builds.
 */
@Composable
fun LocalGroupPreviewHarness(screen: String) {
    val start = when (screen) {
        "local_group_admin_preview" -> LocalGroupRoutes.ADMIN
        "local_group_members_preview" -> LocalGroupRoutes.MEMBERS
        "local_group_stats_preview" -> LocalGroupRoutes.STATS
        "local_group_events_preview" -> LocalGroupRoutes.EVENTS
        "local_group_new_members_preview" -> LocalGroupRoutes.NEW_MEMBERS
        "local_group_offices_preview" -> LocalGroupRoutes.OFFICES
        "local_group_association_preview" -> LocalGroupRoutes.ASSOCIATION
        else -> null
    }
    if (start == null) {
        MainAppShell()
        return
    }
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = start) {
        composable(LocalGroupRoutes.ROOT) { LocalGroupScreen(onNavigate = { navController.navigate(it) }) }
        drillNavGraphs(navController)
    }
}
