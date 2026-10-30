package it.mensa.app.features.leaderboard

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import it.mensa.app.support.tr
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * The ink of each podium place, light enough to read on the card's blue:
 * gold, silver, bronze. Same values on iOS.
 */
internal fun stampInk(place: Int): Color = when (place) {
    1 -> Color(0xFFF2C452)
    2 -> Color(0xFFDDE3EC)
    else -> Color(0xFFE39A68)
}

/**
 * Where the stamp lands on the card's front: drawn once per launch, so it
 * sits still while the member looks and lands somewhere else next time, as
 * a real rubber stamp would. Fractions of the card, left or right of the
 * logo, clear of the edges and of the chevron.
 */
private object StampPlacement {
    private val left: Boolean = Random.nextBoolean()
    val x: Float = if (left) 0.17f + Random.nextFloat() * 0.06f else 0.77f + Random.nextFloat() * 0.05f
    val y: Float = 0.27f + Random.nextFloat() * (if (left) 0.44f else 0.30f)
    val rotation: Float = -26f + Random.nextFloat() * 52f
}

/**
 * The podium stamp on the front of the membership card, for the first three
 * of the ranking: pressed on at a random spot and angle, with a thump each
 * time that side comes into view. Sized as a share of the card, so it
 * scales with it.
 */
@Composable
internal fun PodiumStampOverlay(place: Int, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        val side = maxWidth * 0.31f
        val press = remember { Animatable(0f) }
        LaunchedEffect(place) {
            // A beat after the side shows: then the stamp comes down.
            delay(180)
            press.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 520f))
        }
        val fade = remember { Animatable(0f) }
        LaunchedEffect(place) {
            delay(180)
            fade.animateTo(1f, tween(140))
        }
        Box(
            Modifier
                .offset(
                    x = maxWidth * StampPlacement.x - side / 2,
                    y = maxHeight * StampPlacement.y - side / 2,
                )
                .size(side)
                .graphicsLayer {
                    val s = 1.7f - 0.7f * press.value
                    scaleX = s
                    scaleY = s
                    alpha = fade.value
                    rotationZ = StampPlacement.rotation
                },
        ) {
            PodiumStamp(place = place, size = side)
        }
    }
}

/** A round rubber stamp: two rings, the words around, the place in the middle. */
@Composable
internal fun PodiumStamp(place: Int, size: Dp, modifier: Modifier = Modifier) {
    val ink = stampInk(place)
    val top = tr("leaderboard.stamp.top", "Classifica soci").uppercase()
    val bottom = tr("leaderboard.stamp.bottom", "Mensa Italia").uppercase()
    val year = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year }
    Canvas(
        modifier
            .size(size)
            // Its own layer, so the worn spots can punch through the ink only.
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = 0.86f
            },
    ) {
        val r = this.size.minDimension / 2f
        val c = center
        drawCircle(ink, radius = r * 0.95f, center = c, style = Stroke(width = r * 0.075f))
        drawCircle(ink, radius = r * 0.66f, center = c, style = Stroke(width = r * 0.03f))

        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ink.toArgb()
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                letterSpacing = 0.12f
                textSize = r * 0.17f
            }
            // The words around: over the top left to right, and under the
            // bottom left to right too, so neither reads upside down.
            val band = r * 0.80f
            val capHalf = paint.textSize * 0.36f
            fun arc(radius: Float, start: Float, sweep: Float) = android.graphics.Path().apply {
                addArc(c.x - radius, c.y - radius, c.x + radius, c.y + radius, start, sweep)
            }
            val topPath = arc(band - capHalf, 180f, 180f)
            val topLength = (PI * (band - capHalf)).toFloat()
            native.drawTextOnPath(top, topPath, (topLength - paint.measureText(top)) / 2f, 0f, paint)
            val bottomPath = arc(band + capHalf, 180f, -180f)
            val bottomLength = (PI * (band + capHalf)).toFloat()
            native.drawTextOnPath(bottom, bottomPath, (bottomLength - paint.measureText(bottom)) / 2f, 0f, paint)

            // Stars between the two lines of words.
            paint.letterSpacing = 0f
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = r * 0.16f
            listOf(-1f, 1f).forEach { side ->
                native.drawText("★", c.x + side * band, c.y + paint.textSize * 0.36f, paint)
            }

            // The place, big, and the year under it.
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.textSize = r * 0.62f
            native.drawText("$place°", c.x + r * 0.06f, c.y + r * 0.16f, paint)
            paint.textSize = r * 0.15f
            paint.letterSpacing = 0.2f
            native.drawText("$year", c.x, c.y + r * 0.44f, paint)
        }

        // Worn ink: spots where the rubber didn't touch, always the same for a place.
        val wear = Random(place * 7919)
        repeat(170) {
            val a = wear.nextFloat() * 2f * PI.toFloat()
            val d = sqrt(wear.nextFloat()) * r
            drawCircle(
                Color.Black,
                radius = r * (0.006f + wear.nextFloat() * 0.022f),
                center = Offset(c.x + cos(a) * d, c.y + sin(a) * d),
                blendMode = BlendMode.Clear,
            )
        }
        // A lighter streak, where the stamp was pressed unevenly.
        val streak = wear.nextFloat() * 360f
        drawArc(
            Color.Black.copy(alpha = 0.35f),
            startAngle = streak,
            sweepAngle = 70f,
            useCenter = true,
            topLeft = Offset(c.x - r, c.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            blendMode = BlendMode.DstOut,
        )
    }
}
