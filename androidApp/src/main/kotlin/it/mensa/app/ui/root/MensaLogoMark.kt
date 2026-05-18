package it.mensa.app.ui.root

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.R
import it.mensa.app.ui.theme.MensaBlue

/**
 * Logo display variant for [MensaLogoMark].
 */
enum class LogoVariant {
    /** Filled circle background with white logo (default) */
    Solid,

    /** Outline circle border with logo tinted by LocalContentColor */
    Outline,

    /** Logo only, no container — tinted by LocalContentColor */
    Mono,
}

/**
 * MensaLogoMark — the Mensa globe+M mark with three display variants.
 *
 * - [LogoVariant.Solid]: MensaBlue circle badge with white logo (hero use)
 * - [LogoVariant.Outline]: transparent circle with 1.5dp border, logo tinted
 * - [LogoVariant.Mono]: bare logo image tinted by LocalContentColor
 *
 * Color is inferred from [LocalContentColor] for non-Solid variants.
 *
 * @param size diameter in dp (default 80dp)
 * @param variant display variant (default Solid)
 */
@Composable
fun MensaLogoMark(
    size: Dp = 80.dp,
    variant: LogoVariant = LogoVariant.Solid,
) {
    val contentColor = LocalContentColor.current.takeIf { it != Color.Unspecified }
        ?: MaterialTheme.colorScheme.onSurface

    when (variant) {
        LogoVariant.Solid -> {
            // Filled MensaBlue badge → logo tintato di bianco per contrasto AAA.
            // L'asset PNG `mensa_logo` è una glyph monocromatica nera: senza
            // ColorFilter resterebbe nera sul fondo blu (contrasto pessimo).
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MensaBlue),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.mensa_logo),
                    contentDescription = "Mensa Italia",
                    colorFilter = ColorFilter.tint(Color.White),
                    modifier = Modifier.size(size * 0.72f),
                )
            }
        }

        LogoVariant.Outline -> {
            // Outline transparent, logo tintato col contentColor (di solito
            // onSurface o onPrimary) — segue il tema light/dark.
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(
                        width = 1.5.dp,
                        color = contentColor,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.mensa_logo),
                    contentDescription = "Mensa Italia",
                    colorFilter = ColorFilter.tint(contentColor),
                    modifier = Modifier.size(size * 0.60f),
                )
            }
        }

        LogoVariant.Mono -> {
            // Nessun container: logo tintato col contentColor — sarà nero su
            // sfondi chiari, bianco su sfondi scuri (segue il tema).
            Image(
                painter = painterResource(R.drawable.mensa_logo),
                contentDescription = "Mensa Italia",
                colorFilter = ColorFilter.tint(contentColor),
                modifier = Modifier.size(size),
            )
        }
    }
}

/**
 * Legacy overload — accepts [size] as Int and [inBlueBadge] boolean.
 * Kept for backwards compatibility with existing callsites.
 *
 * @deprecated Use MensaLogoMark(size = N.dp, variant = LogoVariant.Solid/Mono) instead.
 */
@Deprecated(
    message = "Use MensaLogoMark(size: Dp, variant: LogoVariant) instead.",
    replaceWith = ReplaceWith(
        "MensaLogoMark(size = size.dp, variant = if (inBlueBadge) LogoVariant.Solid else LogoVariant.Mono)",
        imports = ["it.mensa.app.ui.root.LogoVariant"],
    ),
)
@Composable
fun MensaLogoMark(
    size: Int = 80,
    inBlueBadge: Boolean = true,
) {
    MensaLogoMark(
        size = size.dp,
        variant = if (inBlueBadge) LogoVariant.Solid else LogoVariant.Mono,
    )
}
