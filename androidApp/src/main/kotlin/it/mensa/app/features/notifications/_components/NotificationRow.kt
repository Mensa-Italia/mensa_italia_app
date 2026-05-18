package it.mensa.app.features.notifications._components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.features.notifications.notificationDataMap
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.NotificationModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ─── Icon resolver ────────────────────────────────────────────────────────────

fun notificationIcon(n: NotificationModel): ImageVector {
    val dict = it.mensa.app.features.notifications.notificationDataMap(n)
    return when (dict?.get("type")) {
        "event" -> Icons.Outlined.Event
        "single_document", "multiple_documents" -> Icons.Outlined.Description
        "account_confirmation" -> Icons.Outlined.VerifiedUser
        "deal" -> Icons.Outlined.LocalOffer
        "ticket_purchase" -> Icons.Outlined.ConfirmationNumber
        "payment_update_status" -> Icons.Outlined.CreditCard
        "quid" -> Icons.Outlined.Newspaper
        "quid_article" -> Icons.AutoMirrored.Outlined.Article
        "local_office" -> Icons.Outlined.LocationCity
        else -> Icons.Outlined.Notifications
    }
}

// ─── Title / body helpers ─────────────────────────────────────────────────────

fun notificationTitleText(n: NotificationModel): String {
    if (n.tr.isEmpty()) return "Notifica"
    val key = "${n.tr}.title"
    return koinAccess().i18n.t(key, key, n.trNamedParams)
}

fun notificationBodyText(n: NotificationModel): String {
    if (n.tr.isEmpty()) return ""
    val key = "${n.tr}.body"
    return koinAccess().i18n.t(key, "", n.trNamedParams)
}

// ─── Relative time helper ─────────────────────────────────────────────────────

fun relativeTime(instant: Instant): String {
    val now = Clock.System.now()
    val diff = (now - instant).inWholeSeconds
    return when {
        diff < 60 -> "ora"
        diff < 3600 -> "${diff / 60} min fa"
        diff < 86400 -> "${diff / 3600} ore fa"
        diff < 172800 -> "ieri"
        diff < 604800 -> "${diff / 86400} giorni fa"
        else -> {
            val tz = TimeZone.currentSystemDefault()
            val d = instant.toLocalDateTime(tz)
            "${d.dayOfMonth}/${d.monthNumber}/${d.year}"
        }
    }
}

// ─── Row composable ───────────────────────────────────────────────────────────

/**
 * NotificationRow — M3 canonico.
 * Leading: Surface/CircleShape icon badge (primaryContainer se non letto, surfaceVariant se letto).
 * Center: title (semibold se non letto) + body + relative time.
 * Trailing: unread dot quando seen == null.
 */
@Composable
fun NotificationRow(
    notification: NotificationModel,
    modifier: Modifier = Modifier,
) {
    val isUnread = notification.seen == null
    val icon = remember(notification.id) { notificationIcon(notification) }
    val title = remember(notification.id, notification.tr) { notificationTitleText(notification) }
    val body = remember(notification.id, notification.tr) { notificationBodyText(notification) }
    val timeLabel = remember(notification.created) { relativeTime(notification.created) }

    val primary = MaterialTheme.colorScheme.primary

    // Discipline: primaryContainer se unread+event/deal, primaryContainer se unread, tertiaryContainer altrimenti
    val notifType = it.mensa.app.features.notifications.notificationDataMap(notification)?.get("type")
    val badgeBackground = when {
        isUnread && notifType in listOf("event", "deal") -> MaterialTheme.colorScheme.tertiaryContainer
        isUnread -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeTint = when {
        isUnread && notifType in listOf("event", "deal") -> MaterialTheme.colorScheme.onTertiaryContainer
        isUnread -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        // Leading icon badge
        Surface(shape = CircleShape, color = badgeBackground, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = badgeTint, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Center column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
            )
            if (body.isNotEmpty()) {
                Text(text = body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, modifier = Modifier.padding(top = 2.dp))
            }
            Text(text = timeLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), modifier = Modifier.padding(top = 4.dp))
        }

        // Trailing unread dot
        if (isUnread) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.padding(top = 6.dp).size(8.dp).background(color = primary, shape = CircleShape))
        }
    }
}
