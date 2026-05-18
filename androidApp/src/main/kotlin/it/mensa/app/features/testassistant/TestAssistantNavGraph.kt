package it.mensa.app.features.testassistant

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object TestAssistantRoutes {
    const val DASHBOARD = "testassistant/dashboard"
}

/**
 * testAssistantNavGraph — wires TestAssistant routes into an existing [NavGraphBuilder].
 *
 * Routes:
 *  - [TestAssistantRoutes.DASHBOARD] → TestAssistantScreen (gated by "testmakers" power)
 */
fun NavGraphBuilder.testAssistantNavGraph(navController: NavController) {
    composable(
        route = TestAssistantRoutes.DASHBOARD,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
    ) {
        TestAssistantScreen(
            onBack = { navController.popBackStack() },
        )
    }
}
