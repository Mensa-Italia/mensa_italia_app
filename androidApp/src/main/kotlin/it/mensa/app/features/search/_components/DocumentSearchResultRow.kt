package it.mensa.app.features.search._components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.shared.model.DocumentModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * DocumentSearchResultRow — icon + title + extension + date.
 *
 * iOS equivalent: DocumentRow (reused in search context).
 */
@Composable
fun DocumentSearchResultRow(
    document: DocumentModel,
    modifier: Modifier = Modifier,
) {
    val brandColor = MaterialTheme.colorScheme.primary
    val extension = document.file.substringAfterLast('.', "").uppercase().takeIf { it.isNotEmpty() }
    val icon = iconForExtension(document.file)
    val dateStr = formatDocDate(document)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon badge 36×36
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = brandColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = brandColor,
                modifier = Modifier.size(18.dp),
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (extension != null) {
                    Text(
                        text = extension,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = brandColor,
                        modifier = Modifier
                            .background(
                                color = brandColor.copy(alpha = 0.10f),
                                shape = RoundedCornerShape(4.dp),
                            )
                            .padding(horizontal = 5.dp, vertical = 1.dp),
                    )
                }
                if (dateStr.isNotEmpty()) {
                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun iconForExtension(filename: String): ImageVector {
    return when (filename.substringAfterLast('.').lowercase()) {
        "pdf" -> Icons.Outlined.PictureAsPdf
        "xls", "xlsx", "csv" -> Icons.Outlined.TableChart
        "jpg", "jpeg", "png", "gif", "webp" -> Icons.Outlined.Image
        else -> Icons.Outlined.Description
    }
}

private fun formatDocDate(document: DocumentModel): String {
    return try {
        val local = document.created.toLocalDateTime(TimeZone.currentSystemDefault())
        "%02d/%02d/%d".format(local.dayOfMonth, local.monthNumber, local.year)
    } catch (e: Exception) { "" }
}
