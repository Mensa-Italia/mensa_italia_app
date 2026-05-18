package it.mensa.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import it.mensa.app.R

// ─── Font families ────────────────────────────────────────────────────────────
val GothamBold = FontFamily(
    Font(R.font.gotham_bold, FontWeight.Bold),
    Font(R.font.gotham_bold, FontWeight.Black),
    Font(R.font.gotham_bold, FontWeight.ExtraBold),
)

// ─── M3 Typography — all 15 canonical roles ───────────────────────────────────
val MensaTypography = Typography(
    // Display — impactful, tight letterSpacing per M3 Expressive spec
    displayLarge = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.5).sp,
    ),

    // Headline — Gotham ExtraBold for strong section anchors
    headlineLarge = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.25).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.15).sp,
    ),

    // Title
    titleLarge = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp,
    ),

    // Body — Roboto / system SansSerif for readability
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp,
    ),

    // Label — Gotham Bold for buttons + chips, uppercase convention enforced per use-site
    labelLarge = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp,
    ),
)

// ─── M3 Expressive — hero-moment emphasized variants ─────────────────────────
/**
 * Expressive typography additions per M3 Expressive spec.
 * Use for hero moments, featured cards, pull quotes — ONE per screen.
 *
 * The *Emphasized variants push weight to Black and tighten letterSpacing
 * beyond the M3 defaults to give the brand its academic gravity.
 */
object ExpressiveTypography {
    val displayLargeEmphasized = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 64.sp,
        lineHeight = 70.sp,
        letterSpacing = (-0.5).sp,
    )
    val displayMediumEmphasized = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = (-0.5).sp,
    )
    val headlineLargeEmphasized = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.5).sp,
    )
    val headlineMediumEmphasized = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp,
    )
    val titleLargeEmphasized = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp,
    )
    val labelLargeEmphasized = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Black,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp,
    )
    /** Kicker label: small caps, wide tracking, for section labels above headlines */
    val kickerLabel = TextStyle(
        fontFamily = GothamBold,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.sp,
    )
}
