package it.mensa.app.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ─── Brand primitives ────────────────────────────────────────────────────────
val MensaBlue = Color(0xFF184295)
val MensaCyan = Color(0xFF6AC9F0)
val MensaInk = Color(0xFF575656)
val BackdropDark = Color(0xFF061F2E)
val Parchment = Color(0xFFFCFBF7)

// ─── Light color scheme — M3 Expressive Committed strategy ───────────────────
// Background: warm Parchment — NOT pastel blue
val LightPrimary = Color(0xFF184295)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD8E2FF)
val LightOnPrimaryContainer = Color(0xFF001A41)

// Secondary: rich navy-derived tonal (deeper than the old cyan-tinted blue)
val LightSecondary = Color(0xFF3F6090)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD6E3FF)
val LightOnSecondaryContainer = Color(0xFF00174A)

// Tertiary: warm mauve — complements navy without competing
val LightTertiary = Color(0xFF735471)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFD6FA)
val LightOnTertiaryContainer = Color(0xFF2C0D2B)

val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnErrorContainer = Color(0xFF410002)

// Background and surface use Parchment as warm neutral base
val LightBackground = Parchment
val LightOnBackground = Color(0xFF1A1C22)

val LightSurface = Parchment
val LightOnSurface = Color(0xFF1A1C22)
val LightSurfaceVariant = Color(0xFFE1E2EC)
val LightOnSurfaceVariant = Color(0xFF44474F)

val LightOutline = Color(0xFF74777F)
val LightOutlineVariant = Color(0xFFC4C6D0)

// Surface container hierarchy — warm-neutral derived, no blue tint
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF8F7F2)
val LightSurfaceContainer = Color(0xFFF2F1EC)
val LightSurfaceContainerHigh = Color(0xFFECEBE6)
val LightSurfaceContainerHighest = Color(0xFFE6E5E0)

val LightInverseSurface = Color(0xFF2F3038)
val LightInverseOnSurface = Color(0xFFF1F0F8)
val LightInversePrimary = MensaCyan

val LightScrim = Color(0xFF000000)
val LightShadow = Color(0xFF000000)

// ─── Dark color scheme — BackdropDark base, cyan as primary ──────────────────
val DarkPrimary = MensaCyan
val DarkOnPrimary = Color(0xFF003547)
val DarkPrimaryContainer = MensaBlue
val DarkOnPrimaryContainer = LightPrimaryContainer

val DarkSecondary = Color(0xFFA9C8F5)
val DarkOnSecondary = Color(0xFF0A305E)
val DarkSecondaryContainer = Color(0xFF264777)
val DarkOnSecondaryContainer = LightSecondaryContainer

val DarkTertiary = Color(0xFFE8B4E5)
val DarkOnTertiary = Color(0xFF432542)
val DarkTertiaryContainer = Color(0xFF5B3C59)
val DarkOnTertiaryContainer = LightTertiaryContainer

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnErrorContainer = LightErrorContainer

// Dark background = BackdropDark for full brand depth
val DarkBackground = BackdropDark
val DarkOnBackground = Color(0xFFE3E2EA)

val DarkSurface = BackdropDark
val DarkOnSurface = Color(0xFFE3E2EA)
val DarkSurfaceVariant = Color(0xFF44474F)
val DarkOnSurfaceVariant = LightOutlineVariant

val DarkOutline = Color(0xFF8E9099)
val DarkOutlineVariant = Color(0xFF44474F)

// Dark surface containers: deep ocean-blue tones from BackdropDark
val DarkSurfaceContainerLowest = Color(0xFF030F17)
val DarkSurfaceContainerLow = Color(0xFF0E2133)
val DarkSurfaceContainer = Color(0xFF122538)
val DarkSurfaceContainerHigh = Color(0xFF1C2F42)
val DarkSurfaceContainerHighest = Color(0xFF263A4D)

val DarkInverseSurface = Color(0xFFE3E2EA)
val DarkInverseOnSurface = Color(0xFF2F3038)
val DarkInversePrimary = MensaBlue

val DarkScrim = Color(0xFF000000)
val DarkShadow = Color(0xFF000000)

// ─── Brand gradient brushes — NO pastel, always saturated ────────────────────

/**
 * Hero gradient: brand blue (top) → deep dark (bottom).
 * Use for splash, login backdrop, audio player, membership card.
 */
val brandHeroLight = Brush.verticalGradient(
    colors = listOf(MensaBlue, Color(0xFF0D2E6B)),
)

/**
 * Hero gradient dark variant: deep backdrop → MensaBlue.
 * Use for dark-mode hero areas.
 */
val brandHeroDark = Brush.verticalGradient(
    colors = listOf(BackdropDark, MensaBlue.copy(alpha = 0.85f)),
)

/**
 * Warm surface light: Parchment → warm off-white.
 * Subtle warmth for content surfaces, NOT a brand color.
 */
val warmSurfaceLight = Brush.verticalGradient(
    colors = listOf(Parchment, Color(0xFFF5EFE3)),
)

/**
 * Warm surface dark: deep blue-grey tones.
 */
val warmSurfaceDark = Brush.verticalGradient(
    colors = listOf(Color(0xFF0D2638), BackdropDark),
)

/**
 * Cyan highlight: MensaBlue → MensaCyan fade.
 * Use for hero cards, membership card accent, featured content.
 */
val cyanHighlight = Brush.linearGradient(
    colors = listOf(MensaBlue, MensaCyan.copy(alpha = 0.6f)),
    start = Offset(0f, 0f),
    end = Offset.Infinite,
)

/**
 * Diagonal brand hero: blue → deep → dark.
 * Use for landing/login full-screen backdrops.
 */
val brandHeroDiagonal = Brush.linearGradient(
    colors = listOf(MensaBlue, Color(0xFF0D2E6B), BackdropDark),
)

// ─── Backwards-compat aliases ─────────────────────────────────────────────────
// These allow Wave-1 callsites in features to keep compiling.
// Wave 2 will migrate them to the new names.

/** @deprecated Use brandHeroLight or brandHeroDark from MensaGradients */
@Deprecated(
    message = "Use mensaGradients.brandHero (or brandHeroLight/brandHeroDark). " +
        "This top-level val defaults to the SATURATED dark hero gradient.",
    replaceWith = ReplaceWith("brandHeroLight"),
)
val brandGradient = brandHeroLight

/** @deprecated Use brandHeroDiagonal from MensaGradients */
@Deprecated(
    message = "Use mensaGradients.brandDiagonal (brandHeroDiagonal).",
    replaceWith = ReplaceWith("brandHeroDiagonal"),
)
val brandGradientDiagonal = brandHeroDiagonal

/** @deprecated Use warmSurfaceLight or warmSurfaceDark from MensaGradients */
@Deprecated(
    message = "Use mensaGradients.warmSurface (warmSurfaceLight/warmSurfaceDark).",
    replaceWith = ReplaceWith("warmSurfaceLight"),
)
val brandGradientLight = warmSurfaceLight
