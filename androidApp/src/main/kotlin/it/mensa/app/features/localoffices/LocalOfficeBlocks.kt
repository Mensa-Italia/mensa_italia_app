package it.mensa.app.features.localoffices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.support.AppFormat
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.rememberAppLocale
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeModel
import java.util.Locale

// I blocchi della pagina di una sede, estratti da LocalOfficeScreen perche'
// li usa anche la tab "Gruppo locale" (LocalGroupScreen): una sola
// implementazione per entrambe. Il comportamento in LocalOfficeScreen e'
// quello di prima (i parametri nuovi hanno i valori di allora come default).

// ─── Hero (immagine, nome, bio) ──────────────────────────────────────────────

/**
 * The office's cover (or the building placeholder), then its name and bio.
 * [showRegion] adds the region under the name (the "Gruppo locale" tab;
 * LocalOfficeScreen keeps it off, as before).
 */
@Composable
internal fun LocalOfficeHero(office: LocalOfficeModel, showRegion: Boolean = false) {
    Column(Modifier.fillMaxWidth()) {
        // ── Hero image ──────────────────────────────────────────
        val coverUrl = if (office.image.isEmpty()) null
        else FilesUrl.build("local_offices", office.id, office.image, "1200x0")

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            if (coverUrl != null) {
                CachedAsyncImage(
                    model = coverUrl,
                    contentDescription = office.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Business,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // ── Identity (name + region + bio) ──────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                office.name,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (showRegion && office.region.isNotBlank()) {
                Text(
                    office.region,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (office.bio.isNotBlank()) {
                Text(
                    office.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
    }
}

// ─── Canonical M3 section header ─────────────────────────────────────────────

@Composable
internal fun SectionHeader(
    title: String,
    actionLabel: String? = null,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f),
        )
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                if (actionIcon != null) {
                    Icon(
                        actionIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.size(4.dp))
                }
                Text(actionLabel)
            }
        }
    }
}

@Composable
internal fun GroupedCard(horizontalPadding: Dp = 16.dp, content: @Composable () -> Unit) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding),
        shape = RoundedCornerShape(16.dp),
    ) { content() }
}

// ─── Linktree ────────────────────────────────────────────────────────────────

@Composable
internal fun LinktreeGroup(
    linktree: List<LocalOfficeLinktreeRowModel>,
    onLinkClick: (String) -> Unit,
    horizontalPadding: Dp = 16.dp,
) {
    val sorted = linktree.sortedBy { it.sortOrder }
    val rootLinks = sorted.filter { it.parent.isEmpty() && it.kind == "link" }
    val preview = rootLinks.take(2)
    if (preview.isEmpty()) return

    GroupedCard(horizontalPadding) {
        Column {
            preview.forEachIndexed { idx, link ->
                ListItem(
                    headlineContent = {
                        Text(
                            link.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        Text(
                            resolveIcon(link.icon),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onLinkClick(link.url) },
                )
                if (idx < preview.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}


// ─── People (admins + assistants) ────────────────────────────────────────────

internal data class PersonRowData(
    val recordId: String,   // row id in the view collection (for file URL)
    val userId: String,     // user id (for onClick navigation)
    val name: String,
    val image: String,
    /** Shown under the name (the "Gruppo locale" tab: Segretario / Co-segretario). */
    val role: String? = null,
    /** With [PersonGroup]'s onMailClick, a mail button at the end of the row. */
    val email: String = "",
)

@Composable
internal fun PersonGroup(
    people: List<PersonRowData>,
    onPersonClick: (String) -> Unit,
    collection: String,
    horizontalPadding: Dp = 16.dp,
    onMailClick: ((String) -> Unit)? = null,
) {
    GroupedCard(horizontalPadding) {
        Column {
            people.forEachIndexed { idx, p ->
                val imageUrl = remember(p.recordId, p.image) {
                    if (p.image.isEmpty()) null
                    else FilesUrl.build(collection, p.recordId, p.image, "200x200")
                }
                ListItem(
                    headlineContent = { Text(titleCaseName(p.name, rememberAppLocale())) },
                    supportingContent = p.role?.let { role -> { Text(role) } },
                    leadingContent = { PersonAvatar(imageUrl = imageUrl, name = p.name) },
                    trailingContent = if (onMailClick != null && p.email.isNotBlank()) {
                        {
                            IconButton(onClick = { onMailClick(p.email) }) {
                                Icon(Icons.Outlined.Mail, contentDescription = p.email)
                            }
                        }
                    } else null,
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onPersonClick(p.userId) },
                )
                if (idx < people.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                }
            }
        }
    }
}

@Composable
internal fun PersonAvatar(imageUrl: String?, name: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            CachedAsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize().clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

internal fun titleCaseName(s: String, locale: Locale): String =
    s.split(' ').joinToString(" ") { word -> AppFormat.titlecase(word.lowercase(), locale) }

/** The emoji for a link's icon name; also used by LocalOfficeLinktreeScreen. */
internal fun resolveIcon(raw: String): String {
    val trimmed = raw.trim()
    val lower = trimmed.lowercase()
    return when {
        trimmed.isEmpty() -> "🔗"
        trimmed.codePointCount(0, trimmed.length) == 1 &&
            trimmed.codePoints().findFirst().asInt > 127 -> trimmed
        lower == "instagram" -> "📷"
        lower == "facebook" -> "👤"
        lower == "telegram" -> "✈️"
        lower == "whatsapp" -> "💬"
        lower == "youtube" -> "▶️"
        lower == "email" -> "📧"
        lower == "linkedin" -> "💼"
        lower == "github" -> "💻"
        lower == "twitter" -> "🐦"
        else -> "🔗"
    }
}

