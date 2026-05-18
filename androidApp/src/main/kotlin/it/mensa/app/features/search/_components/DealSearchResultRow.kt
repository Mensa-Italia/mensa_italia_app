package it.mensa.app.features.search._components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.shared.model.DealModel

/**
 * DealSearchResultRow — title + category chip + location subtitle + discount badge.
 *
 * Mirrors iOS DealSearchResultRow.swift: compact HStack without glass effect.
 */
@Composable
fun DealSearchResultRow(
    deal: DealModel,
    modifier: Modifier = Modifier,
) {
    val brandColor = MaterialTheme.colorScheme.primary
    val discountBadge = computeDiscount(deal)
    val subtitle = computeSubtitle(deal)
    val category = deal.commercialSector.trim().takeIf { it.isNotEmpty() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = deal.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
            )
            if (category != null) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = brandColor,
                    modifier = Modifier
                        .background(
                            color = brandColor.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50),
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    maxLines = 1,
                )
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }

        // Discount badge
        Box(
            modifier = Modifier
                .background(color = brandColor, shape = RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = discountBadge,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                ),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

private fun computeDiscount(deal: DealModel): String {
    val candidates = listOfNotNull(deal.details, deal.who)
    val regex = Regex("""(\d{1,3})\s?%""")
    for (text in candidates) {
        regex.find(text)?.let { match ->
            return "-${match.value.replace(" ", "")}"
        }
    }
    return "Sconto"
}

private fun computeSubtitle(deal: DealModel): String? {
    deal.position?.let { loc ->
        val city = loc.name.trim()
        val state = loc.state.trim()
        val parts = listOf(city, state).filter { it.isNotEmpty() }
        if (parts.isNotEmpty()) return parts.joinToString(", ")
    }
    return deal.details?.trim()?.takeIf { it.isNotEmpty() }
}
