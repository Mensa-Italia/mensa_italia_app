package it.mensa.app.features.profile

import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import android.content.Intent
import android.net.Uri
import it.mensa.app.features.profile.sub.CalendarLinkerScreen
import it.mensa.app.features.profile.sub.CreditsScreen
import it.mensa.app.features.profile.sub.DevicesScreen
import it.mensa.app.features.profile.sub.LanguagePickerScreen
import it.mensa.app.features.profile.sub.MakeDonationScreen
import it.mensa.app.features.profile.sub.OrgChartScreen
import it.mensa.app.features.profile.sub.PaymentMethodsScreen
import it.mensa.app.features.profile.sub.RenewMembershipScreen

sealed class ProfileRoute(val route: String) {
    object Main : ProfileRoute("profile_main")
    object LanguagePicker : ProfileRoute("profile_language")
    object PaymentMethods : ProfileRoute("profile_payments")
    object RenewMembership : ProfileRoute("profile_renew")
    object MakeDonation : ProfileRoute("profile_donation")
    object CalendarLinker : ProfileRoute("profile_calendar")
    object Devices : ProfileRoute("profile_devices")
    object OrgChart : ProfileRoute("profile_orgchart")
    object Credits : ProfileRoute("profile_credits")
    object PrivacyPolicy : ProfileRoute("profile_privacy")
    object Terms : ProfileRoute("profile_terms")
}

@Composable
fun ProfileNavGraph(
    navController: NavHostController = rememberNavController(),
    onSearchTap: () -> Unit = {},
    onAtRootChange: (Boolean) -> Unit = {},
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
            )
        }
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
        composable(ProfileRoute.OrgChart.route) {
            OrgChartScreen(onBack = { navController.popBackStack() })
        }
        composable(ProfileRoute.Credits.route) {
            CreditsScreen(onBack = { navController.popBackStack() })
        }
        composable(ProfileRoute.PrivacyPolicy.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mensa.it/privacy")))
                navController.popBackStack()
            }
        }
        composable(ProfileRoute.Terms.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.runtime.LaunchedEffect(Unit) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.mensa.it/termini")))
                navController.popBackStack()
            }
        }
    }
}
