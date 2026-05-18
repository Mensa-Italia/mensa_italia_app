package it.mensa.app.features.card._components

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import it.mensa.app.R
import it.mensa.app.ui.theme.GothamBold
import it.mensa.app.ui.theme.MensaBlue
import it.mensa.app.ui.theme.MensaMotion
import kotlinx.coroutines.launch

private const val CARD_ASPECT = 1.586f
private const val REFERENCE_WIDTH = 343f

@Composable
fun MembershipCardHero(
    fullName: String,
    memberId: String,
    modifier: Modifier = Modifier,
) {
    val flipRotation = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.85f) }
    var flipped by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(1f, animationSpec = MensaMotion.springHeroOvershoot)
    }

    // Subtle floating effect — always on, low amplitude so the card looks alive
    // even without a working gyroscope.
    val floatTransition = rememberInfiniteTransition(label = "tessera_float")
    val floatX by floatTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tessera_float_rotX",
    )
    val floatY by floatTransition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tessera_float_rotY",
    )
    // Vertical drift — the "lift" component of the float.
    val floatLift by floatTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "tessera_float_lift",
    )

    // Optional gyroscope-driven tilt. No-op if sensor unavailable or fails.
    val tilt by rememberDeviceTilt()
    val tiltX = tilt.y * 8f // pitch → rotationX degrees
    val tiltY = tilt.x * 10f // roll → rotationY degrees

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(CARD_ASPECT),
    ) {
        val cardWidthDp = maxWidth
        val u = cardWidthDp.value / REFERENCE_WIDTH
        val cornerDp = (20f * u).dp
        val cardShape = RoundedCornerShape(cornerDp)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    // Single graphicsLayer = single 3D space. Shadow + clip + rotation
                    // all transform together, so the silhouette breathes with the card
                    // instead of looking like a static frame.
                    val s = scaleAnim.value
                    scaleX = s
                    scaleY = s
                    rotationY = flipRotation.value + tiltY + floatY
                    rotationX = tiltX + floatX
                    translationY = (floatLift * 6f).dp.toPx()
                    cameraDistance = 16f * density
                    shape = cardShape
                    clip = true
                    shadowElevation = (12f * u).dp.toPx()
                    ambientShadowColor = Color.Black.copy(alpha = 0.22f)
                    spotShadowColor = Color.Black.copy(alpha = 0.28f)
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    scope.launch {
                        scaleAnim.animateTo(
                            0.97f,
                            animationSpec = spring(stiffness = 800f, dampingRatio = 0.6f),
                        )
                        scaleAnim.animateTo(1f, animationSpec = MensaMotion.springHero)
                    }
                    scope.launch {
                        val target = if (flipped) 0f else 180f
                        flipRotation.animateTo(
                            target,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = Spring.StiffnessLow,
                            ),
                        )
                        flipped = !flipped
                    }
                },
        ) {
            val angle = ((flipRotation.value % 360f) + 360f) % 360f
            val showFront = angle < 90f || angle > 270f

            if (showFront) {
                CardFrontFace(u = u)
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f },
                ) {
                    CardBackFace(
                        fullName = fullName,
                        memberId = memberId,
                        u = u,
                    )
                }
            }

            // Border overlay
            val borderWidthPx = with(LocalDensity.current) { maxOf(0.5f, 1f * u).dp.toPx() }
            val cornerPx = with(LocalDensity.current) { cornerDp.toPx() }
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.45f),
                            Color.White.copy(alpha = 0.05f),
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width, size.height),
                    ),
                    cornerRadius = CornerRadius(cornerPx),
                    style = Stroke(width = borderWidthPx),
                )
            }
        }
    }
}

// ── Front face: solid Mensa Blue, centered logo, chevron bottom-right ────────

@Composable
private fun CardFrontFace(u: Float) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MensaBlue),
    ) {
        // Centered logo (white tint on blue background)
        Image(
            painter = painterResource(R.drawable.tessera_logo_mark),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.33f),
        )

        // Diagonal shine overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        start = Offset.Zero,
                        end = Offset.Infinite,
                    ),
                ),
        )

        // Chevron bottom-right
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = (14f * u).dp,
                    bottom = (12f * u).dp,
                )
                .size((18f * u).dp),
        )
    }
}

// ── Back face: background image + member data, pixel-locked layout ───────────

@Composable
private fun CardBackFace(
    fullName: String,
    memberId: String,
    u: Float,
) {
    val nameParts = remember(fullName) {
        val trimmed = fullName.trim()
        val spaceIdx = trimmed.indexOf(' ')
        if (spaceIdx < 0) listOf(trimmed)
        else {
            val first = trimmed.substring(0, spaceIdx).trim()
            val rest = trimmed.substring(spaceIdx + 1).trim()
            if (rest.isEmpty()) listOf(first) else listOf(first, rest)
        }
    }

    val paddingDp = (20f * u).dp
    val nameFontSize = (20f * u).sp
    val labelFontSize = (14f * u).sp
    val idFontSize = (20f * u).sp
    val mensaItFontSize = (14f * u).sp

    Box(modifier = Modifier.fillMaxSize().background(MensaBlue)) {
        // Background image
        Image(
            painter = painterResource(R.drawable.tessera_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // White wash
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.30f)),
        )

        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingDp),
        ) {
            // Top: lettering (2/3 width)
            Image(
                painter = painterResource(R.drawable.tessera_lettering),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth(2f / 3f),
            )

            Spacer(Modifier.weight(1f))

            // Name band
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy((2f * u).dp),
            ) {
                nameParts.forEach { part ->
                    Text(
                        text = part.uppercase(),
                        fontFamily = GothamBold,
                        fontSize = nameFontSize,
                        color = Color.Black,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }

            Spacer(Modifier.height((6f * u).dp))

            // "Tessera" label
            Text(
                text = "Tessera",
                fontFamily = GothamBold,
                fontSize = labelFontSize,
                color = Color.Black,
                maxLines = 1,
            )

            Spacer(Modifier.height((4f * u).dp))

            // Footer: member ID + MENSA.IT
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = memberId,
                    fontFamily = GothamBold,
                    fontSize = idFontSize,
                    color = Color.Black,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "MENSA.IT",
                    fontFamily = GothamBold,
                    fontSize = mensaItFontSize,
                    color = Color.Black,
                    maxLines = 1,
                )
            }
        }
    }
}

// ── Gyroscope tilt helper ────────────────────────────────────────────────────
//
// Returns an Offset where:
//  - x ∈ [-1, 1] roll (phone tilted left/right around its long axis)
//  - y ∈ [-1, 1] pitch (phone tilted forward/back)
//
// Stays at Offset.Zero (no-op) if the sensor is missing or anything throws —
// the floating animation alone keeps the card alive in that case.
@Composable
private fun rememberDeviceTilt(): State<Offset> {
    val context = LocalContext.current
    val tilt = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sm?.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR)
            ?: sm?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        if (sm == null || sensor == null) {
            return@DisposableEffect onDispose {}
        }

        val rotationMatrix = FloatArray(9)
        val orientation = FloatArray(3)
        var smoothedX = 0f
        var smoothedY = 0f
        val alpha = 0.15f // low-pass smoothing — avoids jitter

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                try {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientation)
                    // orientation[1] = pitch (radians), orientation[2] = roll (radians)
                    val pitchNorm = (orientation[1] / 0.6f).coerceIn(-1f, 1f)
                    val rollNorm = (orientation[2] / 0.6f).coerceIn(-1f, 1f)
                    smoothedX = smoothedX + alpha * (rollNorm - smoothedX)
                    smoothedY = smoothedY + alpha * (pitchNorm - smoothedY)
                    tilt.value = Offset(smoothedX, smoothedY)
                } catch (_: Throwable) {
                    // Gracefully ignore — sensor may report bad data on some devices.
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        try {
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        } catch (_: Throwable) {
            // Some OEMs throw on registerListener — fall back silently.
        }

        onDispose {
            try { sm.unregisterListener(listener) } catch (_: Throwable) {}
        }
    }

    return tilt
}
