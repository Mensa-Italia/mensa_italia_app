package it.mensa.app.ui.components

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * MensaNavigationBar — thin wrapper over the canonical M3 [NavigationBar].
 *
 * Uses [NavigationBarItem] for each tab so we inherit:
 *  - Pill indicator drenched in `secondaryContainer` (M3 Expressive default)
 *  - Active label in `labelMedium` (or `labelMediumEmphasized` on Expressive)
 *  - Correct sizing, elevation, ripple, a11y semantics
 *  - Future-proof: any Compose Material 3 update tweaks the look here for free
 *
 * Container color is `NavigationBarDefaults.containerColor` (M3
 * `surfaceContainer`) — edge-to-edge full-width with no top corner rounding,
 * matching the M3 spec (the previous custom "lifted card" look was iOS-ish).
 */
@Composable
fun MensaNavigationBar(
    items: List<MensaNavItem>,
    selectedRoute: String?,
    onItemSelect: (MensaNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        modifier = modifier,
        containerColor = NavigationBarDefaults.containerColor,
        tonalElevation = NavigationBarDefaults.Elevation,
    ) {
        items.forEach { item ->
            val selected = item.route == selectedRoute
            NavigationBarItem(
                selected = selected,
                onClick = { onItemSelect(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                alwaysShowLabel = true,
                colors = NavigationBarItemDefaults.colors(),
            )
        }
    }
}

/**
 * MensaNavigationRail — the side counterpart of [MensaNavigationBar], for a
 * window at least [TwoColumnMinWidth] wide (iPhone Duo unfolded, iPad on iOS,
 * where the system turns the tab bar into a vertical bar on the side).
 *
 * Collapsed M3 Expressive [WideNavigationRail]: same items, icon above the
 * label, same selection indicator as the bar. The rail pads itself for the
 * status bar, the system bars and a display cutout on its side.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MensaNavigationRail(
    items: List<MensaNavItem>,
    selectedRoute: String?,
    onItemSelect: (MensaNavItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    WideNavigationRail(
        modifier = modifier.fillMaxHeight(),
        state = rememberWideNavigationRailState(WideNavigationRailValue.Collapsed),
    ) {
        items.forEach { item ->
            WideNavigationRailItem(
                selected = item.route == selectedRoute,
                onClick = { onItemSelect(item) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                railExpanded = false,
            )
        }
    }
}

data class MensaNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
)
