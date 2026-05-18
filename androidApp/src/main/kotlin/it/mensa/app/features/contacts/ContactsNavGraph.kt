package it.mensa.app.features.contacts

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable

object ContactsRoutes {
    const val LIST = "contacts/list"
}

fun NavGraphBuilder.contactsNavGraph(navController: NavController) {
    composable(
        route = ContactsRoutes.LIST,
        enterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeIn() },
        exitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeOut() },
        popEnterTransition = { slideInHorizontally(animationSpec = spring(0.8f, 300f)) { -it / 3 } + fadeIn() },
        popExitTransition = { slideOutHorizontally(animationSpec = spring(0.8f, 300f)) { it } + fadeOut() },
    ) {
        ContactsAddonScreen(
            onBack = { navController.popBackStack() },
        )
    }
}
