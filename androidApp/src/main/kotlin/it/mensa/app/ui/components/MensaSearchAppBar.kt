package it.mensa.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * MensaSearchAppBar — canonical M3 [SearchBar] in collapsed/launcher mode.
 *
 * The official `androidx.compose.material3.SearchBar` provides container tokens
 * (shape, tonal/shadow elevation, status-bar insets, colors). We render it in
 * `expanded = false` and use a custom non-focusable input slot that acts as a
 * tap-target — taps route to the dedicated Search screen rather than expanding
 * the bar inline. This keeps the visual identity 100% M3 while preserving the
 * existing navigation contract.
 *
 * For an inline expand-with-results experience, swap the input slot for
 * `SearchBarDefaults.InputField` and host the result list in the `content`
 * lambda.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MensaSearchAppBar(
    placeholder: String,
    onSearchTap: () -> Unit,
    modifier: Modifier = Modifier,
    avatar: (@Composable () -> Unit)? = null,
    inlineActions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        SearchBar(
            modifier = Modifier.fillMaxWidth(),
            inputField = {
                SearchBarLauncherField(
                    placeholder = placeholder,
                    onClick = onSearchTap,
                    avatar = avatar,
                    inlineActions = inlineActions,
                )
            },
            expanded = false,
            onExpandedChange = { if (it) onSearchTap() },
            shape = SearchBarDefaults.inputFieldShape,
            colors = SearchBarDefaults.colors(),
            tonalElevation = SearchBarDefaults.TonalElevation,
            shadowElevation = SearchBarDefaults.ShadowElevation,
            windowInsets = WindowInsets(0, 0, 0, 0),
            content = {},
        )
    }
}

/**
 * Non-focusable input slot used inside a collapsed [SearchBar]. Visually mirrors
 * [SearchBarDefaults.InputField]: leading search icon + placeholder body + optional
 * trailing avatar (canonical Gmail/Photos pattern). Taps route to the search
 * screen instead of grabbing keyboard focus; the avatar consumes taps separately.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchBarLauncherField(
    placeholder: String,
    onClick: () -> Unit,
    avatar: (@Composable () -> Unit)?,
    inlineActions: (@Composable RowScope.() -> Unit)?,
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val hasTrailing = avatar != null || inlineActions != null

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SearchBarDefaults.InputFieldHeight)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = placeholder }
            .padding(start = 16.dp, end = if (hasTrailing) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
        )
        Text(
            text = placeholder,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = colorScheme.onSurfaceVariant,
            ),
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp),
        )
        if (inlineActions != null) {
            inlineActions()
        }
        if (avatar != null) {
            avatar()
        }
    }
}

/**
 * Inline notifications [IconButton] designed to live INSIDE the SearchBar trailing
 * area (Google Photos pattern). 40dp touch target with [BadgedBox] + canonical
 * [Badge] for the unread indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchAppBarNotificationsButton(
    onClick: () -> Unit,
    badge: Boolean = false,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.size(40.dp),
    ) {
        BadgedBox(
            badge = {
                if (badge) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Notifiche",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
