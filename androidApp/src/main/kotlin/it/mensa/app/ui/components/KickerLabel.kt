package it.mensa.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.theme.ExpressiveTypography

/**
 * KickerLabel — small uppercase tracking label for section identifiers.
 *
 * Used above headlines to provide context category (e.g. "QUESTO MESE", "NOVITA").
 * Enforces consistent spacing and typography across the design system.
 *
 * @param text label text (will be uppercased automatically)
 * @param modifier layout modifier
 * @param color text color (default onSurfaceVariant for appropriate hierarchy)
 * @param leadingIcon optional icon shown before the text
 */
@Composable
fun KickerLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    leadingIcon: ImageVector? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = color,
            )
            Spacer(Modifier.width(4.dp))
        }
        Text(
            text = text.uppercase(),
            style = ExpressiveTypography.kickerLabel,
            color = color,
        )
    }
}
