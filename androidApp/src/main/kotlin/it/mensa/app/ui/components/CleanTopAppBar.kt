package it.mensa.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * CleanTopAppBar — M3 LargeTopAppBar styled for Mensa.
 *
 * @deprecated Use [MensaTopAppBar] for the new expressive large variant, or
 *   [MensaTopAppBarSmall] for compact drill screens.
 *   This shim is kept to avoid breaking existing callsites.
 *
 * @param title composable for the title slot
 * @param scrollBehavior provide [TopAppBarDefaults.exitUntilCollapsedScrollBehavior]
 * @param navigationIcon optional back/drawer icon
 * @param actions trailing icon buttons
 */
@Deprecated(
    message = "Use MensaTopAppBar (large/kicker variant) or MensaTopAppBarSmall. " +
        "CleanTopAppBar accepts a Composable title slot; MensaTopAppBar accepts a String. " +
        "Wave 2 will migrate callsites.",
    replaceWith = ReplaceWith(
        "MensaTopAppBar(title = \"...\", scrollBehavior = scrollBehavior, ...)",
        imports = ["it.mensa.app.ui.components.MensaTopAppBar"],
    ),
)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanTopAppBar(
    title: @Composable () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val fraction = scrollBehavior.state.collapsedFraction
    val containerColor = colorScheme.surfaceContainer.copy(alpha = fraction)

    LargeTopAppBar(
        title = title,
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = colorScheme.surfaceContainer,
            titleContentColor = colorScheme.onSurface,
            navigationIconContentColor = colorScheme.onSurface,
            actionIconContentColor = colorScheme.onSurfaceVariant,
        ),
    )
}
