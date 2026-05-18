package it.mensa.app.features.today._components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.shared.model.NotificationModel
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * NotificationsPreview — top-3 notifications with resolved i18n titles.
 *
 * BUG FIX: NotificationModel.tr holds the raw key (e.g. push_notification.new_document_available).
 * This composable resolves it via the i18n catalog with a human-readable fallback.
 *
 * Entrance animation: staggered slide-up + fade per row (80ms delay per index).
 */
@Composable
fun NotificationsPreview(
    notifications: List<NotificationModel>,
    onNotificationClick: (NotificationModel) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preview = remember(notifications) { notifications.take(3) }
    if (preview.isEmpty()) return

    val i18n = remember { koinAccess().i18n }

    // Staggered entrance visibility per row
    val visible = remember { mutableStateListOf(false, false, false) }
    LaunchedEffect(Unit) {
        preview.indices.forEach { i ->
            delay(80L * i)
            if (i < visible.size) visible[i] = true
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel(
            title = tr("app.today.recent_notifications", fallback = "Notifiche recenti"),
            icon = Icons.Outlined.Notifications,
        )

        Card {
            Column {
                preview.forEachIndexed { index, notif ->
                    val rowVisible = visible.getOrElse(index) { false }

                    AnimatedVisibility(
                        visible = rowVisible,
                        enter = fadeIn(tween(250)) + slideInVertically(
                            animationSpec = tween(280),
                            initialOffsetY = { 32 },
                        ),
                    ) {
                        Column {
                            NotificationRow(
                                notification = notif,
                                resolvedTitle = notif.resolveTitle(i18n),
                                onClick = { onNotificationClick(notif) },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }
                    }
                }

                SeeAllRow(onClick = onSeeAllClick)
            }
        }
    }
}

@Composable
private fun SeeAllRow(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = tr("app.today.notifications.see_all", fallback = "Vedi tutte"),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun NotificationRow(
    notification: NotificationModel,
    resolvedTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isUnread = notification.seen == null
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = if (isPressed) 0.98f else 1f
                scaleY = if (isPressed) 0.98f else 1f
            },
        color = colorScheme.surface.copy(alpha = 0f),
        interactionSource = interactionSource,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = if (isUnread) colorScheme.primaryContainer else colorScheme.tertiaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = notification.iconForKind(),
                        contentDescription = null,
                        tint = if (isUnread) colorScheme.onPrimaryContainer else colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = resolvedTitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    ),
                    color = if (isUnread) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = notification.created.timeAgo(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }

            if (isUnread) {
                Spacer(Modifier.width(8.dp))
                Badge(containerColor = colorScheme.primary)
            }
        }
    }
}

// ─── i18n resolution ─────────────────────────────────────────────────────────

/**
 * Resolves the raw notification tr key to a human-readable string.
 *
 * NotificationModel.tr comes from the backend as an i18n key
 * (e.g. "push_notification.new_document_available").
 * When the Tolgee catalog is missing the key, we fall back to a humanized
 * version of the key's last segment.
 */
private fun NotificationModel.resolveTitle(i18n: it.mensa.shared.i18n.I18n): String {
    val raw = tr.trim()
    if (raw.isBlank()) return "Notifica"

    // Check if it looks like an i18n key (dots, underscores, no spaces)
    val looksLikeKey = !raw.contains(' ') && (raw.contains('.') || raw.contains('_'))

    return if (looksLikeKey) {
        val humanFallback = raw
            .substringAfterLast('.')
            .replace('_', ' ')
            .replaceFirstChar { it.uppercase() }
        i18n.t(raw, humanFallback, emptyMap())
    } else {
        raw
    }
}

private fun NotificationModel.iconForKind(): ImageVector {
    val key = tr.lowercase()
    return when {
        key.contains("event") || key.contains("evento") -> Icons.Outlined.CalendarMonth
        key.contains("deal") || key.contains("offer") || key.contains("offerta") -> Icons.Outlined.LocalOffer
        key.contains("document") || key.contains("documento") -> Icons.Outlined.Description
        else -> Icons.Outlined.Notifications
    }
}

// ─── Time formatting ─────────────────────────────────────────────────────────

private fun kotlinx.datetime.Instant.timeAgo(): String {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val diffMs = nowMs - toEpochMilliseconds()
    val diffMin = diffMs / 60_000
    val diffHour = diffMin / 60
    val diffDay = diffHour / 24
    val diffWeek = diffDay / 7

    return when {
        diffMin < 1 -> "Adesso"
        diffMin < 60 -> "${diffMin} min fa"
        diffHour < 24 -> "${diffHour}h fa"
        diffDay == 1L -> "Ieri"
        diffDay < 7 -> "${diffDay} giorni fa"
        diffWeek == 1L -> "1 settimana fa"
        diffWeek < 4 -> "$diffWeek settimane fa"
        else -> {
            val local = toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.dayOfMonth}/${local.monthNumber}/${local.year}"
        }
    }
}
