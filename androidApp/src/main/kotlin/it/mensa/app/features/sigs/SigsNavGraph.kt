package it.mensa.app.features.sigs

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object SigsRoutes {
    const val LIST = "sigs/list"
    const val DETAIL = "sigs/detail/{sigId}"
    fun detail(sigId: String) = "sigs/detail/$sigId"
}

fun NavGraphBuilder.sigsNavGraph(navController: NavController) {
    composable(SigsRoutes.LIST) {
        SigListScreen(
            onSigClick = { sigId -> navController.navigate(SigsRoutes.detail(sigId)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        route = SigsRoutes.DETAIL,
        arguments = listOf(navArgument("sigId") { type = NavType.StringType }),
    ) { backStack ->
        val sigId = backStack.arguments?.getString("sigId") ?: return@composable
        SigDetailScreen(
            sigId = sigId,
            onBack = { navController.popBackStack() },
        )
    }
}
