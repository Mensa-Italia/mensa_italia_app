package it.mensa.app.features.tickets

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

object TicketsRoutes {
    const val LIST = "tickets/list"
    const val DETAIL = "tickets/detail/{ticketId}"
    fun detail(ticketId: String) = "tickets/detail/$ticketId"
}

/**
 * ticketsNavGraph — wires Tickets routes into an existing [NavGraphBuilder].
 *
 * Usage (e.g. inside a NavHost):
 * ```
 * NavHost(navController, startDestination = TicketsRoutes.LIST) {
 *     ticketsNavGraph(navController)
 * }
 * ```
 */
fun NavGraphBuilder.ticketsNavGraph(navController: NavController) {
    composable(
        route = TicketsRoutes.LIST,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            ) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            ) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            ) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            ) + fadeOut()
        },
    ) {
        TicketsListScreen(
            onNavigateToDetail = { ticketId ->
                navController.navigate(TicketsRoutes.detail(ticketId))
            },
        )
    }

    composable(
        route = TicketsRoutes.DETAIL,
        arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            ) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            ) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            ) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f),
            ) + fadeOut()
        },
    ) { backStackEntry ->
        val ticketId = backStackEntry.arguments?.getString("ticketId").orEmpty()
        TicketDetailScreen(
            ticketId = ticketId,
            onBack = { navController.popBackStack() },
        )
    }
}
