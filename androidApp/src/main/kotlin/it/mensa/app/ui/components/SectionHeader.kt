package it.mensa.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.theme.ExpressiveTypography

/**
 * SectionHeader — M3 Expressive section anchor with optional kicker + trailing action.
 *
 * Spacing:
 *   - 32dp top padding (breathing room above new sections)
 *   - 16dp bottom padding (tight coupling to section content below)
 *
 * @param title section title in headlineMedium emphasized Gotham
 * @param kicker optional all-caps label shown above title (e.g. "QUESTO MESE", "NEWS")
 * @param trailingAction optional trailing composable (e.g. TertiaryButton "Vedi tutti")
 * @param modifier layout modifier applied to the outer Column
 */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    kicker: String? = null,
    trailingAction: @Composable (() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (kicker != null) {
                Text(
                    text = kicker.uppercase(),
                    style = ExpressiveTypography.kickerLabel,
                    color = colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = title,
                style = ExpressiveTypography.headlineMediumEmphasized,
                color = colorScheme.onBackground,
            )
        }
        if (trailingAction != null) {
            trailingAction()
        }
    }
}
