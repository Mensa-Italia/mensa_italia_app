package it.mensa.app.features.leaderboard

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.features.tableport.medals.MedalArtwork
import it.mensa.app.features.tableport.medals.MedalTier
import it.mensa.app.support.tr
import it.mensa.shared.leaderboard.LeaderboardRow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val StepShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 6.dp)

/**
 * The first three on a podium under three spotlights: second, first, third,
 * each on a step of their metal that rises with a spring when the page
 * opens, third first and first last; then the members land on top. Tapping
 * one opens their profile. Mirrors iOS `LeaderboardPodium`.
 */
@Composable
internal fun LeaderboardPodium(
    rows: List<LeaderboardRow>,
    onMemberClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    // How far above the podium the spotlights start: up to the top of the
    // screen, when the podium sits under the bar.
    spotlightReach: Dp = 0.dp,
) {
    // Left to right: second, first, third.
    val places = listOf(1, 0, 2).mapNotNull { index -> rows.getOrNull(index)?.let { index to it } }
    val rise = remember { List(3) { Animatable(0f) } }
    LaunchedEffect(Unit) {
        // Third, second, first.
        listOf(2, 1, 0).forEach { index ->
            launch { rise[index].animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)) }
            delay(140)
        }
    }

    Box(modifier.fillMaxWidth()) {
        Spotlights(rise = rise, modifier = Modifier.matchParentSize())
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = spotlightReach + 28.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            places.forEach { (index, row) ->
                PodiumColumn(
                    row = row,
                    stepHeight = StepHeights[index],
                    avatarSize = if (index == 0) 84.dp else 64.dp,
                    rise = rise[index],
                    onClick = { if (row.member != null && !row.isMe) onMemberClick(row.id) },
                    modifier = Modifier.weight(if (index == 0) 1.15f else 1f),
                )
            }
        }
    }
}

private val StepHeights = listOf(132.dp, 100.dp, 76.dp)

/** Three cones of light from above, one on each step, the middle one brightest. */
@Composable
private fun Spotlights(rise: List<Animatable<Float, *>>, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // Centres of the three columns, as laid out: 1 : 1.15 : 1.
        listOf(0.5f to 0, 0.165f to 1, 0.835f to 2).forEach { (x, index) ->
            val tier = podiumTier(index + 1) ?: MedalTier.Bronze
            val strength = rise[index].value.coerceIn(0f, 1f) * if (index == 0) 1f else 0.7f
            val cone = Path().apply {
                moveTo(w * (x - 0.035f), 0f)
                lineTo(w * (x + 0.035f), 0f)
                lineTo(w * (x + 0.17f), h * 0.78f)
                lineTo(w * (x - 0.17f), h * 0.78f)
                close()
            }
            drawPath(
                cone,
                Brush.verticalGradient(
                    listOf(tier.metal.hi.copy(alpha = 0.22f * strength), Color.Transparent),
                    endY = h * 0.78f,
                ),
            )
        }
    }
}

@Composable
private fun PodiumColumn(
    row: LeaderboardRow,
    stepHeight: Dp,
    avatarSize: Dp,
    rise: Animatable<Float, *>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tier = podiumTier(row.position) ?: MedalTier.Bronze
    val metal = tier.metal
    val name = if (row.isMe) tr("leaderboard.you", "Tu") else row.displayName
    val points = tr("leaderboard.points", "{points} pt", "points" to row.score)
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "${row.position}°, $name, $points" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // The member lands on the step once it's up.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer {
                val landed = ((rise.value - 0.6f) / 0.4f).coerceIn(0f, 1f)
                alpha = landed
                translationY = (1f - landed) * -28.dp.toPx()
            },
        ) {
            Box {
                Box(
                    Modifier
                        .size(avatarSize + 12.dp)
                        .drawBehind {
                            drawCircle(
                                Brush.radialGradient(listOf(metal.mid.copy(alpha = 0.55f), Color.Transparent)),
                                radius = size.minDimension * 0.85f,
                            )
                        }
                        .border(3.dp, Brush.sweepGradient(listOf(metal.hi, metal.mid, metal.lo, metal.mid, metal.hi)), CircleShape)
                        .padding(6.dp),
                ) {
                    RankAvatar(row = row, size = avatarSize, hiRes = true)
                }
                MedalArtwork(
                    tier = tier,
                    icon = Icons.Filled.EmojiEvents,
                    size = if (avatarSize > 70.dp) 30.dp else 24.dp,
                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = 2.dp, y = 2.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Text(
                text = points,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = metal.hi,
            )
        }
        Spacer(Modifier.height(10.dp))
        // The step, rising from the floor with a bounce.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(stepHeight)
                .graphicsLayer {
                    scaleY = rise.value
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .clip(StepShape)
                .background(Brush.verticalGradient(listOf(metal.mid, metal.lo, Color(0xFF0B0E18))))
                .drawBehind {
                    // The lit top face of the step.
                    drawRect(
                        Brush.verticalGradient(listOf(metal.hi.copy(alpha = 0.9f), Color.Transparent), endY = 10.dp.toPx()),
                        size = Size(size.width, 10.dp.toPx()),
                    )
                    // Its rounded sides.
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.White.copy(alpha = 0.12f), Color.Transparent, Color.Black.copy(alpha = 0.25f)),
                        ),
                    )
                },
            contentAlignment = Alignment.TopCenter,
        ) {
            Text(
                text = "${row.position}",
                style = TextStyle(
                    fontSize = if (stepHeight > 110.dp) 64.sp else 48.sp,
                    fontWeight = FontWeight.Black,
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), metal.hi.copy(alpha = 0.55f))),
                    shadow = Shadow(metal.lo, Offset(0f, 3f), 6f),
                ),
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
