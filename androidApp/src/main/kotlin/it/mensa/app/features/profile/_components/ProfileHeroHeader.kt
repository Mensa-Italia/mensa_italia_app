package it.mensa.app.features.profile._components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.theme.MensaCyan
import it.mensa.app.ui.theme.MensaSystemBars
import it.mensa.shared.model.UserModel

/**
 * ProfileHeroHeader — M3 Expressive drenched hero for the Profile tab.
 *
 * Mirrors TodayHeroHeader's drenched-zone pattern, but the headline is the user
 * themselves: 96dp avatar with cyan halo ring, kicker, headlineLargeEmphasized
 * full name, supporting member-since line, and a "Socio attivo" chip.
 *
 * Drenching: brand gradient with curved bottom edge so the hero feels organic.
 */
@Composable
fun ProfileHeroHeader(
    user: UserModel?,
    modifier: Modifier = Modifier,
) {
    MensaSystemBars(darkIcons = false)

    // Breathing cyan halo behind avatar
    val haloTransition = rememberInfiniteTransition(label = "profile-hero-halo")
    val haloAlpha by haloTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.42f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo-alpha",
    )

    val displayName = remember(user?.name, user?.username, user?.email) {
        deriveDisplayName(user)
    }
    val supportingLine = remember(user?.email, user?.created) {
        deriveSupportingLine(user)
    }
    val avatarUrl = user?.let { u ->
        if (u.avatar.isNotBlank()) {
            FilesUrl.build(collection = "users", recordId = u.id, filename = u.avatar, thumb = "300x300")
        } else null
    }
    val initials = remember(user?.name, user?.username, user?.email) {
        deriveInitials(user)
    }
    val isActive = user?.isMembershipActive == true

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 380.dp)
            .clip(ProfileHeroShape)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        // Off-screen cyan blob — soft halo behind everything
        Box(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.TopCenter)
                .offset(y = (-80).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MensaCyan.copy(alpha = haloAlpha),
                            MensaCyan.copy(alpha = 0f),
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 24.dp)
                .padding(top = 32.dp, bottom = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Kicker — cyan, all caps, wide tracking
            Text(
                text = tr("app.profile.hero_kicker", fallback = "IL TUO ACCOUNT"),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = MensaCyan,
                    letterSpacing = 2.sp,
                ),
            )

            Spacer(Modifier.height(20.dp))

            // 96dp avatar with breathing cyan halo ring
            ProfileAvatarHalo(
                avatarUrl = avatarUrl,
                initials = initials,
                haloAlpha = haloAlpha,
            )

            Spacer(Modifier.height(20.dp))

            // Hero name — single headlineLargeEmphasized moment for this screen
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 36.sp,
                ),
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            if (supportingLine.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = supportingLine,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White.copy(alpha = 0.78f),
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Status chip — cyan when active, neutral otherwise
            MembershipStatusChip(isActive = isActive)
        }
    }
}

@Composable
private fun ProfileAvatarHalo(
    avatarUrl: String?,
    initials: String,
    haloAlpha: Float,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.size(124.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outermost halo — radial cyan glow
        Box(
            modifier = Modifier
                .size(124.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MensaCyan.copy(alpha = (haloAlpha + 0.20f).coerceIn(0f, 0.65f)),
                            MensaCyan.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
        // Solid cyan ring
        Box(
            modifier = Modifier
                .size(108.dp)
                .clip(CircleShape)
                .background(MensaCyan.copy(alpha = 0.85f)),
        )
        // Thin white gap
        Box(
            modifier = Modifier
                .size(102.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
        // Avatar disc — 96dp as requested
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            when {
                avatarUrl != null -> CachedAsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                initials.isNotBlank() -> Text(
                    text = initials,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSecondaryContainer,
                    ),
                )
                else -> Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(40.dp),
                )
            }
        }
    }
}

@Composable
private fun MembershipStatusChip(isActive: Boolean) {
    val (background, foreground) = if (isActive) {
        MensaCyan.copy(alpha = 0.22f) to MensaCyan
    } else {
        Color.White.copy(alpha = 0.14f) to Color.White.copy(alpha = 0.85f)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Verified,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = if (isActive)
                tr("app.profile.hero_chip_active", fallback = "Socio attivo")
            else
                tr("app.profile.hero_chip_inactive", fallback = "Socio"),
            style = MaterialTheme.typography.labelMedium.copy(
                color = foreground,
                letterSpacing = 0.6.sp,
            ),
        )
    }
}

// ─── Concave curved hero shape — matches Today hero language ──────────────────

private val ProfileHeroShape = GenericShape { size, _ ->
    val curveDepth = size.height * 0.06f
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - curveDepth)
    quadraticBezierTo(
        size.width / 2f, size.height + curveDepth * 0.6f,
        0f, size.height - curveDepth,
    )
    close()
}

// ─── Name / line / initial derivation — mirrors CardScreen fallback chain ─────

private fun deriveDisplayName(user: UserModel?): String {
    if (user == null) return "Socio Mensa"
    val direct = user.name.trim().ifBlank { user.username.trim() }
    if (direct.isNotBlank()) return direct
    // Build from email local-part: marco.montanari@mensa.it -> "Marco Montanari"
    val local = user.email.substringBefore('@', "").trim()
    if (local.isBlank()) return "Socio Mensa"
    return local.replace('.', ' ').replace('_', ' ').replace('-', ' ')
        .split(' ').filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
        .ifBlank { "Socio Mensa" }
}

private fun deriveInitials(user: UserModel?): String {
    if (user == null) return ""
    val baseName = user.name.trim().ifBlank { user.username.trim() }.ifBlank {
        user.email.substringBefore('@', "").replace('.', ' ').replace('_', ' ').trim()
    }
    if (baseName.isBlank()) return ""
    val parts = baseName.split(' ', '.', '_', '-').filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts.first().take(2).uppercase()
        else -> ""
    }
}

private fun deriveSupportingLine(user: UserModel?): String {
    if (user == null) return ""
    val emailLine = user.email.ifBlank { null }
    return emailLine ?: ""
}
