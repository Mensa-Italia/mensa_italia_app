package it.mensa.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * SwipeRevealRow — horizontal swipe-to-action affordance.
 *
 * Prepares a list item for M3 Expressive swipe actions (e.g. delete, archive).
 * Uses spring physics for the reveal and snap-back with haptic affordance.
 *
 * @param revealContent the action content shown when swiped (e.g. delete button)
 * @param revealWidth maximum px the row reveals when fully swiped
 * @param content the main row content (rendered in front)
 */
@Composable
fun SwipeRevealRow(
    revealContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    revealWidth: Float = 200f,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier.height(IntrinsicSize.Min),
    ) {
        // Reveal layer (actions behind)
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight(),
            contentAlignment = Alignment.CenterEnd,
        ) {
            revealContent()
        }

        // Front content layer (the list row)
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                // Snap: if revealed more than half, stay open; else snap closed
                                val target = if (-offsetX.value > revealWidth / 2) {
                                    -revealWidth
                                } else {
                                    0f
                                }
                                offsetX.animateTo(
                                    targetValue = target,
                                    animationSpec = spring(
                                        dampingRatio = 0.75f,
                                        stiffness = 500f,
                                    ),
                                )
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val newValue = (offsetX.value + dragAmount)
                                    .coerceIn(-revealWidth, 0f)
                                offsetX.snapTo(newValue)
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
