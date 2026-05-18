package it.mensa.app.features.addonshub

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object AddonsHubRoutes {
    const val HUB = "addons/hub"
}

/**
 * addonsHubNavGraph — wires AddonsHub routes into an existing [NavGraphBuilder].
 *
 * Routes:
 *  - [AddonsHubRoutes.HUB] → AddonsHubScreen (2-col grid of available addons)
 *
 * The [onAddonClick] callback is forwarded to [AddonsHubScreen] so the parent
 * NavGraph can push the appropriate addon destination (tableport, boutique, etc.).
 */
fun NavGraphBuilder.addonsHubNavGraph(
    navController: NavController,
    onAddonClick: (addonId: String) -> Unit = {},
) {
    composable(
        route = AddonsHubRoutes.HUB,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
    ) {
        AddonsHubScreen(
            onAddonClick = onAddonClick,
            onBack = { navController.popBackStack() },
        )
    }
}
