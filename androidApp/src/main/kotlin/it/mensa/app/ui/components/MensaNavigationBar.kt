package it.mensa.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
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

data class MensaNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String,
)
