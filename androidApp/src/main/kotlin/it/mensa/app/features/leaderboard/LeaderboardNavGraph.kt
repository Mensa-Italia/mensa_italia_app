package it.mensa.app.features.leaderboard

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import it.mensa.app.features.members.MembersRoutes

object LeaderboardRoutes {
    const val BOARD = "leaderboard"
}

/** The ranking, opened from Today's card. */
fun NavGraphBuilder.leaderboardNavGraph(navController: NavController) {
    composable(
        route = LeaderboardRoutes.BOARD,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = spring(0.8f, 300f)) + fadeIn() },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = spring(0.8f, 300f)) + fadeOut() },
    ) {
        LeaderboardScreen(
            onNavigateBack = { navController.popBackStack() },
            onMemberClick = { id -> navController.navigate(MembersRoutes.detail(id)) },
        )
    }
}
