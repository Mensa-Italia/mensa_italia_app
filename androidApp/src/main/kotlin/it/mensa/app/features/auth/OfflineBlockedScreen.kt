package it.mensa.app.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.AppFormat
import it.mensa.app.support.koinAccess
import it.mensa.app.support.rememberAppLocale
import it.mensa.app.support.tr
import it.mensa.app.ui.root.LogoVariant
import it.mensa.app.ui.root.MensaLogoMark
import it.mensa.app.ui.theme.LocalMensaGradients
import kotlinx.coroutines.launch

/**
 * Offline da piu' di un giorno: per continuare serve collegarsi.
 *
 * L'app parte dalla cache e offline mostra i dati salvati, ma non per sempre:
 * tessera, timbri e dati devono essere riverificati dal server almeno una
 * volta ogni 24 ore (vedi `Connectivity`). La schermata se ne va da sola
 * appena il server risponde — anche senza toccare "Riprova", il controllo
 * gira in sottofondo. Stile del muro del rinnovo ([MembershipExpiredScreen]).
 * Mirrors iOS `OfflineBlockedView`.
 */
@Composable
fun OfflineBlockedScreen() {
    val scope = rememberCoroutineScope()
    val locale = rememberAppLocale()
    val connectivity = remember { koinAccess().connectivity }
    val auth = remember { koinAccess().auth }
    val lastContact by connectivity.lastContact.collectAsStateWithLifecycle()
    var checking by remember { mutableStateOf(false) }
    var stillOffline by remember { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = LocalMensaGradients.current.brandDiagonal),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MensaLogoMark(size = 88.dp, variant = LogoVariant.Solid)
            Spacer(Modifier.height(28.dp))
            Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.18f), modifier = Modifier.size(64.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = tr("app.offline.blocked.title", "Collegati a internet"),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = tr(
                    "app.offline.blocked.body",
                    "L'app è offline da più di un giorno. Per continuare serve un collegamento: " +
                        "tessera e dati vanno verificati almeno una volta ogni 24 ore.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
            )
            lastContact?.let { last ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = tr(
                        "app.offline.blocked.last_contact",
                        "Ultimo collegamento: {date}",
                        "date" to AppFormat.format(last, AppFormat.Skeleton.MEDIUM_WITH_TIME, locale),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(28.dp))
            OutlinedButton(
                onClick = {
                    if (checking) return@OutlinedButton
                    checking = true
                    stillOffline = false
                    scope.launch {
                        // Se risponde, la fase cambia e questa schermata sparisce.
                        stillOffline = !runCatching { connectivity.probeNow() }.getOrDefault(false)
                        checking = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 16.dp),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.9f)),
                enabled = !checking,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (checking) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = tr("app.offline.blocked.retry", "Riprova"),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (stillOffline) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = tr("app.offline.blocked.still_offline", "Ancora nessuna connessione."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.height(16.dp))
            TextButton(
                onClick = {
                    if (loggingOut) return@TextButton
                    loggingOut = true
                    scope.launch { runCatching { auth.logout() } }
                },
            ) {
                Text(
                    text = tr("views.settings.tile.logout.title", "Esci"),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
