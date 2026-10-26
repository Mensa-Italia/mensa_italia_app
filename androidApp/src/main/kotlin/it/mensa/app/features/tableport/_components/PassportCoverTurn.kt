package it.mensa.app.features.tableport._components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * The passport's cover, turned open like a book's: it hinges on the left
 * edge and follows the finger, the leather darkening as it leaves the light
 * and its lining showing once it passes upright, while the page underneath
 * ([pages]) catches its shadow. Let go before halfway it falls shut again;
 * past it, or flung, it opens and [onOpened] takes the cover away.
 *
 * The Android counterpart of iOS's page curl (`PassportCurlBook`), which
 * Compose has no equivalent of: the turn is a 3D rotation of the whole sheet.
 */
@Composable
fun PassportCoverTurn(
    onOpened: () -> Unit,
    pages: @Composable () -> Unit,
    cover: @Composable () -> Unit,
    lining: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 0 closed … 180 open, in degrees.
    val angle = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current.density

    fun settle(open: Boolean, velocity: Float = 0f) {
        scope.launch {
            if (open) {
                angle.animateTo(180f, spring(dampingRatio = 1f, stiffness = 120f), initialVelocity = velocity)
                onOpened()
            } else {
                angle.animateTo(0f, spring(dampingRatio = 0.75f, stiffness = 300f), initialVelocity = velocity)
            }
        }
    }

    BoxWithConstraints(modifier) {
        val widthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        Box(
            Modifier
                .fillMaxSize()
                .drawWithContent {
                    drawContent()
                    // The cover's shadow on the page, deepest by the spine and
                    // while the cover hangs over it.
                    val lift = sin(angle.value / 180f * PI).toFloat()
                    val reach = size.width * (1f - angle.value / 180f).coerceAtLeast(0.05f)
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Black.copy(alpha = 0.45f * lift + 0.25f * (1f - angle.value / 180f)), Color.Transparent),
                            endX = reach,
                        ),
                    )
                },
        ) { pages() }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0f, 0.5f)
                    rotationY = -angle.value
                    cameraDistance = 14f * density
                }
                .pointerInput(Unit) {
                    val tracker = VelocityTracker()
                    detectHorizontalDragGestures(
                        onDragStart = { tracker.resetTracking() },
                        onDragEnd = {
                            // Degrees per second: leftward is opening.
                            val v = -tracker.calculateVelocity().x / widthPx * 180f
                            settle(open = angle.value > 60f || v > 240f, velocity = v)
                        },
                        onDragCancel = { settle(open = angle.value > 90f) },
                    ) { change, dragAmount ->
                        tracker.addPosition(change.uptimeMillis, change.position)
                        val next = (angle.value - dragAmount / widthPx * 180f).coerceIn(0f, 180f)
                        scope.launch { angle.snapTo(next) }
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures {
                        scope.launch {
                            angle.animateTo(180f, tween(900, easing = FastOutSlowInEasing))
                            onOpened()
                        }
                    }
                },
        ) {
            val turned = angle.value > 90f
            if (!turned) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .drawWithContent {
                            drawContent()
                            // Turning away from the light.
                            drawRect(Color.Black.copy(alpha = 0.5f * (angle.value / 90f)))
                        },
                ) { cover() }
            } else {
                // The back of the cover, seen through: mirrored so it reads
                // the right way round once turned.
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { scaleX = -1f }
                        .drawWithContent {
                            drawContent()
                            drawRect(Color.Black.copy(alpha = 0.5f * ((180f - angle.value) / 90f)))
                        },
                ) { lining() }
            }
        }
    }
}
