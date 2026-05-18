package it.mensa.app.features.deals._components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalOffer
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
import it.mensa.shared.model.DealModel

/**
 * DealCardView — M3 canonico.
 * Card M3 con Surface/CircleShape per categoria, discount badge, shape morph on press.
 */
@Composable
fun DealCardView(
    deal: DealModel,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val discountBadge = computeDiscountBadge(deal)
    val subtitle = computeSubtitle(deal)
    val category = deal.commercialSector.trim().takeIf { it.isNotEmpty() }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
        label = "DealCardScale",
    )

    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.fillMaxWidth().scale(scale),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Leading: categoria icon badge
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp).align(Alignment.Top),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.LocalOffer, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                }
            }

            // Centro: nome + categoria + subtitle
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = deal.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (category != null) {
                    // KickerLabel → labelSmall con colore primary
                    Text(
                        text = category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            // Trailing: Discount badge
            Box(
                modifier = Modifier
                    .background(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = discountBadge,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

internal fun computeDiscountBadge(deal: DealModel): String {
    val candidates = listOfNotNull(deal.details, deal.who)
    val regex = Regex("""(\d{1,3})\s?%""")
    for (text in candidates) {
        regex.find(text)?.let { match -> return "-${match.value.replace(" ", "")}" }
    }
    return "Sconto"
}

internal fun computeSubtitle(deal: DealModel): String? {
    deal.position?.let { loc ->
        val city = loc.name.trim()
        val state = loc.state.trim()
        val parts = listOf(city, state).filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) return parts.joinToString(", ")
    }
    return deal.details?.trim()?.takeIf { it.isNotEmpty() }
}
