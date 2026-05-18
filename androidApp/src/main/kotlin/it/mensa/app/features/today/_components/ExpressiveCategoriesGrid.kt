package it.mensa.app.features.today._components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.ui.theme.MensaCyan

/**
 * ExpressiveCategoriesGrid — M3 Expressive broken-grid layout.
 *
 * NOT a uniform 3×2 grid (that's an M3 Expressive anti-pattern — "identical card grids").
 *
 * Layout:
 *   ┌──────────────┬───────┐
 *   │              │   B   │
 *   │      A       ├───────┤
 *   │   (hero)     │   C   │
 *   ├───────┬──────┴───────┤
 *   │   D   │      E       │
 *   └───────┴──────────────┘
 *
 * Each tile uses a different tonal container and a different corner-radius asymmetry
 * to break the monotony. Press states use shape morph springs.
 */
@Composable
fun ExpressiveCategoriesGrid(
    onEventsClick: () -> Unit,
    onDealsClick: () -> Unit,
    onSigsClick: () -> Unit,
    onMembersClick: () -> Unit,
    onDocumentsClick: () -> Unit,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Top row: hero tile (left, 60%) + 2 stacked tiles (right, 40%) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            HeroCategoryTile(
                label = "Eventi",
                icon = Icons.Outlined.CalendarMonth,
                tonal = TonalVariant.PrimaryDrenched,
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 12.dp,
                    bottomEnd = 12.dp,
                    bottomStart = 28.dp,
                ),
                onClick = onEventsClick,
                modifier = Modifier
                    .weight(1.55f)
                    .fillMaxHeight(),
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SmallCategoryTile(
                    label = "Deal",
                    icon = Icons.Outlined.LocalOffer,
                    tonal = TonalVariant.TertiaryContainer,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 24.dp,
                        bottomEnd = 12.dp,
                        bottomStart = 12.dp,
                    ),
                    onClick = onDealsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
                SmallCategoryTile(
                    label = "SIG",
                    icon = Icons.Outlined.GroupWork,
                    tonal = TonalVariant.SecondaryContainer,
                    shape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomEnd = 24.dp,
                        bottomStart = 12.dp,
                    ),
                    onClick = onSigsClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                )
            }
        }

        // ── Bottom row: 3 medium tiles, asymmetric weights ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SmallCategoryTile(
                label = "Soci",
                icon = Icons.Outlined.People,
                tonal = TonalVariant.SurfaceContainerHigh,
                shape = RoundedCornerShape(
                    topStart = 28.dp,
                    topEnd = 12.dp,
                    bottomEnd = 12.dp,
                    bottomStart = 12.dp,
                ),
                onClick = onMembersClick,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            )
            SmallCategoryTile(
                label = "Doc",
                icon = Icons.Outlined.Description,
                tonal = TonalVariant.PrimaryContainer,
                shape = RoundedCornerShape(12.dp),
                onClick = onDocumentsClick,
                modifier = Modifier
                    .weight(0.9f)
                    .fillMaxHeight(),
            )
            SmallCategoryTile(
                label = "Tessera",
                icon = Icons.Outlined.CreditCard,
                tonal = TonalVariant.CyanDrenched,
                shape = RoundedCornerShape(
                    topStart = 12.dp,
                    topEnd = 28.dp,
                    bottomEnd = 28.dp,
                    bottomStart = 12.dp,
                ),
                onClick = onCardClick,
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight(),
            )
        }
    }
}

// ─── Tile variants ────────────────────────────────────────────────────────────

private enum class TonalVariant {
    PrimaryDrenched,      // brand blue, white text — the hero tile
    PrimaryContainer,
    SecondaryContainer,
    TertiaryContainer,
    SurfaceContainerHigh,
    CyanDrenched,         // MensaCyan, dark text
}

private data class TileColors(
    val container: Color,
    val onContainer: Color,
    val iconBg: Color,
    val iconTint: Color,
)

@Composable
private fun TonalVariant.toColors(scheme: ColorScheme): TileColors = when (this) {
    TonalVariant.PrimaryDrenched -> TileColors(
        container = scheme.primary,
        onContainer = scheme.onPrimary,
        iconBg = Color.White.copy(alpha = 0.16f),
        iconTint = Color.White,
    )
    TonalVariant.PrimaryContainer -> TileColors(
        container = scheme.primaryContainer,
        onContainer = scheme.onPrimaryContainer,
        iconBg = scheme.primary.copy(alpha = 0.12f),
        iconTint = scheme.primary,
    )
    TonalVariant.SecondaryContainer -> TileColors(
        container = scheme.secondaryContainer,
        onContainer = scheme.onSecondaryContainer,
        iconBg = scheme.secondary.copy(alpha = 0.12f),
        iconTint = scheme.secondary,
    )
    TonalVariant.TertiaryContainer -> TileColors(
        container = scheme.tertiaryContainer,
        onContainer = scheme.onTertiaryContainer,
        iconBg = scheme.tertiary.copy(alpha = 0.14f),
        iconTint = scheme.tertiary,
    )
    TonalVariant.SurfaceContainerHigh -> TileColors(
        container = scheme.surfaceContainerHigh,
        onContainer = scheme.onSurface,
        iconBg = scheme.primary.copy(alpha = 0.08f),
        iconTint = scheme.primary,
    )
    TonalVariant.CyanDrenched -> TileColors(
        container = MensaCyan,
        onContainer = Color(0xFF002C3F),
        iconBg = Color.White.copy(alpha = 0.32f),
        iconTint = Color(0xFF002C3F),
    )
}

@Composable
private fun HeroCategoryTile(
    label: String,
    icon: ImageVector,
    tonal: TonalVariant,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = tonal.toColors(MaterialTheme.colorScheme)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerAccent by animateDpAsState(
        targetValue = if (isPressed) 20.dp else 28.dp,
        label = "hero-tile-morph",
    )
    val morphShape = RoundedCornerShape(
        topStart = cornerAccent,
        topEnd = 12.dp,
        bottomEnd = 12.dp,
        bottomStart = cornerAccent,
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = morphShape,
        color = Color.Transparent,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(morphShape)
                .background(colors.container)
                .padding(20.dp),
        ) {
            // Icon top-right
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(50))
                    .background(colors.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.size(24.dp),
                )
            }

            // Label bottom-left
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onContainer,
                    ),
                )
            }
        }
    }
}

@Composable
private fun SmallCategoryTile(
    label: String,
    icon: ImageVector,
    tonal: TonalVariant,
    shape: RoundedCornerShape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = tonal.toColors(MaterialTheme.colorScheme)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateDpAsState(
        targetValue = if (isPressed) 32.dp else 36.dp,
        label = "tile-icon-size",
    )

    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(colors.container)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.align(Alignment.TopStart),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(scale)
                        .clip(RoundedCornerShape(50))
                        .background(colors.iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.iconTint,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = colors.onContainer,
                ),
                modifier = Modifier.align(Alignment.BottomStart),
            )
        }
    }
}
