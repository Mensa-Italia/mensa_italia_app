package it.mensa.app.features.card._components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.mensa.app.features.card.CardStatus
import it.mensa.app.support.tr

/**
 * CardStatusBadge — coloured pill showing membership status.
 *
 * - Active      → primary color (Mensa blue)
 * - ExpiringSoon → tertiary (warm amber)
 * - Expired     → error (red)
 */
@Composable
fun CardStatusBadge(
    status: CardStatus,
    modifier: Modifier = Modifier,
) {
    val (bgColor, dotColor, label) = when (status) {
        CardStatus.Active -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
            tr("card.active", fallback = "Attiva"),
        )
        CardStatus.ExpiringSoon -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.tertiary,
            tr("card.expiring_soon", fallback = "In scadenza"),
        )
        CardStatus.Expired -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.error,
            tr("card.expired", fallback = "Scaduta"),
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Spacer(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = dotColor,
        )
    }
}
