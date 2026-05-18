package it.mensa.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MensaCard — the Mensa design system card component.
 *
 * Identity:
 *   Light: surfaceContainerHighest with subtle outlineVariant border and 2dp tonal elevation.
 *   Dark: surfaceContainerHigh with cyan-tinted subtle border.
 *
 * NOT glassmorphism — purposeful elevation and warm surface material that reads
 * "premium stationery" not "tech bubble".
 *
 * @param modifier layout modifier
 * @param shape corner shape (default 24dp — Mensa card standard)
 * @param accent optional accent color for a 4dp top-edge strip (category cards only)
 * @param onClick optional click handler (renders as clickable surface with ripple)
 * @param padding internal content padding (default 20dp)
 * @param content composable content
 */
@Composable
fun MensaCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    accent: Color? = null,
    onClick: (() -> Unit)? = null,
    padding: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = colorScheme.background.luminance() < 0.2f

    val containerColor = if (isDark) {
        colorScheme.surfaceContainerHigh
    } else {
        colorScheme.surfaceContainerHighest
    }

    val borderColor = if (isDark) {
        // Subtle cyan tint on dark for brand presence
        colorScheme.primary.copy(alpha = 0.15f)
    } else {
        colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    val border = BorderStroke(
        width = 1.dp,
        color = borderColor,
    )

    val cardContent: @Composable () -> Unit = {
        Column {
            // Optional 4dp accent strip along top edge (category cards)
            if (accent != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .then(Modifier),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = accent,
                    ) {}
                }
            }
            Box(modifier = Modifier.padding(padding)) {
                content()
            }
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = 2.dp,
            border = border,
            content = cardContent,
        )
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = containerColor,
            tonalElevation = 2.dp,
            border = border,
            content = cardContent,
        )
    }
}

// ─── Luminance helper ─────────────────────────────────────────────────────────

private fun Color.luminance(): Float {
    val r = red.toLinear()
    val g = green.toLinear()
    val b = blue.toLinear()
    return 0.2126f * r + 0.7152f * g + 0.0722f * b
}

private fun Float.toLinear(): Float {
    return if (this <= 0.04045f) {
        this / 12.92f
    } else {
        ((this + 0.055f) / 1.055f).let { v ->
            v * v * v // approximation, good enough for dark/light detection
        }
    }
}
