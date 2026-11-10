package it.mensa.app.features.deals

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import it.mensa.app.features.locations.navigateToLocationPicker

// ─── Route constants ─────────────────────────────────────────────────────────

object DealsRoute {
    const val LIST = "deals/list"

    /** The list opened already filtered by a region (a region key, e.g. "Piemonte"). */
    const val LIST_IN_REGION = "deals/region/{region}"
    const val DETAIL = "deals/detail/{dealId}"
    const val ADD = "deals/add"
    const val EDIT = "deals/edit/{dealId}"

    const val ARG_DEAL_ID = "dealId"
    const val ARG_REGION = "region"

    fun listInRegion(region: String) = "deals/region/" + Uri.encode(region)

    fun detail(dealId: String) = "deals/detail/$dealId"
    fun edit(dealId: String) = "deals/edit/$dealId"
}

// ─── Nav graph builder ────────────────────────────────────────────────────────

/**
 * dealsNavGraph — registers all Deal routes into the given [NavGraphBuilder].
 *
 * Routes:
 * - [DealsRoute.LIST]   → DealListScreen
 * - [DealsRoute.LIST_IN_REGION] → DealListScreen filtered by region (arg: region)
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
        DealList(navController = navController, initialRegion = null)
    }

    composable(
        route = DealsRoute.LIST_IN_REGION,
        arguments = listOf(
            navArgument(DealsRoute.ARG_REGION) { type = NavType.StringType }
        ),
    ) { backStackEntry ->
        DealList(
            navController = navController,
            initialRegion = backStackEntry.arguments?.getString(DealsRoute.ARG_REGION),
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

    composable(DealsRoute.ADD) { backStackEntry ->
        AddDealScreen(
            dealId = null,
            backStackEntry = backStackEntry,
            onPickLocationClick = { current -> navController.navigateToLocationPicker(current?.id) },
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
            backStackEntry = backStackEntry,
            onPickLocationClick = { current -> navController.navigateToLocationPicker(current?.id) },
            onBack = { navController.popBackStack() },
        )
    }
}

/** The deals list, the same on both list routes. */
@Composable
private fun DealList(navController: NavController, initialRegion: String?) {
    DealListScreen(
        initialRegion = initialRegion,
        onNavigateToDetail = { dealId ->
            navController.navigate(DealsRoute.detail(dealId))
        },
        onNavigateToAdd = {
            navController.navigate(DealsRoute.ADD)
        },
        onBack = { navController.popBackStack() },
    )
}
