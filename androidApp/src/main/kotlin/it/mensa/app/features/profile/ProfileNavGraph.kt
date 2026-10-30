package it.mensa.app.features.profile

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.mensa.app.features.profile.sub.CalendarLinkerScreen
import it.mensa.app.features.profile.sub.CreditsScreen
import it.mensa.app.features.profile.sub.DevicesScreen
import it.mensa.app.features.profile.sub.LanguagePickerScreen
import it.mensa.app.features.profile.sub.MakeDonationScreen
import it.mensa.app.features.profile.sub.OrgChartScreen
import it.mensa.app.features.profile.sub.PasskeysScreen
import it.mensa.app.features.profile.sub.PaymentMethodsScreen
import it.mensa.app.features.profile.sub.RenewMembershipScreen
import it.mensa.app.features.receipts.ReceiptsRoutes
import it.mensa.app.navigation.drillNavGraphs
import it.mensa.app.support.ExternalLinks
import it.mensa.app.ui.components.IndexDetailLayout
import it.mensa.app.ui.components.IndexDetailPane
import it.mensa.app.ui.components.openInPane
import it.mensa.app.ui.components.paneRoot

sealed class ProfileRoute(val route: String) {
    object Main : ProfileRoute("profile_main")
    object LanguagePicker : ProfileRoute("profile_language")
    object PaymentMethods : ProfileRoute("profile_payments")
    object RenewMembership : ProfileRoute("profile_renew")
    object MakeDonation : ProfileRoute("profile_donation")
    object CalendarLinker : ProfileRoute("profile_calendar")
    object Devices : ProfileRoute("profile_devices")
    object Passkeys : ProfileRoute("profile_passkeys")
    object OrgChart : ProfileRoute("profile_orgchart")
    object Credits : ProfileRoute("profile_credits")
    object PrivacyPolicy : ProfileRoute("profile_privacy")
    object Terms : ProfileRoute("profile_terms")
}

private const val PRIVACY_URL = "https://www.mensa.it/privacy"
private const val TERMS_URL = "https://www.mensa.it/privacy-policy/"

/**
 * ProfileNavGraph — the Profile tab, on a phone or beside a page.
 *
 * Narrow, it is the nested stack of [ProfileStack] (unchanged). With room for
 * two columns the profile list stays on the left and the chosen section opens
 * on the right, in a detail pane with a back stack of its own; the pane
 * opens on the membership, like Settings opens on its first section. Rows
 * that act in place (theme, notifications, external links) do not open a pane.
 *
 * Mirrors iOS `ProfileView` (`NavigationSplitView`, default `.membership`).
 */
@Composable
fun ProfileNavGraph(
    navController: NavHostController = rememberNavController(),
    onSearchTap: () -> Unit = {},
    onReceiptsClick: () -> Unit = {},
    onAtRootChange: (Boolean) -> Unit = {},
) {
    val pane = rememberNavController()
    val context = LocalContext.current
    IndexDetailLayout(
        collapsed = {
            ProfileStack(navController, onSearchTap, onReceiptsClick, onAtRootChange)
        },
        index = {
            // Beside the page the list is always on screen: the shell never
            // has anything of Profile to hide its navigation behind.
            LaunchedEffect(Unit) { onAtRootChange(true) }
            ProfileScreen(
                onNavigate = { route ->
                    when (route) {
                        ProfileRoute.PrivacyPolicy -> ExternalLinks.open(context, PRIVACY_URL)
                        ProfileRoute.Terms -> ExternalLinks.open(context, TERMS_URL)
                        else -> pane.openInPane(route.route)
                    }
                },
                onSearchTap = onSearchTap,
                onReceiptsClick = { pane.openInPane(ReceiptsRoutes.LIST) },
                selectedRoute = pane.paneRoot(),
            )
        },
        detail = {
            IndexDetailPane(
                navController = pane,
                initialRoute = ProfileRoute.RenewMembership.route,
            ) {
                profileSubRoutes(pane)
                drillNavGraphs(pane)
            }
        },
    )
}

/** The phone form: the profile list with each section pushed on [navController]. */
@Composable
private fun ProfileStack(
    navController: NavHostController,
    onSearchTap: () -> Unit,
    onReceiptsClick: () -> Unit,
    onAtRootChange: (Boolean) -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry?.destination?.route) {
        onAtRootChange(backStackEntry?.destination?.route == ProfileRoute.Main.route)
    }

    NavHost(
        navController = navController,
        startDestination = ProfileRoute.Main.route,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ) + fadeIn()
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ) + fadeOut()
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ) + fadeIn()
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = spring(dampingRatio = 0.8f, stiffness = 300f)
            ) + fadeOut()
        },
    ) {
        composable(ProfileRoute.Main.route) {
            ProfileScreen(
                onNavigate = { route -> navController.navigate(route.route) },
                onSearchTap = onSearchTap,
                onReceiptsClick = onReceiptsClick,
            )
        }
        profileSubRoutes(navController)
    }
}

/**
 * The pages a profile row opens, registered into the host `NavHost`: the
 * phone's [ProfileStack] and the detail pane of the wide layout.
 */
private fun NavGraphBuilder.profileSubRoutes(navController: NavController) {
    composable(ProfileRoute.LanguagePicker.route) {
        LanguagePickerScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.PaymentMethods.route) {
        PaymentMethodsScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.RenewMembership.route) {
        RenewMembershipScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.MakeDonation.route) {
        MakeDonationScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.CalendarLinker.route) {
        CalendarLinkerScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.Devices.route) {
        DevicesScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.Passkeys.route) {
        PasskeysScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.OrgChart.route) {
        OrgChartScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.Credits.route) {
        CreditsScreen(onBack = { navController.popBackStack() })
    }
    composable(ProfileRoute.PrivacyPolicy.route) {
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            ExternalLinks.open(context, PRIVACY_URL)
            navController.popBackStack()
        }
    }
    composable(ProfileRoute.Terms.route) {
        val context = androidx.compose.ui.platform.LocalContext.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            ExternalLinks.open(context, TERMS_URL)
            navController.popBackStack()
        }
    }
}
