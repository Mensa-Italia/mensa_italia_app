package it.mensa.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * LoadingDots — 3-dot animated loading indicator with spring-based wave.
 *
 * Each dot scales from 0.6 to 1.0 with a 120ms stagger between dots,
 * creating a smooth wave effect. Spring-based for physical feel.
 *
 * @param dotSize diameter of each dot (default 10dp per M3 Expressive)
 * @param spacing space between dots (default 8dp)
 * @param color dot color (default colorScheme.primary for brand presence)
 */
@Composable
fun LoadingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 10.dp,
    spacing: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val scales = remember { List(3) { Animatable(0.6f) } }

    scales.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(120L * index)
            animatable.animateTo(
                targetValue = 0.6f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1000
                        0.6f at 0 using FastOutSlowInEasing
                        1.0f at 300 using FastOutSlowInEasing
                        0.6f at 600 using FastOutSlowInEasing
                        0.6f at 1000 using FastOutSlowInEasing
                    },
                    repeatMode = RepeatMode.Restart,
                ),
            )
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        scales.forEach { scale ->
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .scale(scale.value)
                    .background(color = color, shape = CircleShape),
            )
        }
    }
}
