package it.mensa.app.features.discover._components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.ui.theme.EasingEmphasizedDecelerate
import it.mensa.app.ui.theme.MensaMotion

// ─── Tonal palette for the Discover broken-grid ──────────────────────────────

/**
 * Tonal variants used to drench Discover tiles.
 *
 * Discipline: never repeat the same variant on two adjacent tiles (horizontal
 * OR vertical). The drenched variants ([PrimaryDrenched], [CyanDrenched]) are
 * used sparingly — at most one per row — as visual anchors.
 */
enum class DiscoverTonal {
    PrimaryDrenched,      // MensaBlue surface, white text — top anchor
    CyanDrenched,         // MensaCyan surface, dark text — accent anchor
    PrimaryContainer,     // Soft blue container
    SecondaryContainer,   // Light navy container
    TertiaryContainer,    // Warm mauve container
    ErrorContainer,       // Peach / coral — used very sparingly for surprise
    SurfaceContainerHigh, // Warm neutral
}

internal data class DiscoverTileColors(
    val container: Color,
    val onContainer: Color,
    val iconBg: Color,
    val iconTint: Color,
    val kickerColor: Color,
)

@Composable
internal fun DiscoverTonal.toColors(scheme: ColorScheme): DiscoverTileColors = when (this) {
    DiscoverTonal.PrimaryDrenched -> DiscoverTileColors(
        container = scheme.primary,
        onContainer = scheme.onPrimary,
        iconBg = Color.White.copy(alpha = 0.18f),
        iconTint = Color.White,
        kickerColor = scheme.secondary,
    )
    DiscoverTonal.CyanDrenched -> DiscoverTileColors(
        container = scheme.secondary,
        onContainer = scheme.onSecondary,
        iconBg = Color.White.copy(alpha = 0.34f),
        iconTint = scheme.onSecondary,
        kickerColor = scheme.onSecondary.copy(alpha = 0.72f),
    )
    DiscoverTonal.PrimaryContainer -> DiscoverTileColors(
        container = scheme.primaryContainer,
        onContainer = scheme.onPrimaryContainer,
        iconBg = scheme.primary.copy(alpha = 0.14f),
        iconTint = scheme.primary,
        kickerColor = scheme.primary,
    )
    DiscoverTonal.SecondaryContainer -> DiscoverTileColors(
        container = scheme.secondaryContainer,
        onContainer = scheme.onSecondaryContainer,
        iconBg = scheme.secondary.copy(alpha = 0.14f),
        iconTint = scheme.secondary,
        kickerColor = scheme.secondary,
    )
    DiscoverTonal.TertiaryContainer -> DiscoverTileColors(
        container = scheme.tertiaryContainer,
        onContainer = scheme.onTertiaryContainer,
        iconBg = scheme.tertiary.copy(alpha = 0.16f),
        iconTint = scheme.tertiary,
        kickerColor = scheme.tertiary,
    )
    DiscoverTonal.ErrorContainer -> DiscoverTileColors(
        container = scheme.errorContainer,
        onContainer = scheme.onErrorContainer,
        iconBg = scheme.error.copy(alpha = 0.16f),
        iconTint = scheme.error,
        kickerColor = scheme.error,
    )
    DiscoverTonal.SurfaceContainerHigh -> DiscoverTileColors(
        container = scheme.surfaceContainerHigh,
        onContainer = scheme.onSurface,
        iconBg = scheme.primary.copy(alpha = 0.10f),
        iconTint = scheme.primary,
        kickerColor = scheme.onSurfaceVariant,
    )
}

// ─── Tile size profile ───────────────────────────────────────────────────────

/**
 * Size profile drives typography + icon scale + corner range for a tile.
 *
 *  - Hero    — 220dp tall, large headline, larger icon, deeper corner morph
 *  - Medium  — 130dp tall, mid headline
 *  - Small   — 110dp tall, compact label
 */
enum class DiscoverTileSize { Hero, Medium, Small }

/**
 * Asymmetric corner profile, expressed as four dp values.
 *
 * The press-state animation morphs the **accent corner** — the corner with the
 * largest base radius — toward a tighter value, while keeping the other three
 * stable. This preserves the hand-arranged asymmetry while still feeling tactile.
 */
data class TileCorners(
    val topStart: Dp,
    val topEnd: Dp,
    val bottomEnd: Dp,
    val bottomStart: Dp,
) {
    /** Returns which corner is the visual "accent" (largest), to be morphed on press. */
    internal val accentCorner: Corner
        get() = listOf(
            Corner.TopStart to topStart.value,
            Corner.TopEnd to topEnd.value,
            Corner.BottomEnd to bottomEnd.value,
            Corner.BottomStart to bottomStart.value,
        ).maxBy { it.second }.first

    internal val accentBaseDp: Dp
        get() = when (accentCorner) {
            Corner.TopStart -> topStart
            Corner.TopEnd -> topEnd
            Corner.BottomEnd -> bottomEnd
            Corner.BottomStart -> bottomStart
        }

    internal enum class Corner { TopStart, TopEnd, BottomEnd, BottomStart }
}

// ─── Public composable ───────────────────────────────────────────────────────

/**
 * DiscoverExpressiveTile — drenched/tonal tile used in the broken-grid menu.
 *
 * The full surface is the click target. Press-state morphs the accent corner
 * + scales the tile, while the asymmetric silhouette remains. Entrance fades
 * + slides up in staggered cadence.
 *
 * @param kicker uppercase 2-3 word category bucket (e.g. "COMUNITÀ"). Null → no kicker.
 * @param label primary tile label
 * @param icon trailing top-right icon
 * @param tonal color drench variant (see [DiscoverTonal])
 * @param corners asymmetric corner profile — vary per tile to break the grid
 * @param size profile that drives typography + padding (see [DiscoverTileSize])
 * @param onClick tap handler
 * @param entranceIndex stagger offset for the fade+slide entrance animation
 */
@Composable
fun DiscoverExpressiveTile(
    kicker: String?,
    label: String,
    icon: ImageVector,
    tonal: DiscoverTonal,
    corners: TileCorners,
    size: DiscoverTileSize,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    entranceIndex: Int = 0,
) {
    val colors = tonal.toColors(MaterialTheme.colorScheme)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // ── Entrance animation: fade + slide-up, staggered by index ──
    val entranceAlpha = remember { Animatable(0f) }
    val entranceSlide = remember { Animatable(18f) }

    LaunchedEffect(Unit) {
        val delayMs = (entranceIndex * 55).toLong().coerceAtMost(640L)
        kotlinx.coroutines.delay(delayMs)
        entranceAlpha.animateTo(1f, animationSpec = MensaMotion.tweenEnter)
    }
    LaunchedEffect(Unit) {
        val delayMs = (entranceIndex * 55).toLong().coerceAtMost(640L)
        kotlinx.coroutines.delay(delayMs)
        entranceSlide.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 380, easing = EasingEmphasizedDecelerate),
        )
    }

    // ── Press shape morph: tighten the accent corner ──
    val morphDelta = when (size) {
        DiscoverTileSize.Hero -> 10.dp
        DiscoverTileSize.Medium -> 8.dp
        DiscoverTileSize.Small -> 6.dp
    }
    val accentBase = corners.accentBaseDp
    val accentMorphed by animateDpAsState(
        targetValue = if (isPressed) {
            (accentBase - morphDelta).coerceAtLeast(8.dp)
        } else {
            accentBase
        },
        animationSpec = tween(durationMillis = 220, easing = EasingEmphasizedDecelerate),
        label = "discover-tile-accent-corner",
    )

    // ── Press scale: slight depression ──
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 180, easing = EasingEmphasizedDecelerate),
        label = "discover-tile-scale",
    )

    val pressedCorners = remember(corners, accentMorphed) {
        when (corners.accentCorner) {
            TileCorners.Corner.TopStart -> corners.copy(topStart = accentMorphed)
            TileCorners.Corner.TopEnd -> corners.copy(topEnd = accentMorphed)
            TileCorners.Corner.BottomEnd -> corners.copy(bottomEnd = accentMorphed)
            TileCorners.Corner.BottomStart -> corners.copy(bottomStart = accentMorphed)
        }
    }

    val shape = RoundedCornerShape(
        topStart = pressedCorners.topStart,
        topEnd = pressedCorners.topEnd,
        bottomEnd = pressedCorners.bottomEnd,
        bottomStart = pressedCorners.bottomStart,
    )

    val (iconBox, iconGlyph) = when (size) {
        DiscoverTileSize.Hero -> 52.dp to 26.dp
        DiscoverTileSize.Medium -> 44.dp to 22.dp
        DiscoverTileSize.Small -> 40.dp to 20.dp
    }

    val pad = when (size) {
        DiscoverTileSize.Hero -> 20.dp
        DiscoverTileSize.Medium -> 16.dp
        DiscoverTileSize.Small -> 14.dp
    }

    Surface(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                alpha = entranceAlpha.value
                translationY = entranceSlide.value * density
                scaleX = pressScale
                scaleY = pressScale
            },
        shape = shape,
        color = Color.Transparent,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape)
                .background(colors.container)
                .padding(pad),
        ) {
            // ── Top-right icon badge ──
            Box(
                modifier = Modifier
                    .size(iconBox)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(50))
                    .background(colors.iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.iconTint,
                    modifier = Modifier.size(iconGlyph),
                )
            }

            // ── Bottom-left text stack ──
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.88f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (kicker != null) {
                    Text(
                        text = kicker,
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.kickerColor),
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = label,
                    style = when (size) {
                        DiscoverTileSize.Hero -> MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onContainer,
                        )
                        DiscoverTileSize.Medium -> MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onContainer,
                        )
                        DiscoverTileSize.Small -> MaterialTheme.typography.titleMedium.copy(
                            color = colors.onContainer,
                        )
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
