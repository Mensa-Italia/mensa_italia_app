package it.mensa.app.features.members._components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.theme.brandGradient
import it.mensa.shared.model.RegSociModel

/**
 * Compact member cell — iPhone Contacts style.
 * Avatar (circular) + styled name (first regular, last bold).
 */
@Composable
fun MemberCellCompact(
    member: RegSociModel,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(member = member, size = 40.dp)
        StyledName(
            name = member.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

/** Circular avatar with cached remote image and gradient-initial fallback. */
@Composable
fun MemberAvatar(
    member: RegSociModel,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
) {
    // Same `0x100` thumb usato sia in lista che dall'engine Spotlight — è
    // quello sicuramente configurato lato PocketBase. Per il dettaglio è
    // disponibile `MemberHeroAvatar` che fa load progressivo 0x100 → 0x500.
    val imageUrl = remember(member) {
        val raw = member.image
        when {
            raw.isEmpty() -> null
            isPlaceholderUrl(raw) -> null
            raw.startsWith("http://") || raw.startsWith("https://") -> raw
            else -> FilesUrl.build("members_registry", member.id, raw, "0x100")
        }
    }

    val initials = remember(member.name) {
        member.name
            .split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl != null) {
            CachedAsyncImage(
                model = imageUrl,
                contentDescription = member.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(brandGradient),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

/**
 * First name(s) regular, last word bold — Apple Contacts default.
 * Words are Title-cased (backend stores UPPERCASE).
 */
@Composable
fun StyledName(
    name: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val parts = name.split(" ").map { word ->
        word.lowercase().replaceFirstChar { it.uppercaseChar() }
    }
    val text = buildAnnotatedString {
        parts.forEachIndexed { index, word ->
            if (index == parts.lastIndex && parts.size > 1) {
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(word) }
            } else {
                append(word)
                if (index < parts.lastIndex) append(" ")
            }
        }
    }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}

private fun isPlaceholderUrl(url: String) =
    url.contains("cloud32.it/Associazioni/img/Uomo-1.png")
