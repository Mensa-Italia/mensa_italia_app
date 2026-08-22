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
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Warning
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.AppFormat
import it.mensa.app.support.koinAccess
import it.mensa.app.support.rememberAppLocale
import it.mensa.app.support.tr
import it.mensa.app.ui.components.PrimaryButton
import it.mensa.app.ui.root.LogoVariant
import it.mensa.app.ui.root.MensaLogoMark
import it.mensa.app.ui.theme.LocalMensaGradients
import it.mensa.shared.auth.Membership
import kotlinx.coroutines.launch

/**
 * Muro del rinnovo.
 *
 * Con la tessera scaduta l'app si apriva normalmente e la scadenza si vedeva
 * solo entrando in Profilo → Tessera. Adesso questa e' l'unica schermata
 * disponibile: si rinnova sul portale, si ricontrolla, oppure si esce.
 *
 * Non e' una pagina dentro l'app: e' una fase di
 * [it.mensa.app.ui.root.RootPhase], quindi non ha back, non ha bottom bar e non
 * c'e' nessuna scorciatoia da cui rientrare.
 */
@Composable
fun MembershipExpiredScreen() {
    val uriHandler = LocalUriHandler.current
    val locale = rememberAppLocale()
    val scope = rememberCoroutineScope()
    val auth = remember { koinAccess().auth }
    val user by auth.currentUser.collectAsStateWithLifecycle()
    var checking by remember { mutableStateOf(false) }
    var loggingOut by remember { mutableStateOf(false) }

    val expiryText = remember(user, locale) {
        val instant = user?.expireMembership
        if (instant == null || instant.toEpochMilliseconds() <= 0L) {
            "—"
        } else {
            runCatching { AppFormat.format(instant, AppFormat.Skeleton.DAY_MONTH_YEAR, locale) }
                .getOrDefault("—")
        }
    }
    val daysOverdue = remember(user) { Membership.daysUntilExpiry(user)?.let { -it } }

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

            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(64.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Text(
                text = tr("app.renew.gate.title", fallback = "La tua tessera è scaduta"),
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = tr(
                    "app.renew.gate.body",
                    fallback = "Rinnova l'iscrizione per tornare a usare l'app. Il rinnovo " +
                        "si completa sul portale dei soci: appena risulta registrato, " +
                        "tocca «Ho rinnovato».",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))

            Surface(
                shape = MaterialTheme.shapes.large,
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = tr("app.renew.expiry_label", fallback = "Scadenza").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = expiryText,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    if (daysOverdue != null && daysOverdue > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = tr(
                                "app.renew.gate.overdue",
                                fallback = "Scaduta da {days} giorni",
                                "days" to daysOverdue,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))

            PrimaryButton(
                text = tr("app.renew.cta_now", fallback = "Rinnova ora"),
                onClick = { uriHandler.openUri(Membership.RENEWAL_URL) },
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Outlined.OpenInBrowser,
            )
            Spacer(Modifier.height(12.dp))

            // Outlined bianco a mano invece di SecondaryButton: quello tinge
            // bordo e testo di `colorScheme.primary`, cioe' blu su blu.
            OutlinedButton(
                onClick = {
                    if (checking) return@OutlinedButton
                    checking = true
                    scope.launch {
                        runCatching { auth.refreshCurrentUser() }
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
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White,
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = if (checking) {
                            tr("app.renew.gate.checking", fallback = "Controllo in corso…")
                        } else {
                            tr("app.renew.gate.recheck", fallback = "Ho rinnovato")
                        },
                        color = Color.White,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
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
                    text = tr("views.settings.tile.logout.title", fallback = "Esci"),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
