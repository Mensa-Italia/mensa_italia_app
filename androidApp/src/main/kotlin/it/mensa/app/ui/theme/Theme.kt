package it.mensa.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Color schemes ────────────────────────────────────────────────────────────

private val MensaLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary,
    scrim = LightScrim,
)

private val MensaDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary,
    scrim = DarkScrim,
)

// ─── Gradient tokens ──────────────────────────────────────────────────────────

/**
 * Typed gradient bag provided via CompositionLocal.
 *
 * New names (Wave 1+):
 *   - brandHero: saturated blue hero gradient (always dark/blue — for splash, login, audio player)
 *   - brandDiagonal: diagonal deep-blue gradient for full-screen backdrops
 *   - warmSurface: subtle warm surface for content screens (light) or deep blue (dark)
 *   - cyanHighlight: MensaBlue -> MensaCyan accent for hero cards
 *
 * Backwards-compat properties (Wave 2 should migrate to new names):
 *   - brand: alias for brandHero
 *   - brandSurface: alias for warmSurface
 */
data class MensaGradients(
    // New canonical names
    val brandHero: Brush,
    val brandDiagonal: Brush,
    val warmSurface: Brush,
    val cyanHighlight: Brush,
) {
    // Backwards-compat shims — keep existing callsites compiling
    @Deprecated(
        message = "Use brandHero instead. 'brand' now always resolves to the saturated hero gradient.",
        replaceWith = ReplaceWith("brandHero"),
    )
    val brand: Brush get() = brandHero

    @Deprecated(
        message = "Use warmSurface instead.",
        replaceWith = ReplaceWith("warmSurface"),
    )
    val brandSurface: Brush get() = warmSurface
}

val LocalMensaGradients = staticCompositionLocalOf {
    MensaGradients(
        brandHero = brandHeroLight,
        brandDiagonal = brandHeroDiagonal,
        warmSurface = warmSurfaceLight,
        cyanHighlight = cyanHighlight,
    )
}

val LocalExpressiveTypography = staticCompositionLocalOf { ExpressiveTypography }

// ─── Accessors ────────────────────────────────────────────────────────────────

val mensaGradients: MensaGradients
    @Composable @ReadOnlyComposable
    get() = LocalMensaGradients.current

// ─── System bar helper ────────────────────────────────────────────────────────

/**
 * Configures system bar icon appearance for the current window.
 *
 * Call inside any Composable that wants light icons (e.g. over a hero dark bg)
 * or dark icons (over a light content surface).
 *
 * @param darkIcons true = dark icons on light background (default light-mode content),
 *                  false = light icons on dark background (hero / brand areas)
 */
@Composable
fun MensaSystemBars(darkIcons: Boolean) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            @Suppress("DEPRECATION")
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            @Suppress("DEPRECATION")
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).let { ctrl ->
                ctrl.isAppearanceLightStatusBars = darkIcons
                ctrl.isAppearanceLightNavigationBars = darkIcons
            }
        }
    }
}

// ─── MensaTheme ───────────────────────────────────────────────────────────────

@Composable
fun MensaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color disabled — brand identity takes priority
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        // Dynamic color only on Android 12+ AND when explicitly requested
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) {
                androidx.compose.material3.dynamicDarkColorScheme(context)
            } else {
                androidx.compose.material3.dynamicLightColorScheme(context)
            }
        }
        darkTheme -> MensaDarkColorScheme
        else -> MensaLightColorScheme
    }

    val gradients = if (darkTheme) {
        MensaGradients(
            brandHero = brandHeroDark,
            brandDiagonal = brandHeroDiagonal,
            warmSurface = warmSurfaceDark,
            cyanHighlight = cyanHighlight,
        )
    } else {
        MensaGradients(
            brandHero = brandHeroLight,
            brandDiagonal = brandHeroDiagonal,
            warmSurface = warmSurfaceLight,
            cyanHighlight = cyanHighlight,
        )
    }

    CompositionLocalProvider(
        LocalMensaGradients provides gradients,
        LocalExpressiveTypography provides ExpressiveTypography,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MensaTypography,
            shapes = MensaShapes,
            content = content,
        )
    }
}
