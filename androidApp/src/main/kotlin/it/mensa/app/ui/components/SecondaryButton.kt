package it.mensa.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.theme.GothamBold
import it.mensa.app.ui.theme.SecondaryButtonShape

/**
 * SecondaryButton — outlined variant for secondary actions.
 *
 * - Shape: pill (28dp radius) matching PrimaryButton
 * - Border: 1.5dp primary color
 * - Text: primary color, Gotham Bold
 * - Height: 56dp to match PrimaryButton touch target
 */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val colorScheme = MaterialTheme.colorScheme

    OutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = 56.dp),
        shape = SecondaryButtonShape,
        enabled = enabled,
        border = BorderStroke(
            width = 1.5.dp,
            color = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.12f),
        ),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.38f),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontFamily = GothamBold,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                color = if (enabled) colorScheme.primary else colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}
