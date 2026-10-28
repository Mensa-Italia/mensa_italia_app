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
import androidx.compose.material.icons.outlined.Block
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import it.mensa.app.features.notifications.AccountConfirmationHost
import it.mensa.app.features.notifications.NotificationsRoutes
import it.mensa.app.features.notifications.notificationsNavGraph
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.app.ui.root.LogoVariant
import it.mensa.app.ui.root.MensaLogoMark
import it.mensa.app.ui.theme.LocalMensaGradients
import kotlinx.coroutines.launch

/**
 * Account sospeso: il server risponde 403 `account_blocked` a ogni richiesta.
 *
 * Prima non c'era uno stato per questo: ogni schermata mostrava il suo errore
 * generico, e un logout avrebbe solo portato a un nuovo accesso, subito
 * rifiutato allo stesso modo. Qui si resta fermi con un messaggio chiaro.
 * "Riprova" rilegge il socio dal server: se la sospensione e' stata tolta,
 * `AccountStatus` si spegne e la fase torna normale da sola. Stile del muro
 * del rinnovo ([MembershipExpiredScreen]). Mirrors iOS `AccountSuspendedView`.
 */
@Composable
fun AccountSuspendedScreen() {
    // Un NavHost tutto suo: da qui si raggiunge solo l'elenco delle notifiche (e il
    // dettaglio), con le schermate di sempre in modalita' `restricted`. La shell
    // dell'app, con le sue rotte, non c'e'.
    val navController = rememberNavController()
    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = HOME_ROUTE) {
            composable(HOME_ROUTE) {
                SuspendedHome(onOpenNotifications = { navController.navigate(NotificationsRoutes.LIST) })
            }
            notificationsNavGraph(navController, restricted = true)
        }
        // Le conferme delle app esterne restano consentite a un account
        // sospeso (allowlist del server): la push, o un tocco sulla lista,
        // apre il foglio anche qui.
        AccountConfirmationHost(drainPush = true)
    }
}

private const val HOME_ROUTE = "suspended/home"

@Composable
private fun SuspendedHome(onOpenNotifications: () -> Unit) {
    val scope = rememberCoroutineScope()
    val auth = remember { koinAccess().auth }
    val suspended by auth.accountSuspended.collectAsStateWithLifecycle()
    var checking by remember { mutableStateOf(false) }
    var stillSuspended by remember { mutableStateOf(false) }
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
                    Icon(Icons.Outlined.Block, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                text = tr("app.account.suspended.title", "Account sospeso"),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = tr(
                    "app.account.suspended.body",
                    "Il tuo account è stato sospeso e non puoi usare l'app. Per informazioni contatta la segreteria di Mensa Italia.",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            OutlinedButton(
                onClick = {
                    if (checking) return@OutlinedButton
                    checking = true
                    stillSuspended = false
                    scope.launch {
                        // Se la sospensione e' stata tolta, la fase cambia e questa schermata sparisce.
                        stillSuspended = !runCatching { auth.recheckAccount() }.getOrDefault(false)
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
                        text = tr("app.account.suspended.retry", "Riprova"),
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (stillSuspended && suspended) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = tr("app.account.suspended.still", "L'account risulta ancora sospeso."),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Spacer(Modifier.height(12.dp))
            // Le notifiche restano leggibili (allowlist del server): chi si e' perso
            // la richiesta di conferma di un'app esterna la riapre da li'.
            TextButton(onClick = onOpenNotifications) {
                Text(
                    text = tr("app.account.suspended.notifications", "Notifiche"),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
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
