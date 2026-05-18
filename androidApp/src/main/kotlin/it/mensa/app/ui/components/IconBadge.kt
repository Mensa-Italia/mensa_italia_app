package it.mensa.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.theme.MensaCyan

/**
 * IconBadge — circular icon container for Discover tiles and feature cards.
 *
 * Enforces brand color discipline: max 3 distinct colors per screen.
 *
 * Variant presets:
 *   - Primary (default): brand blue on primaryContainer
 *   - Cyan: dark backdrop on cyan (for highlighted items)
 *   - Tertiary: warm tertiary palette
 *
 * @param icon icon to display
 * @param variant color variant (controls background and icon tint)
 * @param size container diameter (default 48dp per M3 Expressive spec)
 * @param iconSize icon size within container (default 24dp)
 * @param contentDescription accessibility description
 */
@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    variant: IconBadgeVariant = IconBadgeVariant.Primary,
    size: Dp = 48.dp,
    iconSize: Dp = 24.dp,
    contentDescription: String? = null,
) {
    val colorScheme = MaterialTheme.colorScheme

    val (backgroundColor, iconTint) = when (variant) {
        IconBadgeVariant.Primary -> Pair(
            colorScheme.primaryContainer,
            colorScheme.onPrimaryContainer,
        )
        IconBadgeVariant.Cyan -> Pair(
            MensaCyan.copy(alpha = 0.18f),
            colorScheme.primary,
        )
        IconBadgeVariant.Tertiary -> Pair(
            colorScheme.tertiaryContainer,
            colorScheme.onTertiaryContainer,
        )
        is IconBadgeVariant.Custom -> Pair(
            variant.background ?: colorScheme.primaryContainer,
            variant.tint ?: colorScheme.onPrimaryContainer,
        )
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint,
        )
    }
}

/**
 * Color variant for [IconBadge].
 * Sealed to enforce discipline — only predefined variants + Custom.
 */
sealed class IconBadgeVariant {
    open val background: Color? = null
    open val tint: Color? = null

    object Primary : IconBadgeVariant()
    object Cyan : IconBadgeVariant()
    object Tertiary : IconBadgeVariant()
    data class Custom(
        override val background: Color?,
        override val tint: Color?,
    ) : IconBadgeVariant()
}
