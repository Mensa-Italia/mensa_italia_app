package it.mensa.app.features.profile._components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.ui.theme.MensaMotion

/**
 * Tonal flavour for a [ProfileSectionGroup] and its rows.
 *
 * Each section uses a different colorScheme container family to deliver
 * the M3 Expressive "tonal variety" lever — the screen should NOT be all-primary.
 */
enum class ProfileSectionTone {
    /** primaryContainer header card — Account block */
    Primary,
    /** tertiaryContainer header card — Subscription block (warm mauve) */
    Tertiary,
    /** surfaceContainerHigh header card — App / system block */
    Neutral,
    /** errorContainer header card — Logout block (used sparingly) */
    Error,
}

@Composable
fun ProfileSectionGroup(
    kicker: String,
    title: String,
    tone: ProfileSectionTone,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScopeProvider.() -> Unit,
) {
    val palette = sectionPalette(tone)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = palette.groupContainer,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        ) {
            // Header — kicker + title, drenched in tone color
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = if (title.isBlank()) 12.dp else 18.dp),
            ) {
                // KickerLabel eliminato — sostituito con titleSmall colore primary
                if (title.isNotBlank()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = palette.headerColor,
                        ),
                    )
                }
            }

            // Rows render against a slightly lighter inner surface so they read
            // as nested children of the drenched group header.
            val scope = remember(palette) { ColumnScopeProvider(palette) }
            Column(modifier = Modifier.fillMaxWidth()) {
                scope.content()
            }
        }
    }
}

/**
 * Receiver scope passed into the [ProfileSectionGroup] content lambda so rows
 * can pick up the parent palette without callers having to thread it through.
 */
class ColumnScopeProvider internal constructor(
    internal val palette: ProfileSectionPalette,
)

/**
 * A single row inside a [ProfileSectionGroup].
 *
 * Shape morphs from 22dp → 14dp on press, mirroring the Today screen's
 * shape-morph lever. Icon badge picks up the section's accent tint.
 */
@Composable
fun ColumnScopeProvider.ProfileRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    subtitle: String? = null,
    trailing: String? = null,
    accentOverride: Color? = null,
    isDestructive: Boolean = false,
) {
    ProfileRowScaffold(
        icon = icon,
        title = title,
        modifier = modifier,
        onClick = onClick,
        subtitle = subtitle,
        accentOverride = accentOverride,
        isDestructive = isDestructive,
        trailingContent = {
            if (!trailing.isNullOrBlank()) {
                Text(
                    text = trailing,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = palette.rowSubtitleColor,
                    ),
                )
                Spacer(Modifier.width(4.dp))
            }
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = if (isDestructive)
                        MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    else
                        palette.rowSubtitleColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    )
}

/**
 * Variant with a trailing [Switch] — the click target is the whole row.
 * Use for boolean preferences (es. notifiche on/off).
 */
@Composable
fun ColumnScopeProvider.ProfileToggleRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    ProfileRowScaffold(
        icon = icon,
        title = title,
        modifier = modifier,
        onClick = { onCheckedChange(!checked) },
        subtitle = subtitle,
        accentOverride = null,
        isDestructive = false,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

/**
 * Variant with a trailing dropdown — current value shown as label, tap to
 * open an M3 [DropdownMenu] anchored to the row.
 *
 * Use for inline single-choice preferences (es. tema sistema/chiaro/scuro).
 */
@Composable
fun <T> ColumnScopeProvider.ProfileDropdownRow(
    icon: ImageVector,
    title: String,
    value: T,
    options: List<T>,
    onSelect: (T) -> Unit,
    labelFor: @Composable (T) -> String,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        ProfileRowScaffold(
            icon = icon,
            title = title,
            modifier = modifier,
            onClick = { expanded = true },
            subtitle = null,
            accentOverride = null,
            isDestructive = false,
            trailingContent = {
                Text(
                    text = labelFor(value),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = palette.rowSubtitleColor,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = palette.rowSubtitleColor,
                    modifier = Modifier.size(20.dp),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(labelFor(option)) },
                    onClick = {
                        expanded = false
                        onSelect(option)
                    },
                )
            }
        }
    }
}

/**
 * Internal scaffold shared by [ProfileRow], [ProfileToggleRow], and
 * [ProfileDropdownRow] so they look identical (drenched container, shape
 * morph on press, icon badge) and differ only in their trailing content.
 */
@Composable
private fun ColumnScopeProvider.ProfileRowScaffold(
    icon: ImageVector,
    title: String,
    modifier: Modifier,
    onClick: (() -> Unit)?,
    subtitle: String?,
    accentOverride: Color?,
    isDestructive: Boolean,
    trailingContent: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cornerRadius by animateDpAsState(
        targetValue = if (isPressed && onClick != null) 14.dp else 22.dp,
        animationSpec = MensaMotion.springShape,
        label = "row-corner-morph",
    )
    val shape = RoundedCornerShape(cornerRadius)

    val iconTint = when {
        isDestructive -> MaterialTheme.colorScheme.error
        accentOverride != null -> accentOverride
        else -> palette.rowAccent
    }
    val iconBg = when {
        isDestructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.14f)
        accentOverride != null -> accentOverride.copy(alpha = 0.16f)
        else -> palette.rowAccent.copy(alpha = 0.16f)
    }
    val titleColor = if (isDestructive) MaterialTheme.colorScheme.error else palette.rowTitleColor
    val subtitleColor = if (isDestructive)
        MaterialTheme.colorScheme.error.copy(alpha = 0.80f)
    else
        palette.rowSubtitleColor

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .heightIn(min = 60.dp)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(50))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                    ),
                    maxLines = 1,
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = subtitleColor,
                        ),
                        maxLines = 1,
                    )
                }
            }

            trailingContent()
        }
    }

    val rowModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 4.dp)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = rowModifier,
            shape = shape,
            color = palette.rowContainer,
            tonalElevation = 0.dp,
            interactionSource = interactionSource,
        ) { rowContent() }
    } else {
        Surface(
            modifier = rowModifier,
            shape = shape,
            color = palette.rowContainer,
            tonalElevation = 0.dp,
        ) { rowContent() }
    }
}

// ─── Palette ──────────────────────────────────────────────────────────────────

class ProfileSectionPalette(
    val groupContainer: Color,
    val headerColor: Color,
    val kickerColor: Color,
    val rowContainer: Color,
    val rowAccent: Color,
    val rowTitleColor: Color,
    val rowSubtitleColor: Color,
)

@Composable
fun sectionPalette(tone: ProfileSectionTone): ProfileSectionPalette {
    val scheme = MaterialTheme.colorScheme
    return when (tone) {
        ProfileSectionTone.Primary -> ProfileSectionPalette(
            groupContainer = scheme.primaryContainer,
            headerColor = scheme.onPrimaryContainer,
            kickerColor = scheme.primary,
            rowContainer = scheme.surface,
            rowAccent = scheme.primary,
            rowTitleColor = scheme.onSurface,
            rowSubtitleColor = scheme.onSurfaceVariant,
        )
        ProfileSectionTone.Tertiary -> ProfileSectionPalette(
            groupContainer = scheme.tertiaryContainer,
            headerColor = scheme.onTertiaryContainer,
            kickerColor = scheme.tertiary,
            rowContainer = scheme.surface,
            rowAccent = scheme.tertiary,
            rowTitleColor = scheme.onSurface,
            rowSubtitleColor = scheme.onSurfaceVariant,
        )
        ProfileSectionTone.Neutral -> ProfileSectionPalette(
            groupContainer = scheme.surfaceContainerHigh,
            headerColor = scheme.onSurface,
            kickerColor = scheme.primary,
            rowContainer = scheme.surface,
            rowAccent = scheme.secondary,
            rowTitleColor = scheme.onSurface,
            rowSubtitleColor = scheme.onSurfaceVariant,
        )
        ProfileSectionTone.Error -> ProfileSectionPalette(
            groupContainer = scheme.errorContainer,
            headerColor = scheme.onErrorContainer,
            kickerColor = scheme.error,
            rowContainer = scheme.errorContainer.copy(alpha = 0.5f),
            rowAccent = scheme.error,
            rowTitleColor = scheme.error,
            rowSubtitleColor = scheme.onErrorContainer.copy(alpha = 0.8f),
        )
    }
}
