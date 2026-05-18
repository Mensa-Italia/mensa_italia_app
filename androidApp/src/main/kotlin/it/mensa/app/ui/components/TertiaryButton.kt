package it.mensa.app.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.theme.GothamBold

/**
 * TertiaryButton — text-only compact button for low-emphasis actions.
 *
 * - No border, no background (tap state provides subtle highlight via ripple)
 * - Typography: labelLarge Gotham Bold, primary color
 * - Use for "Vedi tutti", inline links, auxiliary actions
 */
@Composable
fun TertiaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme

    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.38f),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                fontFamily = GothamBold,
                style = MaterialTheme.typography.labelLarge,
                color = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}
