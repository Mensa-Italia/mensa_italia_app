package it.mensa.app.features.tableport

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object TableportRoutes {
    const val PASSPORT = "tableport/passport"
    const val SCAN     = "tableport/scan"
}

/**
 * tableportNavGraph — wires Tableport routes into an existing [NavGraphBuilder].
 *
 * Routes:
 *  - [TableportRoutes.PASSPORT] → PassportScreen (skeuomorphic passport with stamps)
 *  - [TableportRoutes.SCAN]     → QrScannerScreen (CameraX + ML Kit QR detection)
 */
fun NavGraphBuilder.tableportNavGraph(navController: NavController) {
    composable(
        route = TableportRoutes.PASSPORT,
        enterTransition = {
            slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut()
        },
    ) { backStackEntry ->
        PassportScreen(
            onNavigateToScanner = { navController.navigate(TableportRoutes.SCAN) },
            onNavigateBack = { navController.popBackStack() },
            backStackEntry = backStackEntry,
        )
    }

    composable(
        route = TableportRoutes.SCAN,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
    ) {
        QrScannerScreen(
            onScanned = { stampId, code ->
                // Pop back to passport and pass the scan result via SavedStateHandle
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("qr_stamp_id", stampId)
                navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.set("qr_code", code)
                navController.popBackStack()
            },
            onCancel = { navController.popBackStack() },
        )
    }
}
