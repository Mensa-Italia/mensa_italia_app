package it.mensa.app.features.profile.sub

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import org.koin.androidx.compose.koinViewModel

private const val RENEW_URL = "https://www.cloud32.it/Associazioni/utenti/richirinnovo"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewMembershipScreen(
    onBack: () -> Unit,
    vm: RenewMembershipViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val user = uiState.user
    val isExpired = vm.isExpired(user)
    val uriHandler = LocalUriHandler.current
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("app.renew.title", fallback = "Tessera")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            // Section header — titleSmall colore primary (SectionHeader eliminato)
            Text(
                text = tr("app.renew.section_title", fallback = "La tua tessera"),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.primary,
                modifier = Modifier.padding(start = 0.dp, end = 8.dp, top = 0.dp, bottom = 8.dp),
            )

            // ── Status Card ──────────────────────────────────────────────────
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isExpired) Icons.Outlined.Warning else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = if (isExpired) colorScheme.error else colorScheme.primary,
                            modifier = Modifier.size(24.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isExpired)
                                tr("app.renew.status_expired", fallback = "Tessera scaduta")
                            else
                                tr("app.renew.status_active", fallback = "Tessera attiva"),
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isExpired) colorScheme.error else colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = tr("app.renew.expiry_label", fallback = "Scadenza").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = vm.expiryString(user),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onSurface,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = tr("app.renew.countdown_label", fallback = "Tempo rimanente").uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = vm.countdownString(user),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isExpired) colorScheme.error else colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Renew CTA ────────────────────────────────────────────────────
            Button(
                onClick = { uriHandler.openUri(RENEW_URL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
            ) {
                Icon(Icons.Outlined.OpenInBrowser, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isExpired)
                        tr("app.renew.cta_now", fallback = "Rinnova ora")
                    else
                        tr("app.renew.cta_early", fallback = "Rinnova in anticipo"),
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Info block (M3 tonal info card, opaque primaryContainer) ─────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = colorScheme.primaryContainer,
                    contentColor = colorScheme.onPrimaryContainer,
                ),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = null,
                            tint = colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = tr("app.renew.info_title", fallback = "Come funziona il rinnovo"),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = tr(
                            "app.renew.info_body",
                            fallback = "Verrai reindirizzato al portale cloud32.it per completare il pagamento della quota associativa annuale. Una volta confermato, la tua tessera verrà aggiornata automaticamente.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}
