package it.mensa.app.features.localgroup

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

/** Routes of the "Gruppo locale" tab and of its admin area. */
object LocalGroupRoutes {
    /** The tab root: registered by the shell as a tab, not in [localGroupNavGraph]. */
    const val ROOT = "local_group"
    const val NEW_MEMBERS = "local_group/new_members"
    const val ADMIN = "local_group/admin"
    const val MEMBERS = "local_group/admin/members"
    const val STATS = "local_group/admin/stats"
    const val EVENTS = "local_group/admin/events"
}

/**
 * The pages of the "Gruppo locale" tab: the week's new members, open to
 * everyone, and the admin area ("Area segreteria" and its three pages). The tab root ([LocalGroupRoutes.ROOT]) is a tab destination of the
 * shell, so it is not registered here: two destinations with the same route
 * in one NavHost would replace each other.
 */
fun NavGraphBuilder.localGroupNavGraph(navController: NavController) {
    val back: () -> Unit = { navController.popBackStack() }
    val open: (String) -> Unit = { navController.navigate(it) }
    slide(LocalGroupRoutes.NEW_MEMBERS) { LocalGroupNewMembersScreen(onBack = back, onNavigate = open) }
    slide(LocalGroupRoutes.ADMIN) { LocalGroupAdminScreen(onBack = back, onNavigate = open) }
    slide(LocalGroupRoutes.MEMBERS) { LocalGroupMembersScreen(onBack = back, onNavigate = open) }
    slide(LocalGroupRoutes.STATS) { LocalGroupStatsScreen(onBack = back) }
    slide(LocalGroupRoutes.EVENTS) { LocalGroupEventsScreen(onBack = back, onNavigate = open) }
}

/** The same horizontal spring slide as the leaderboard's graph. */
private fun NavGraphBuilder.slide(route: String, content: @Composable (NavBackStackEntry) -> Unit) {
    composable(
        route = route,
        enterTransition = { enter() },
        exitTransition = { exit() },
        popEnterTransition = { popEnter() },
        popExitTransition = { popExit() },
    ) { entry -> content(entry) }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.enter(): EnterTransition =
    slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.exit(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnter(): EnterTransition =
    slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExit(): ExitTransition =
    slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut()
