package it.mensa.app.features.podcasts

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

object PodcastsRoutes {
    const val LIST = "podcasts/list"
    const val EPISODES = "podcasts/episodes/{podcastId}"
    fun episodes(podcastId: String) = "podcasts/episodes/$podcastId"
}

fun NavGraphBuilder.podcastsNavGraph(navController: NavController) {
    composable(
        route = PodcastsRoutes.LIST,
        enterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeIn() },
        exitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeOut() },
    ) {
        PodcastsListScreen(
            onNavigateToEpisodes = { podcastId, _ ->
                navController.navigate(PodcastsRoutes.episodes(podcastId))
            },
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = PodcastsRoutes.EPISODES,
        arguments = listOf(navArgument("podcastId") { type = NavType.StringType }),
        enterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeIn() },
        exitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeOut() },
    ) { backStackEntry ->
        val podcastId = backStackEntry.arguments?.getString("podcastId").orEmpty()
        PodcastEpisodesScreen(
            podcastId = podcastId,
            podcastTitle = "",
            onBack = { navController.popBackStack() },
        )
    }
}
