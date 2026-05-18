package it.mensa.app.features.tickets._components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.features.tickets.TicketStatus
import it.mensa.app.features.tickets.statusComputed
import it.mensa.app.support.tr
import it.mensa.shared.model.TicketModel

/**
 * TicketRowCard — M3 canonico.
 * Card M3 con Surface/CircleShape per status icon, shape morph on press.
 */
@Composable
fun TicketRowCard(
    ticket: TicketModel,
    modifier: Modifier = Modifier,
) {
    val status = ticket.statusComputed
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "TicketCardScale",
    )

    val badgeBackground = when (status) {
        TicketStatus.Pending -> MaterialTheme.colorScheme.primaryContainer
        TicketStatus.Completed -> MaterialTheme.colorScheme.tertiaryContainer
        TicketStatus.Failed -> Color(0xFFEF4444).copy(alpha = 0.15f)
        TicketStatus.Unknown -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeTint = when (status) {
        TicketStatus.Pending -> MaterialTheme.colorScheme.onPrimaryContainer
        TicketStatus.Completed -> MaterialTheme.colorScheme.onTertiaryContainer
        TicketStatus.Failed -> Color(0xFFEF4444)
        TicketStatus.Unknown -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(modifier = modifier.fillMaxWidth().scale(scale)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Status icon badge
            Surface(shape = CircleShape, color = badgeBackground, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.ConfirmationNumber, null, tint = badgeTint, modifier = Modifier.size(22.dp))
                }
            }

            // Text info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    text = ticket.name ?: tr("tickets.no_name", fallback = "Ticket"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val desc = ticket.description
                if (!desc.isNullOrBlank()) {
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Status dot + label
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(modifier = Modifier.size(7.dp), shape = CircleShape, color = status.badgeColor) {}
                    Text(text = tr(status.labelKey, fallback = status.fallback), style = MaterialTheme.typography.labelSmall, color = status.badgeColor)
                }
            }

            Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        }
    }
}

// ─── Status helpers ───────────────────────────────────────────────────────────

private val TicketStatus.badgeColor: Color
    @Composable get() = when (this) {
        TicketStatus.Pending -> Color(0xFFF59E0B)
        TicketStatus.Completed -> Color(0xFF22C55E)
        TicketStatus.Failed -> Color(0xFFEF4444)
        TicketStatus.Unknown -> Color(0xFF9CA3AF)
    }

private val TicketStatus.labelKey: String
    get() = when (this) {
        TicketStatus.Pending -> "tickets.status.pending"
        TicketStatus.Completed -> "tickets.status.completed"
        TicketStatus.Failed -> "tickets.status.failed"
        TicketStatus.Unknown -> "tickets.status.unknown"
    }

private val TicketStatus.fallback: String
    get() = when (this) {
        TicketStatus.Pending -> "In attesa"
        TicketStatus.Completed -> "Completato"
        TicketStatus.Failed -> "Fallito"
        TicketStatus.Unknown -> "—"
    }
