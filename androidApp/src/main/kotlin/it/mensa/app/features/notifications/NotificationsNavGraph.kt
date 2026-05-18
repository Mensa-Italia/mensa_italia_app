package it.mensa.app.features.notifications

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

// ─── Route constants ──────────────────────────────────────────────────────────

object NotificationsRoutes {
    const val LIST = "notifications/list"
    const val DETAIL = "notifications/detail/{notificationId}"
    const val MANAGER = "notifications/manager"
    const val ARG_NOTIFICATION_ID = "notificationId"
    fun detail(id: String) = "notifications/detail/$id"
}

// ─── NavGraph extension ───────────────────────────────────────────────────────

/**
 * notificationsNavGraph — adds Notifications routes to an existing [NavGraphBuilder].
 *
 * Routes:
 *  - notifications/list     → NotificationsListScreen
 *  - notifications/detail/{notificationId} → NotificationDetailScreen
 *  - notifications/manager  → NotificationManagerScreen
 *
 * Mirrors iOS NavigationStack { NotificationsListView() } with nested destinations.
 */
fun NavGraphBuilder.notificationsNavGraph(navController: NavController) {

    val enterTransition = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    ) + fadeIn()

    val exitTransition = slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    ) + fadeOut()

    val popEnterTransition = slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    ) + fadeIn()

    val popExitTransition = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
    ) + fadeOut()

    composable(
        route = NotificationsRoutes.LIST,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition },
    ) {
        NotificationsListScreen(navController = navController)
    }

    composable(
        route = NotificationsRoutes.DETAIL,
        arguments = listOf(navArgument(NotificationsRoutes.ARG_NOTIFICATION_ID) {
            type = NavType.StringType
        }),
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition },
    ) { backStack ->
        val notificationId = backStack.arguments
            ?.getString(NotificationsRoutes.ARG_NOTIFICATION_ID) ?: return@composable
        NotificationDetailScreen(
            notificationId = notificationId,
            navController = navController,
        )
    }

    composable(
        route = NotificationsRoutes.MANAGER,
        enterTransition = { enterTransition },
        exitTransition = { exitTransition },
        popEnterTransition = { popEnterTransition },
        popExitTransition = { popExitTransition },
    ) {
        NotificationManagerScreen(onBack = { navController.popBackStack() })
    }
}
