package it.mensa.app.features.external

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

object ExternalRoutes {
    const val ADDON = "external/{addonId}"
    fun addon(addonId: String) = "external/$addonId"
}

/**
 * externalNavGraph — routes for external addon WebView.
 *
 * The AddonModel's [baseUrl] must be passed via saved state handle or as
 * additional nav arg. For simplicity the route encodes only the addonId;
 * the ViewModel resolves the URL from the shared addons repo via getById().
 * Callers that have the URL handy can pass it directly, but the VM also
 * falls back to fetching from the local DB.
 *
 * Usage: navController.navigate(ExternalRoutes.addon(addonId))
 */
fun NavGraphBuilder.externalNavGraph(
    navController: NavController,
    /** Provide the base URL from caller context when navigating. Null = VM resolves from DB. */
    resolveBaseUrl: suspend (addonId: String) -> String = { "" },
) {
    composable(
        route = ExternalRoutes.ADDON,
        arguments = listOf(navArgument("addonId") { type = NavType.StringType }),
        enterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeIn() },
        exitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeOut() },
    ) { backStackEntry ->
        val addonId = backStackEntry.arguments?.getString("addonId").orEmpty()
        // baseUrl and title can be passed via the saved state handle in the future.
        // For now, the screen accepts empty baseUrl and the VM's load() surfaces an error.
        val baseUrl = backStackEntry.savedStateHandle.get<String>("baseUrl") ?: ""
        val title = backStackEntry.savedStateHandle.get<String>("addonTitle") ?: ""
        ExternalAddonScreen(
            addonId = addonId,
            baseUrl = baseUrl,
            title = title,
            onBack = { navController.popBackStack() },
        )
    }
}
