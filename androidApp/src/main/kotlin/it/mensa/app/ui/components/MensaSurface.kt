package it.mensa.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Surface hierarchy levels following M3 Expressive container hierarchy.
 * Maps directly to M3 surfaceContainerLowest..Highest + tonal elevation steps.
 */
enum class SurfaceLevel {
    Lowest,   // surfaceContainerLowest — 0 dp tonal elevation
    Low,      // surfaceContainerLow    — 1 dp
    Medium,   // surfaceContainer       — 2 dp  (default)
    High,     // surfaceContainerHigh   — 3 dp
    Highest,  // surfaceContainerHighest — 4 dp
}

/**
 * MensaSurface — container hierarchy wrapper for M3 Expressive design system.
 *
 * Provides purposeful elevation, consistent shape, and optional bordered styling.
 * Replaces ad-hoc Surface usage scattered through the codebase.
 *
 * @param level M3 surface container level (controls background color + tonal elevation)
 * @param shape container shape (default 24dp rounded — M3 large)
 * @param bordered adds a 1dp outlineVariant border for subtle separation
 * @param onClick if non-null, renders as a clickable surface with ripple
 * @param content composable content
 */
@Composable
fun MensaSurface(
    modifier: Modifier = Modifier,
    level: SurfaceLevel = SurfaceLevel.Medium,
    shape: Shape = RoundedCornerShape(24.dp),
    bordered: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    val containerColor = when (level) {
        SurfaceLevel.Lowest -> colorScheme.surfaceContainerLowest
        SurfaceLevel.Low -> colorScheme.surfaceContainerLow
        SurfaceLevel.Medium -> colorScheme.surfaceContainer
        SurfaceLevel.High -> colorScheme.surfaceContainerHigh
        SurfaceLevel.Highest -> colorScheme.surfaceContainerHighest
    }

    val tonalElevation = when (level) {
        SurfaceLevel.Lowest -> 0.dp
        SurfaceLevel.Low -> 1.dp
        SurfaceLevel.Medium -> 2.dp
        SurfaceLevel.High -> 3.dp
        SurfaceLevel.Highest -> 4.dp
    }

    val border = if (bordered) {
        BorderStroke(
            width = 1.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.5f),
        )
    } else {
        null
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
            border = border,
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = tonalElevation,
            border = border,
        ) {
            content()
        }
    }
}

/**
 * MensaSurface with automatic standard content padding (16dp).
 * Convenience wrapper for common usage pattern.
 */
@Composable
fun MensaSurfacePadded(
    modifier: Modifier = Modifier,
    level: SurfaceLevel = SurfaceLevel.Medium,
    shape: Shape = RoundedCornerShape(24.dp),
    bordered: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    MensaSurface(
        modifier = modifier,
        level = level,
        shape = shape,
        bordered = bordered,
        onClick = onClick,
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}
