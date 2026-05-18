package it.mensa.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState

private val tabIcons: Map<String, ImageVector> = mapOf(
    Route.Today.path to Icons.Default.Home,
    Route.Discover.path to Icons.Default.Explore,
    Route.Search.path to Icons.Default.Search,
    Route.Card.path to Icons.Default.CreditCard,
    Route.Profile.path to Icons.Default.AccountCircle,
)

/**
 * MainBottomNav — M3 NavigationBar with 5 Mensa tabs.
 *
 * Uses placeholder icons from Material Icons Extended.
 * Feature agents should replace icons with branded SVG vectors
 * as their tabs are implemented.
 *
 * @param navController root NavController to observe/navigate
 */
@Composable
fun MainBottomNav(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        mensaBottomNavTabs.forEach { tab ->
            val icon = tabIcons[tab.route.path] ?: Icons.Default.Home
            NavigationBarItem(
                selected = currentRoute == tab.route.path,
                onClick = {
                    navController.navigate(tab.route.path) {
                        popUpTo(Route.Today.path) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = tab.contentDescription,
                    )
                },
                label = { Text(tab.label) },
            )
        }
    }
}
