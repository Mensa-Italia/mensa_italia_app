package it.mensa.app

import android.content.Intent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import it.mensa.app.navigation.MensaNavGraph
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.services.push.DeviceRegistrar
import it.mensa.app.services.push.PendingPushTarget
import it.mensa.app.services.push.PushDeepLinkRouter
import it.mensa.app.support.LaunchHarness
import it.mensa.app.support.koinAccess
import it.mensa.app.support.ThemeManager
import it.mensa.app.support.ThemeMode
import it.mensa.app.ui.theme.MensaTheme
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val audioPlayerController: AudioPlayerController by inject()
    private val themeManager: ThemeManager by inject()
    private val deviceRegistrar: DeviceRegistrar by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Automation hooks (screenshot capture): reads `mensa_screen` /
        // `mensa_autologin_*` extras. Inert on non-debuggable builds.
        // Must run before setContent so RootViewModel sees it on first compose.
        LaunchHarness.configure(this, intent)

        // Cold-launch da una push: il sistema apre questa Activity con il
        // payload FCM negli extra. Il bersaglio si parcheggia in
        // PendingPushTarget e lo drena MainAppShell quando il NavController
        // esiste — qui non esiste ancora.
        capturePushTarget(intent)

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
            // Preferenza utente (Profilo → Tema) sopra il tema di sistema.
            val themeMode by themeManager.mode.collectAsState()
            val isDark = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MensaTheme(darkTheme = isDark) {
                // Set default system bar icon appearance based on theme
                // Content screens (light bg) want dark icons; dark-themed screens override locally
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

    override fun onResume() {
        super.onResume()
        // Rilegge il socio da `/api/cs/me` a ogni ritorno in primo piano.
        //
        // `AuthRepository.init()` lo fa solo all'avvio a freddo: un'app rimasta
        // in background per giorni, o un `/me` fallito una volta perche'
        // offline, continuava a mostrare scadenza tessera, `powers` e `addons`
        // di allora. Da qui arriva anche la riapertura del gate del rinnovo,
        // che e' derivato da `auth.currentUser`.
        //
        // Non serve rifare il login: quei campi li scrive il server sul record,
        // non le credenziali.
        lifecycleScope.launch {
            runCatching { koinAccess().auth.refreshCurrentUser() }
            // Stesso momento buono per riverificare che questo telefono
            // risulti ancora fra i dispositivi registrati (vedi
            // `DeviceRegistrar`): se e' stato cancellato dal server, si
            // ripubblica adesso.
            runCatching { deviceRegistrar.ensureRegistered() }
        }
    }

    override fun onStop() {
        super.onStop()
        audioPlayerController.unbind()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Senza questo, `getIntent()` continuerebbe a restituire quello di
        // partenza e un secondo tap non si vedrebbe.
        setIntent(intent)
        capturePushTarget(intent)
    }

    /**
     * Legge il payload FCM dagli extra e lo mette in [PendingPushTarget].
     *
     * FCM consegna il `data` payload come extra di stringhe sull'Intent che
     * apre l'app, sia quando la notifica l'ha disegnata il sistema (app in
     * background) sia quando l'abbiamo disegnata noi in
     * [it.mensa.app.services.push.MensaMessagingService]. Gli extra non-stringa
     * (flag interni del sistema) escono null da `getString` e vengono scartati.
     *
     * Un payload che [PushDeepLinkRouter] non sa leggere non viene parcheggiato:
     * meglio aprire l'app dov'era che sbatterla su una schermata a caso.
     */
    private fun capturePushTarget(intent: Intent?) {
        // `runCatching` perche' questa gira in onCreate a ogni avvio, anche
        // quando di push non ce n'e' nessuna: un Bundle malformato — o messo
        // li' da un'app terza che apre la nostra Activity — non deve poter
        // impedire l'apertura dell'app. `getExtras` puo' lanciare se il Bundle
        // contiene una Parcelable che non sappiamo deserializzare.
        runCatching {
            val extras = intent?.extras ?: return@runCatching
            val data = extras.keySet()
                .mapNotNull { key -> extras.getString(key)?.let { key to it } }
                .toMap()
            if (data.isEmpty()) return@runCatching

            val target = PushDeepLinkRouter.parse(data)
            if (target !is PushDeepLinkRouter.NotificationTarget.Unknown) {
                PendingPushTarget.set(target)
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
