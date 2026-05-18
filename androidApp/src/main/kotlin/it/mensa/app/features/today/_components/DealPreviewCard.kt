package it.mensa.app.features.today._components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.model.DealModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * DealPreviewCard — horizontal scroll card for deals.
 *
 * Shows deal image/color, name, and expiry date.
 * Fixed width (160dp) for horizontal scroll usage.
 */
@Composable
fun DealPreviewCard(
    deal: DealModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
        tonalElevation = 4.dp,
    ) {
        Column {
            // Deal image (or placeholder)
            val attachment = deal.attachment
            val imageUrl = if (!attachment.isNullOrBlank()) {
                FilesUrl.build(
                    collection = "deals",
                    recordId = deal.id,
                    filename = attachment,
                    thumb = "320x200",
                )
            } else null

            CachedAsyncImage(
                model = imageUrl,
                contentDescription = deal.name,
                modifier = Modifier
                    .size(width = 160.dp, height = 100.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop,
            )

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = deal.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )

                deal.ending?.let { ending ->
                    Spacer(Modifier.height(4.dp))
                    val expiryStr = ending.formatDealDate()
                    Text(
                        text = "Scade: $expiryStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun kotlinx.datetime.Instant.formatDealDate(): String {
    return try {
        val local = toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.dayOfMonth}/${local.monthNumber}/${local.year}"
    } catch (e: Exception) {
        "—"
    }
}
