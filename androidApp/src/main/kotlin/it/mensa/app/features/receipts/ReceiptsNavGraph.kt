package it.mensa.app.features.receipts

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

object ReceiptsRoutes {
    const val LIST = "receipts/list"
    const val DETAIL = "receipts/detail/{receiptId}"
    fun detail(receiptId: String) = "receipts/detail/$receiptId"
}

/**
 * receiptsNavGraph — wires Receipts routes into an existing [NavGraphBuilder].
 *
 * Usage (e.g. inside a NavHost):
 * ```
 * NavHost(navController, startDestination = ReceiptsRoutes.LIST) {
 *     receiptsNavGraph(navController)
 * }
 * ```
 */
fun NavGraphBuilder.receiptsNavGraph(navController: NavController) {
    composable(
        route = ReceiptsRoutes.LIST,
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
        ReceiptsListScreen(
            onNavigateToDetail = { receiptId ->
                navController.navigate(ReceiptsRoutes.detail(receiptId))
            },
        )
    }

    composable(
        route = ReceiptsRoutes.DETAIL,
        arguments = listOf(navArgument("receiptId") { type = NavType.StringType }),
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
        val receiptId = backStackEntry.arguments?.getString("receiptId").orEmpty()
        ReceiptDetailScreen(
            receiptId = receiptId,
            onBack = { navController.popBackStack() },
        )
    }
}
