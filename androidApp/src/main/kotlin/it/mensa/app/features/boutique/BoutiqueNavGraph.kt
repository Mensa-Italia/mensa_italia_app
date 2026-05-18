package it.mensa.app.features.boutique

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

object BoutiqueRoutes {
    const val LIST    = "boutique/list"
    const val PRODUCT = "boutique/product/{id}"
    fun product(id: String) = "boutique/product/$id"
}

/**
 * boutiqueNavGraph — wires Boutique routes into an existing [NavGraphBuilder].
 *
 * Routes:
 *  - [BoutiqueRoutes.LIST]    → BoutiqueListScreen (2-col grid with search)
 *  - [BoutiqueRoutes.PRODUCT] → BoutiqueProductScreen (gallery + CTA Chrome Custom Tab)
 */
fun NavGraphBuilder.boutiqueNavGraph(navController: NavController) {
    composable(
        route = BoutiqueRoutes.LIST,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
    ) {
        BoutiqueListScreen(
            onNavigateToProduct = { id -> navController.navigate(BoutiqueRoutes.product(id)) },
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = BoutiqueRoutes.PRODUCT,
        arguments = listOf(navArgument("id") { type = NavType.StringType }),
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
    ) { backStackEntry ->
        val productId = backStackEntry.arguments?.getString("id").orEmpty()
        BoutiqueProductScreen(
            productId = productId,
            onBack = { navController.popBackStack() },
        )
    }
}
