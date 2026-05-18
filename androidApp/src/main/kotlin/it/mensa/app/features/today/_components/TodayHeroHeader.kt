package it.mensa.app.features.today._components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.theme.MensaCyan
import it.mensa.shared.model.UserModel
import java.util.Calendar

/**
 * TodayHeroHeader — M3 Expressive drenched hero zone.
 *
 * Visual:
 * - Full-bleed brand gradient with concave curved bottom edge (GenericShape).
 * - Display-large emphasized "OGGI" at 88sp — the screen's single hero typography moment.
 * - Soft cyan halo behind "OGGI" — animated, breathing.
 * - Status-bar pass-through; light icons via MensaSystemBars(false).
 * - Avatar with pulsing cyan ring on the right.
 *
 * Drenching: hero occupies ~360dp of the viewport (≈45% of typical phone) so the
 * Committed color strategy is honoured (brand carries ≥30% of the surface).
 */
@Composable
fun TodayHeroHeader(
    user: UserModel?,
    formattedDate: String,
    modifier: Modifier = Modifier,
) {
    val greeting = remember { greetingForHour() }
    val firstName = user?.name?.split(" ")?.firstOrNull()?.ifBlank { null }
        ?: user?.username?.split(".")?.firstOrNull()?.replaceFirstChar { it.titlecase() }?.ifBlank { null }
        ?: "Socio"

    val greetingLine = "$greeting, $firstName"

    val avatarUrl = user?.let { u ->
        if (u.avatar.isNotBlank()) {
            FilesUrl.build(collection = "users", recordId = u.id, filename = u.avatar, thumb = "100x100")
        } else null
    }

    // Breathing cyan halo behind "OGGI"
    val haloTransition = rememberInfiniteTransition(label = "hero-halo")
    val haloAlpha by haloTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.36f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "halo-alpha",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 360.dp)
            .clip(WaveHeroShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer,
                    ),
                ),
            ),
    ) {
        // Cyan halo blob — far right behind everything (offset off-screen for soft glow)
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopEnd)
                .offset(x = 120.dp, y = (-40).dp)
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            // ── Top row: greeting line (left) + avatar (right) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MensaCyan.copy(alpha = 0.9f),
                            letterSpacing = 2.sp,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = greetingLine,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White.copy(alpha = 0.85f),
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.size(16.dp))

                UserAvatarRing(
                    user = user,
                    avatarUrl = avatarUrl,
                    ringAlpha = haloAlpha + 0.20f,
                )
            }

            Spacer(Modifier.height(40.dp))

            // ── Hero display — "OGGI" at 88sp emphasized ──
            Text(
                text = "Oggi",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 88.sp,
                    lineHeight = 92.sp,
                    letterSpacing = (-1.5).sp,
                    color = Color.White,
                ),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "La tua giornata in un colpo d'occhio.",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.White.copy(alpha = 0.78f),
                ),
                maxLines = 2,
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun UserAvatarRing(
    user: UserModel?,
    avatarUrl: String?,
    ringAlpha: Float,
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Outer pulsing cyan ring
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MensaCyan.copy(alpha = ringAlpha.coerceIn(0f, 0.55f))),
        )
        // Inner thin gap
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.15f)),
        )
        // Avatar disc
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            when {
                avatarUrl != null -> CachedAsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                user != null -> {
                    val initials = buildInitials(user)
                    if (initials.isNotBlank()) {
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.labelLarge.copy(
                                color = colorScheme.onSecondaryContainer,
                            ),
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Person,
                            contentDescription = null,
                            tint = colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                else -> Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    tint = colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}

/**
 * Concave curved-bottom shape for the hero zone — gives the brand zone
 * an organic edge instead of a hard horizontal line.
 *
 * Curve depth is 7% of hero height so it scales with status-bar height.
 */
private val WaveHeroShape = GenericShape { size, _ ->
    val curveDepth = size.height * 0.07f
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height - curveDepth)
    quadraticBezierTo(
        size.width / 2f, size.height + curveDepth * 0.6f,
        0f, size.height - curveDepth,
    )
    close()
}

private fun buildInitials(user: UserModel): String {
    val parts = user.name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.size >= 2 -> "${parts.first().first()}${parts.last().first()}".uppercase()
        parts.size == 1 -> parts.first().take(2).uppercase()
        user.username.isNotBlank() -> user.username.take(2).uppercase()
        else -> ""
    }
}

private fun greetingForHour(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..12 -> "Buongiorno"
        in 13..17 -> "Buon pomeriggio"
        in 18..23 -> "Buonasera"
        else -> "Bentornato"
    }
}
