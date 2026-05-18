package it.mensa.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.theme.MetricChipShape

/**
 * MetricChip — compact pill showing a label + metric value.
 *
 * Usage: event count ("12 EVENTI"), member count ("4K SOCI"), upcoming score.
 * Background: secondaryContainer for subtle but present branding.
 *
 * @param label descriptive label (e.g. "EVENTI", "SOCI")
 * @param value the metric value (e.g. "12", "4K")
 */
@Composable
fun MetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .clip(MetricChipShape)
            .background(colorScheme.secondaryContainer)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSecondaryContainer,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = colorScheme.onSecondaryContainer.copy(alpha = 0.75f),
        )
    }
}
