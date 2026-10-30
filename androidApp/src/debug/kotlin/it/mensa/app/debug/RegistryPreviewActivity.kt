package it.mensa.app.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import it.mensa.app.features.members.MembersDirectoryScreen
import it.mensa.app.ui.theme.MensaTheme
import it.mensa.shared.members.RegSociFixtures

/**
 * Il registro soci su [RegSociFixtures], senza login: per provarne ricerca e
 * filtro per regione su un emulatore qualsiasi.
 *
 * ```
 * adb shell am start -n it.mensa.app/it.mensa.app.debug.RegistryPreviewActivity
 * ```
 *
 * Esiste solo nella build debug (`src/debug`). Il refresh scrive i soci
 * inventati nel database dell'app al posto di quelli in cache: il primo
 * refresh dopo un login vero li rimette.
 */
class RegistryPreviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Prima del ViewModel: il suo primo refresh deve gia' leggere i soci inventati.
        RegSociFixtures.enabled = true
        enableEdgeToEdge()
        setContent {
            MensaTheme {
                MembersDirectoryScreen(onBack = ::finish)
            }
        }
    }
}
