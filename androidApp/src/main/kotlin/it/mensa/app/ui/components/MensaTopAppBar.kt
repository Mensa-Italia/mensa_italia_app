package it.mensa.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.theme.ExpressiveTypography

/**
 * MensaTopAppBar — Large emphasized variant.
 *
 * - Expanded: transparent container, headlineLarge Gotham ExtraBold title,
 *   optional kicker label (uppercase) animates in above title
 * - Collapsed: surfaceContainer background, title becomes standard top-bar size
 * - Kicker label fades out as user scrolls (collapsedFraction > 0.3)
 *
 * @param title main title text
 * @param kicker optional kicker label shown above title when expanded (uppercase)
 * @param scrollBehavior use TopAppBarDefaults.exitUntilCollapsedScrollBehavior
 * @param navigationIcon optional back arrow / drawer icon
 * @param actions trailing action icons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensaTopAppBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colorScheme = MaterialTheme.colorScheme
    val fraction = scrollBehavior.state.collapsedFraction

    // Container color: matches the M3 canonical AppBarTokens spec —
    //   at rest (fraction=0): surface (= body), so bar reads as continuous
    //   scrolled (fraction=1): surfaceContainer, signalling content beneath
    // Same tonal step that TopAppBarDefaults.largeTopAppBarColors() uses by
    // default. We interpolate via alpha so the transition is smooth instead
    // of a hard switch.
    val containerColor = colorScheme.surfaceContainer.copy(alpha = fraction)
    val scrolledContainerColor = colorScheme.surfaceContainer

    // Kicker alpha: fully visible when expanded (fraction < 0.2), fades out as scroll approaches 0.5
    val kickerAlpha by animateFloatAsState(
        targetValue = if (fraction < 0.25f) 1f else 0f,
        animationSpec = tween(durationMillis = 150),
        label = "KickerAlpha",
    )

    LargeTopAppBar(
        title = {
            if (kicker != null && kickerAlpha > 0f) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = kicker.uppercase(),
                        style = ExpressiveTypography.kickerLabel,
                        color = colorScheme.primary,
                        modifier = Modifier.alpha(kickerAlpha),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineLarge,
                        color = colorScheme.onSurface,
                    )
                }
            } else {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = colorScheme.onSurface,
                )
            }
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.largeTopAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = scrolledContainerColor,
            titleContentColor = colorScheme.onSurface,
            navigationIconContentColor = colorScheme.onSurface,
            actionIconContentColor = colorScheme.onSurfaceVariant,
        ),
    )
}

/**
 * MensaTopAppBarSmall — compact single-line top bar.
 *
 * Use on drill screens (detail views, settings) where vertical real estate matters.
 * Background: surfaceContainer (opaque), no scroll behavior needed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensaTopAppBarSmall(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    val colorScheme = MaterialTheme.colorScheme

    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = colorScheme.onSurface,
            )
        },
        modifier = modifier,
        navigationIcon = navigationIcon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            // Per AppBarTokens spec:
            //   ContainerColor         = Surface          (at rest)
            //   OnScrollContainerColor = SurfaceContainer (content scrolled under)
            // If caller supplies a scrollBehavior, TopAppBar will lerp between
            // these automatically based on overlappedFraction.
            containerColor = colorScheme.surface,
            scrolledContainerColor = colorScheme.surfaceContainer,
            titleContentColor = colorScheme.onSurface,
            navigationIconContentColor = colorScheme.onSurface,
            actionIconContentColor = colorScheme.onSurfaceVariant,
        ),
    )
}
