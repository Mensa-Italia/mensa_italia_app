package it.mensa.app.features.deals

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

// ─── Route constants ─────────────────────────────────────────────────────────

object DealsRoute {
    const val LIST = "deals/list"
    const val DETAIL = "deals/detail/{dealId}"
    const val ADD = "deals/add"
    const val EDIT = "deals/edit/{dealId}"

    const val ARG_DEAL_ID = "dealId"

    fun detail(dealId: String) = "deals/detail/$dealId"
    fun edit(dealId: String) = "deals/edit/$dealId"
}

// ─── Nav graph builder ────────────────────────────────────────────────────────

/**
 * dealsNavGraph — registers all Deal routes into the given [NavGraphBuilder].
 *
 * Routes:
 * - [DealsRoute.LIST]   → DealListScreen
 * - [DealsRoute.DETAIL] → DealDetailScreen (arg: dealId)
 * - [DealsRoute.ADD]    → AddDealScreen (create mode)
 * - [DealsRoute.EDIT]   → AddDealScreen (edit mode, arg: dealId)
 *
 * Usage:
 * ```
 * NavHost(...) {
 *     dealsNavGraph(navController)
 * }
 * ```
 */
fun NavGraphBuilder.dealsNavGraph(navController: NavController) {

    composable(DealsRoute.LIST) {
        DealListScreen(
            onNavigateToDetail = { dealId ->
                navController.navigate(DealsRoute.detail(dealId))
            },
            onNavigateToAdd = {
                navController.navigate(DealsRoute.ADD)
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = DealsRoute.DETAIL,
        arguments = listOf(
            navArgument(DealsRoute.ARG_DEAL_ID) { type = NavType.StringType }
        ),
    ) { backStackEntry ->
        val dealId = backStackEntry.arguments?.getString(DealsRoute.ARG_DEAL_ID) ?: return@composable
        DealDetailScreen(
            dealId = dealId,
            onBack = { navController.popBackStack() },
            onNavigateToEdit = { id ->
                navController.navigate(DealsRoute.edit(id))
            },
        )
    }

    composable(DealsRoute.ADD) {
        AddDealScreen(
            dealId = null,
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = DealsRoute.EDIT,
        arguments = listOf(
            navArgument(DealsRoute.ARG_DEAL_ID) { type = NavType.StringType }
        ),
    ) { backStackEntry ->
        val dealId = backStackEntry.arguments?.getString(DealsRoute.ARG_DEAL_ID) ?: return@composable
        AddDealScreen(
            dealId = dealId,
            onBack = { navController.popBackStack() },
        )
    }
}
