package it.mensa.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import it.mensa.app.navigation.MensaNavGraph
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.ui.theme.MensaTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val audioPlayerController: AudioPlayerController by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Edge-to-edge: Compose draws behind system bars
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // enableEdgeToEdge() already handles bar colors on API 29+.
        // The deprecated setters below are kept only for API 26-28 compatibility.
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        @Suppress("DEPRECATION")
        window.navigationBarColor = Color.TRANSPARENT

        setContent {
            MensaTheme {
                // Set default system bar icon appearance based on theme
                // Content screens (light bg) want dark icons; dark-themed screens override locally
                val isDark = isSystemInDarkTheme()
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        WindowCompat.getInsetsController(window, view).let { ctrl ->
                            // Light-mode: dark icons on Parchment background
                            // Dark-mode: light icons on BackdropDark background
                            ctrl.isAppearanceLightStatusBars = !isDark
                            ctrl.isAppearanceLightNavigationBars = !isDark
                        }
                    }
                }
                MensaApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        audioPlayerController.bind(this)
    }

    override fun onStop() {
        super.onStop()
        audioPlayerController.unbind()
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        intent.data?.let { uri ->
            if (uri.scheme == "mensa") {
                // TODO: inject StripeService and call handleDeepLink(uri)
            }
        }
    }
}

@Composable
fun MensaApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        MensaNavGraph()
    }
}
