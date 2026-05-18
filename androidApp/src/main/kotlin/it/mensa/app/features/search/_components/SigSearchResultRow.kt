package it.mensa.app.features.search._components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.model.SigModel

/**
 * SigSearchResultRow — logo + name + group type chip.
 *
 * Mirrors iOS SigSearchResultRow.swift: compact row with icon artwork.
 */
@Composable
fun SigSearchResultRow(
    sig: SigModel,
    modifier: Modifier = Modifier,
) {
    val brandColor = MaterialTheme.colorScheme.primary
    val imageUrl = sigImageUrl(sig)
    val typeIcon = sigTypeIcon(sig.groupType)
    val typeLabel = sigTypeLabel(sig.groupType)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Artwork 44×44
        if (imageUrl != null) {
            CachedAsyncImage(
                model = imageUrl,
                contentDescription = sig.name,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = brandColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(10.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = sig.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
            )
            if (typeLabel != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = brandColor,
                        modifier = Modifier.size(11.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun sigImageUrl(sig: SigModel): String? {
    if (sig.image.isEmpty()) return null
    if (sig.image.startsWith("http")) return sig.image
    return FilesUrl.build("sigs", sig.id, sig.image, "200x200")
}

private fun sigTypeIcon(groupType: String): ImageVector {
    val lower = groupType.lowercase()
    return when {
        lower.contains("telegram") || lower.contains("chat") -> Icons.Outlined.Chat
        lower.contains("local") -> Icons.Outlined.Map
        else -> Icons.Outlined.Groups
    }
}

private fun sigTypeLabel(groupType: String): String? {
    val lower = groupType.lowercase()
    return when {
        lower.contains("chat") || lower.contains("telegram") -> "Gruppi Telegram"
        lower.contains("local") -> "Gruppi ufficiali"
        lower.contains("sig") -> "SIG"
        groupType.isNotEmpty() -> groupType.replace("_", " ").replaceFirstChar { it.uppercase() }
        else -> null
    }
}
