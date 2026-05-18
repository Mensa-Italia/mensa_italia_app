package it.mensa.app.features.receipts._components

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.features.receipts.amountFormatted
import it.mensa.app.features.receipts.fallback
import it.mensa.app.features.receipts.iconVec
import it.mensa.app.features.receipts.kind
import it.mensa.app.features.receipts.labelKey
import it.mensa.app.features.receipts.statusColor
import it.mensa.app.support.tr
import it.mensa.shared.model.ReceiptModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ReceiptRowCard — M3 canonico.
 * Card M3 con Surface/CircleShape per tipo, amount right-aligned bold, shape morph on press.
 */
@Composable
fun ReceiptRowCard(
    receipt: ReceiptModel,
    modifier: Modifier = Modifier,
) {
    val kind = receipt.kind
    val dateString = formatShortDate(receipt.created.toEpochMilliseconds())
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "ReceiptCardScale",
    )

    val badgeBackground = when {
        receipt.status.lowercase().contains("paid") ||
            receipt.status.lowercase().contains("success") ||
            receipt.status.lowercase().contains("completed") -> MaterialTheme.colorScheme.primaryContainer
        receipt.status.lowercase().contains("pending") ||
            receipt.status.lowercase().contains("refund") -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val badgeTint = when {
        receipt.status.lowercase().contains("paid") ||
            receipt.status.lowercase().contains("success") ||
            receipt.status.lowercase().contains("completed") -> MaterialTheme.colorScheme.onPrimaryContainer
        receipt.status.lowercase().contains("pending") ||
            receipt.status.lowercase().contains("refund") -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSecondaryContainer
    }

    Card(modifier = modifier.fillMaxWidth().scale(scale)) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Icon badge per tipo ricevuta
            Surface(shape = CircleShape, color = badgeBackground, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(kind.iconVec, null, tint = badgeTint, modifier = Modifier.size(22.dp))
                }
            }

            // Text info
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(text = tr(kind.labelKey, fallback = kind.fallback), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                val desc = receipt.description
                if (!desc.isNullOrBlank()) {
                    Text(text = desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = dateString, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(modifier = Modifier.size(6.dp), shape = CircleShape, color = receipt.statusColor) {}
                    Text(text = receipt.status.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = receipt.statusColor, fontWeight = FontWeight.Medium)
                }
            }

            // Amount right-aligned bold
            Column(horizontalAlignment = Alignment.End) {
                Text(text = receipt.amountFormatted, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(imageVector = Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
            }
        }
    }
}

private fun formatShortDate(epochMs: Long): String {
    return try {
        val fmt = SimpleDateFormat("d MMM yyyy", Locale.ITALIAN)
        fmt.format(Date(epochMs))
    } catch (_: Exception) {
        "—"
    }
}
