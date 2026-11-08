package it.mensa.app.features.tableport

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import it.mensa.app.features.tableport.medals.MedalCabinetScreen

object TableportRoutes {
    const val PASSPORT = "tableport/passport"
    const val SCAN     = "tableport/scan"
    const val MEDALS   = "tableport/medals"

    const val ARG_PAGE = "page"

    /** [MEDALS] with the optional page to open on: `region` for "La mia regione". */
    const val MEDALS_PATTERN = "$MEDALS?$ARG_PAGE={$ARG_PAGE}"
    private const val PAGE_REGION = "region"

    /** The medal collection, on "La mia regione" when [region]. */
    fun medals(region: Boolean = false): String = if (region) "$MEDALS?$ARG_PAGE=$PAGE_REGION" else MEDALS

    internal fun isRegionPage(value: String?): Boolean = value == PAGE_REGION
}

/**
 * tableportNavGraph — wires Tableport routes into an existing [NavGraphBuilder].
 *
 * Routes:
 *  - [TableportRoutes.PASSPORT] → PassportScreen (skeuomorphic passport with stamps)
 *  - [TableportRoutes.SCAN]     → QrScannerScreen (CameraX + ML Kit QR detection)
 *  - [TableportRoutes.MEDALS]   → MedalCabinetScreen (the medal collection)
 */
fun NavGraphBuilder.tableportNavGraph(navController: NavController) {
    composable(
        route = TableportRoutes.PASSPORT,
        // Sale dal basso come una scheda e ci torna chiudendolo, come su iOS
        // (`fullScreenCover`). Verso scanner e medagliere resta orizzontale.
        enterTransition = {
            slideInVertically(initialOffsetY = { it }, animationSpec = spring(0.9f, 400f))
        },
        exitTransition = {
            slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn()
        },
        popExitTransition = {
            slideOutVertically(targetOffsetY = { it }, animationSpec = spring(0.9f, 400f))
        },
    ) { backStackEntry ->
        PassportScreen(
            onNavigateToScanner = { navController.navigate(TableportRoutes.SCAN) },
            onNavigateToMedals = { navController.navigate(TableportRoutes.MEDALS) },
            onNavigateBack = { navController.popBackStack() },
            backStackEntry = backStackEntry,
        )
    }

    composable(
        route = TableportRoutes.MEDALS_PATTERN,
        arguments = listOf(
            navArgument(TableportRoutes.ARG_PAGE) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
    ) { entry ->
        MedalCabinetScreen(
            onNavigateBack = { navController.popBackStack() },
            initialRegion = TableportRoutes.isRegionPage(entry.arguments?.getString(TableportRoutes.ARG_PAGE)),
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
