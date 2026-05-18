package it.mensa.app.features.profile._components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.tr
import it.mensa.app.ui.theme.MensaCyan
import it.mensa.shared.model.UserModel
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * ProfileMetricsRow — broken-grid metric chips below the hero zone.
 *
 * Three tonal variants side-by-side so the screen reads with M3 Expressive
 * tonal variety (not all-blue). Each chip carries an icon badge + value + label.
 *
 * NOT a uniform grid — corner radii and tonal containers vary deliberately.
 */
@Composable
fun ProfileMetricsRow(
    user: UserModel?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme

    val joinedLabel = remember(user?.created) {
        val ts = user?.created?.toEpochMilliseconds() ?: 0L
        if (ts <= 0L) "—" else formatShortMonthYear(ts)
    }

    val expiryLabel = remember(user?.expireMembership) {
        val ts = user?.expireMembership?.toEpochMilliseconds() ?: 0L
        if (ts <= 0L) "—" else formatShortMonthYear(ts)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // 1 — Joined (tertiary container, mauve)
        MetricBlock(
            icon = Icons.Outlined.CalendarMonth,
            value = joinedLabel,
            label = tr("app.profile.metric_joined", fallback = "ISCRITTO"),
            shape = RoundedCornerShape(
                topStart = 24.dp,
                topEnd = 12.dp,
                bottomEnd = 12.dp,
                bottomStart = 20.dp,
            ),
            variant = MetricVariant.Tertiary,
            scheme = scheme,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        )
        // 2 — Membership expiry (primary container, blue)
        MetricBlock(
            icon = Icons.Outlined.CardMembership,
            value = expiryLabel,
            label = tr("app.profile.metric_renewal", fallback = "RINNOVO"),
            shape = RoundedCornerShape(12.dp),
            variant = MetricVariant.Primary,
            scheme = scheme,
            modifier = Modifier
                .weight(1.05f)
                .fillMaxSize(),
        )
        // 3 — Events / placeholder (cyan drenched)
        MetricBlock(
            icon = Icons.Outlined.Event,
            value = tr("app.profile.metric_events_value", fallback = "—"),
            label = tr("app.profile.metric_events", fallback = "EVENTI"),
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 24.dp,
                bottomEnd = 20.dp,
                bottomStart = 12.dp,
            ),
            variant = MetricVariant.Cyan,
            scheme = scheme,
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
        )
    }
}

private enum class MetricVariant { Primary, Tertiary, Cyan }

private data class MetricColors(
    val container: Color,
    val onContainer: Color,
    val iconBg: Color,
    val iconTint: Color,
    val labelColor: Color,
)

@Composable
private fun MetricVariant.toColors(scheme: ColorScheme): MetricColors = when (this) {
    MetricVariant.Primary -> MetricColors(
        container = scheme.primaryContainer,
        onContainer = scheme.onPrimaryContainer,
        iconBg = scheme.primary.copy(alpha = 0.15f),
        iconTint = scheme.primary,
        labelColor = scheme.primary,
    )
    MetricVariant.Tertiary -> MetricColors(
        container = scheme.tertiaryContainer,
        onContainer = scheme.onTertiaryContainer,
        iconBg = scheme.tertiary.copy(alpha = 0.18f),
        iconTint = scheme.tertiary,
        labelColor = scheme.tertiary,
    )
    MetricVariant.Cyan -> MetricColors(
        container = MensaCyan,
        onContainer = Color(0xFF002C3F),
        iconBg = Color.White.copy(alpha = 0.32f),
        iconTint = Color(0xFF002C3F),
        labelColor = Color(0xFF002C3F).copy(alpha = 0.78f),
    )
}

@Composable
private fun MetricBlock(
    icon: ImageVector,
    value: String,
    label: String,
    shape: RoundedCornerShape,
    variant: MetricVariant,
    scheme: ColorScheme,
    modifier: Modifier = Modifier,
) {
    val colors = variant.toColors(scheme)

    Box(
        modifier = modifier
            .clip(shape)
            .background(colors.container)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Icon badge in top corner
        Box(
            modifier = Modifier
                .size(28.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(50))
                .background(colors.iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.iconTint,
                modifier = Modifier.size(16.dp),
            )
        }
        // Value + label stacked bottom-left
        Column(
            modifier = Modifier.align(Alignment.BottomStart),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.onContainer,
                    fontSize = 18.sp,
                    lineHeight = 22.sp,
                ),
                maxLines = 1,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.labelColor,
                    letterSpacing = 1.sp,
                ),
                maxLines = 1,
            )
        }
    }
}

// ─── Helpers ───────────────────────────────────────────────────────────────────

private fun formatShortMonthYear(epochMillis: Long): String {
    return try {
        val instant = kotlinx.datetime.Instant.fromEpochMilliseconds(epochMillis)
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val months = listOf(
            "GEN", "FEB", "MAR", "APR", "MAG", "GIU",
            "LUG", "AGO", "SET", "OTT", "NOV", "DIC",
        )
        val month = months.getOrElse(local.monthNumber - 1) { "?" }
        "$month ${local.year % 100}"
    } catch (e: Exception) {
        "—"
    }
}
