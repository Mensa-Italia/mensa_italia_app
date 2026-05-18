package it.mensa.app.features.today._components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.model.EventModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * EventPreviewCard — M3 Expressive drenched tertiary-container hero card.
 *
 * Visual:
 * - Full-bleed image background with vertical gradient scrim
 * - Asymmetric corner shape: 16dp top, 36dp bottom-left, 16dp bottom-right
 *   (gives a "torn page" / "ticket stub" feel)
 * - Tall format (200dp) — invites scroll without being a hero focal point
 * - Kicker + headline + meta chips on the gradient
 */
@Composable
fun EventPreviewCard(
    event: EventModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Asymmetric "ticket stub" shape — torn bottom-left corner
    val shape = remember {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            bottomStart = 36.dp,
        )
    }

    val pressScale = if (isPressed) 0.98f else 1f

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(208.dp),
        shape = shape,
        color = Color.Transparent,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.tertiaryContainer),
        ) {
            val imageUrl = if (event.image.isNotBlank()) {
                FilesUrl.build(
                    collection = "events",
                    recordId = event.id,
                    filename = event.image,
                    thumb = "1000x600",
                )
            } else null

            if (imageUrl != null) {
                CachedAsyncImage(
                    model = imageUrl,
                    contentDescription = event.name,
                    modifier = Modifier.fillMaxWidth().height(208.dp),
                    contentScale = ContentScale.Crop,
                )
                // Vertical scrim — clear at top, deep at bottom for text legibility
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(208.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.10f),
                                    Color.Black.copy(alpha = 0.20f),
                                    Color.Black.copy(alpha = 0.78f),
                                ),
                            ),
                        ),
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    // Title — emphasized headline on the scrim
                    Text(
                        text = event.name,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 26.sp,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(10.dp))

                    // Meta row — date chip + venue chip
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MetaChip(
                            icon = Icons.Outlined.CalendarMonth,
                            label = event.whenStart.formatEventDate(),
                        )
                        event.position?.name?.takeIf { it.isNotBlank() }?.let { venue ->
                            MetaChip(
                                icon = Icons.Outlined.LocationOn,
                                label = venue,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    maxLines: Int = 1,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = Color.White,
                    letterSpacing = 0.3.sp,
                ),
                maxLines = maxLines,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun kotlinx.datetime.Instant.formatEventDate(): String {
    return try {
        val local = toLocalDateTime(TimeZone.currentSystemDefault())
        val months = listOf(
            "gen", "feb", "mar", "apr", "mag", "giu",
            "lug", "ago", "set", "ott", "nov", "dic",
        )
        val month = months.getOrElse(local.monthNumber - 1) { "?" }
        "${local.dayOfMonth} $month ${local.year}"
    } catch (e: Exception) {
        "—"
    }
}
