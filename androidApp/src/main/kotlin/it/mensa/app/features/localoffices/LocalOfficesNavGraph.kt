package it.mensa.app.features.localoffices

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object LocalOfficesRoutes {
    const val LIST = "local_offices/list"
    const val DETAIL = "local_offices/detail/{officeId}"
    const val LINKTREE = "local_offices/linktree/{officeId}"
    fun detail(officeId: String) = "local_offices/detail/$officeId"
    fun linktree(officeId: String) = "local_offices/linktree/$officeId"
}

fun NavGraphBuilder.localOfficesNavGraph(navController: NavController) {
    composable(LocalOfficesRoutes.LIST) {
        LocalOfficesListScreen(
            onOfficeClick = { officeId -> navController.navigate(LocalOfficesRoutes.detail(officeId)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        route = LocalOfficesRoutes.DETAIL,
        arguments = listOf(navArgument("officeId") { type = NavType.StringType }),
    ) { backStack ->
        val officeId = backStack.arguments?.getString("officeId") ?: return@composable
        LocalOfficeScreen(
            officeId = officeId,
            onBack = { navController.popBackStack() },
            onLinktreeClick = { id -> navController.navigate(LocalOfficesRoutes.linktree(id)) },
            onEventClick = { eventId -> navController.navigate("events/detail/$eventId") },
            onSigClick = { sigId -> navController.navigate("sigs/detail/$sigId") },
            onMemberClick = { memberId -> navController.navigate("members/detail/$memberId") },
        )
    }
    composable(
        route = LocalOfficesRoutes.LINKTREE,
        arguments = listOf(navArgument("officeId") { type = NavType.StringType }),
    ) { backStack ->
        val officeId = backStack.arguments?.getString("officeId") ?: return@composable
        LocalOfficeLinktreeScreen(
            officeId = officeId,
            onBack = { navController.popBackStack() },
        )
    }
}
