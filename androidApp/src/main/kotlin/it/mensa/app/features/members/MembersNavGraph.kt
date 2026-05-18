package it.mensa.app.features.members

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument

object MembersRoutes {
    const val DIRECTORY = "members/directory"
    const val DETAIL = "members/detail/{memberId}"
    fun detail(memberId: String) = "members/detail/$memberId"
}

fun NavGraphBuilder.membersNavGraph(navController: NavController) {
    composable(MembersRoutes.DIRECTORY) {
        MembersDirectoryScreen(
            onMemberClick = { memberId -> navController.navigate(MembersRoutes.detail(memberId)) },
            onBack = { navController.popBackStack() },
        )
    }
    composable(
        route = MembersRoutes.DETAIL,
        arguments = listOf(navArgument("memberId") { type = NavType.StringType }),
    ) { backStack ->
        val memberId = backStack.arguments?.getString("memberId") ?: return@composable
        MemberDetailScreen(
            memberId = memberId,
            onBack = { navController.popBackStack() },
        )
    }
}
