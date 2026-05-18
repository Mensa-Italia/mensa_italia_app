package it.mensa.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val MensaShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

// ─── Named shape tokens ───────────────────────────────────────────────────────

/** Card shape — 24dp, consistent with M3 large */
val CardShape = RoundedCornerShape(24.dp)

/** Glass card shape — same as CardShape, kept for backwards compat */
val GlassCardShape = CardShape

/** Primary button — pill (28dp) for text-only hero CTA */
val PrimaryButtonShape = RoundedCornerShape(28.dp)

/** Primary button with icon — slightly less pill */
val PrimaryButtonWithIconShape = RoundedCornerShape(16.dp)

/** Secondary / outlined button — pill */
val SecondaryButtonShape = RoundedCornerShape(28.dp)

/** Chip / filter tag shape */
val ChipShape = RoundedCornerShape(8.dp)

/** Bottom sheet — rounded top only */
val BottomSheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** Navigation bar container — rounded top */
val NavBarContainerShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

/** Icon badge — 48dp circle container, use with CircleShape */
val IconBadgeShape = RoundedCornerShape(50)

/** Metric chip — small pill */
val MetricChipShape = RoundedCornerShape(50)
