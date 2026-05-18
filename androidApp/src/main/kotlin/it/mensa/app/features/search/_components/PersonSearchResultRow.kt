package it.mensa.app.features.search._components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.features.search.HydratedHit
import it.mensa.app.features.search.LocalOfficeAffiliation
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.model.RegSociModel

/**
 * PersonSearchResultRow — avatar + name + role chip + affiliation chips.
 *
 * Mirrors iOS PersonSearchResultRow.swift: uniform row for all members,
 * role-holders distinguished by a thin brand ring + star+role line.
 */
@Composable
fun PersonSearchResultRow(
    member: RegSociModel,
    orgRole: String?,
    orgGroup: String?,
    localOfficeAffiliations: List<LocalOfficeAffiliation> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val hasRole = !orgRole.isNullOrEmpty()
    val brandColor = MaterialTheme.colorScheme.primary

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Avatar with optional role ring
        Box(contentAlignment = Alignment.Center) {
            MemberAvatar(
                member = member,
                size = 40,
                modifier = Modifier
                    .then(
                        if (hasRole) Modifier.padding(2.dp) else Modifier
                    ),
            )
            if (hasRole) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        // Simulate the thin ring with a border-style overlay
                        .padding(1.dp)
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            // Styled name: first names regular, last name bold
            Text(
                text = styledName(member.name),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )

            if (hasRole && orgRole != null) {
                // Role line: ★ Role · Group
                val roleText = buildAnnotatedString {
                    withStyle(SpanStyle(color = brandColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)) {
                        append("★ ")
                        append(orgRole)
                    }
                    if (!orgGroup.isNullOrEmpty()) {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)) {
                            append("  ·  $orgGroup")
                        }
                    }
                }
                Text(
                    text = roleText,
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall,
                )
            } else if (member.city.isNotEmpty()) {
                Text(
                    text = member.city.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            if (localOfficeAffiliations.isNotEmpty()) {
                AffiliationChips(affiliations = localOfficeAffiliations)
            }
        }
    }
}

@Composable
private fun AffiliationChips(affiliations: List<LocalOfficeAffiliation>) {
    val brandColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (aff in affiliations) {
            Row(
                modifier = Modifier
                    .background(
                        color = brandColor.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50),
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (aff.kind == LocalOfficeAffiliation.Kind.Admin) Icons.Filled.Star else Icons.Filled.School,
                    contentDescription = null,
                    tint = brandColor,
                    modifier = Modifier.size(9.dp),
                )
                Text(
                    text = aff.label,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                    color = brandColor,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun MemberAvatar(
    member: RegSociModel,
    size: Int,
    modifier: Modifier = Modifier,
) {
    val imageUrl = if (member.image.isNotEmpty()) {
        FilesUrl.build(
            collection = "members_registry",
            recordId = member.id,
            filename = member.image,
            thumb = "100x100",
        )
    } else null

    if (imageUrl != null) {
        CachedAsyncImage(
            model = imageUrl,
            contentDescription = member.name,
            modifier = modifier
                .size(size.dp)
                .clip(CircleShape),
        )
    } else {
        AvatarFallback(name = member.name, size = size, modifier = modifier)
    }
}

@Composable
fun AvatarFallback(
    name: String,
    size: Int,
    modifier: Modifier = Modifier,
) {
    val initials = name.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")

    Box(
        modifier = modifier
            .size(size.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size / 3.2).sp,
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

private fun styledName(rawName: String): androidx.compose.ui.text.AnnotatedString {
    val parts = rawName.split(" ").map { word ->
        if (word.isEmpty()) word
        else word[0].uppercase() + word.drop(1).lowercase()
    }
    return buildAnnotatedString {
        val last = parts.lastOrNull() ?: ""
        val first = parts.dropLast(1).joinToString(" ")
        if (first.isNotEmpty()) {
            append(first)
            append(" ")
        }
        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) {
            append(last)
        }
    }
}

/** Lean fallback for person when cache hasn't loaded yet */
@Composable
fun LeanPersonSearchResultRow(
    id: String,
    name: String,
    subtitle: String,
    imageFilename: String,
    modifier: Modifier = Modifier,
) {
    val imageUrl = if (imageFilename.isNotEmpty()) {
        FilesUrl.build("members_registry", id, imageFilename, "100x100")
    } else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (imageUrl != null) {
            CachedAsyncImage(
                model = imageUrl,
                contentDescription = name,
                modifier = Modifier.size(40.dp).clip(CircleShape),
            )
        } else {
            AvatarFallback(name = name, size = 40)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 1,
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
