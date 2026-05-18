package it.mensa.app.features.today._components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.theme.MensaBlue
import it.mensa.app.ui.theme.MensaCyan
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.model.UserModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * MembershipCardPreview — M3 Expressive XL pillowy hero card.
 *
 * Visual:
 * - 32dp XL corners (morphs to 24dp on press via springShape)
 * - Cyan ↔ deep blue diagonal gradient
 * - Animated cyan shimmer band sweeping diagonally every 6s
 * - displayMediumEmphasized member name (48sp) — second hero typography moment of the screen
 * - Member code in monospace, expiry chip on the right
 *
 * Drenching: the whole card is brand-saturated. Sits on Parchment so it pops.
 */
@Composable
fun MembershipCardPreview(
    user: UserModel?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Shape morph on press: 32dp → 24dp
    val pressTransition = updateTransition(targetState = isPressed, label = "press")
    val cornerRadius by pressTransition.animateDp(
        transitionSpec = { MensaMotion.springShape },
        label = "corner",
    ) { pressed -> if (pressed) 24.dp else 32.dp }

    // Shimmer sweep across the card
    val shimmer = rememberInfiniteTransition(label = "tessera-shimmer")
    val shimmerOffset by shimmer.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5800),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-x",
    )

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(168.dp),
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent,
        interactionSource = interactionSource,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(cornerRadius))
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MensaBlue,
                            MensaBlue.copy(red = 0.05f, green = 0.18f, blue = 0.42f),
                            MensaCyan.copy(alpha = 0.45f),
                        ),
                        start = Offset(0f, 0f),
                        end = Offset.Infinite,
                    ),
                ),
        ) {
            // Shimmer band — translucent cyan diagonal sweep
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                MensaCyan.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.10f),
                                MensaCyan.copy(alpha = 0.18f),
                                Color.Transparent,
                            ),
                            start = Offset(shimmerOffset * 1200f, 0f),
                            end = Offset(shimmerOffset * 1200f + 400f, 600f),
                        ),
                    ),
            )

            if (user == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    LoadingDots(color = Color.White.copy(alpha = 0.7f))
                }
            } else {
                CardContent(user = user)
            }
        }
    }
}

@Composable
private fun CardContent(user: UserModel) {
    val displayName = remember(user.name, user.username, user.email) {
        user.name.ifBlank { user.username }.ifBlank {
            user.email.substringBefore("@")
                .split(".", "_", "-")
                .filter { it.isNotBlank() }
                .joinToString(" ") { it.replaceFirstChar { c -> c.titlecase() } }
                .ifBlank { "Socio Mensa" }
        }
    }
    val memberCode = formatMemberCode(user.id)
    val expiry = user.expireMembership.formatExpiryShort()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // Top: label + chevron
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "LA TUA TESSERA",
                style = MaterialTheme.typography.titleSmall,
                color = MensaCyan,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // Member name — hero typography
        Text(
            text = displayName.uppercase(),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 30.sp,
                lineHeight = 32.sp,
                letterSpacing = (-0.5).sp,
            ),
            maxLines = 2,
        )

        Spacer(Modifier.height(8.dp))

        // Bottom: member code + expiry
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Codice",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = MensaCyan.copy(alpha = 0.85f),
                        letterSpacing = 1.sp,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = memberCode,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        letterSpacing = 1.sp,
                    ),
                )
            }

            if (expiry.isNotBlank() && expiry != "—") {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = "scade $expiry",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = Color.White,
                            letterSpacing = 0.5.sp,
                        ),
                    )
                }
            }
        }
    }
}

private fun formatMemberCode(rawId: String): String {
    if (rawId.isBlank()) return "———"
    return if (rawId.length >= 8) {
        rawId.take(4).uppercase() + " · " + rawId.drop(4).take(4).uppercase()
    } else {
        rawId.uppercase()
    }
}

private fun Instant.formatExpiryShort(): String {
    if (toEpochMilliseconds() <= 0L) return "—"
    return try {
        val local = toLocalDateTime(TimeZone.currentSystemDefault())
        val months = listOf(
            "GEN", "FEB", "MAR", "APR", "MAG", "GIU",
            "LUG", "AGO", "SET", "OTT", "NOV", "DIC",
        )
        val month = months.getOrElse(local.monthNumber - 1) { "?" }
        "$month ${local.year}"
    } catch (e: Exception) {
        "—"
    }
}
