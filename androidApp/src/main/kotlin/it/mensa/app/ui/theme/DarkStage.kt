package it.mensa.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** The deep blue room the ranking and the medals are set in. */
val StageBackdrop = Brush.verticalGradient(
    listOf(Color(0xFF161B2C), Color(0xFF0B0E18), Color(0xFF05060A)),
)

/** Sheets and bars over the stage: its darkest blue, opaque. */
val StageSurface = Color(0xFF0E111B)

/** The warm light falling from the stage's ceiling. */
val StageLight = Color(0xFFFFF2D1)

/**
 * A page that is always dark, whatever the app's theme: the dark colour
 * scheme for its components, light status-bar icons while it shows (put back
 * as they were when it goes), the stage's backdrop with a ceiling light.
 * Mirrors iOS `darkStage()`.
 */
@Composable
fun DarkStage(content: @Composable () -> Unit) {
    MensaTheme(darkTheme = true) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            DisposableEffect(view) {
                val window = (view.context as? Activity)?.window
                val bars = window?.let { WindowCompat.getInsetsController(it, view) }
                val lightStatus = bars?.isAppearanceLightStatusBars
                val lightNavigation = bars?.isAppearanceLightNavigationBars
                bars?.isAppearanceLightStatusBars = false
                bars?.isAppearanceLightNavigationBars = false
                onDispose {
                    if (bars != null && lightStatus != null && lightNavigation != null) {
                        bars.isAppearanceLightStatusBars = lightStatus
                        bars.isAppearanceLightNavigationBars = lightNavigation
                    }
                }
            }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(StageBackdrop)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            listOf(StageLight.copy(alpha = 0.08f), Color.Transparent),
                            center = Offset(size.width / 2f, 0f),
                            radius = 560.dp.toPx(),
                        ),
                    )
                },
        ) {
            // Transparent containers on the stage pass no content colour of
            // their own: text and icons take the dark scheme's.
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
                content()
            }
        }
    }
}
