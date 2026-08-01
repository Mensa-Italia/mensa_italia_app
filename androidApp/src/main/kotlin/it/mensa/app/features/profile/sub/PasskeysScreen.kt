package it.mensa.app.features.profile.sub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.auth.passkey.PasskeyModel
import org.koin.androidx.compose.koinViewModel

/**
 * Gestione delle passkey. E' anche l'unica strada per chi ha rifiutato la
 * proposta automatica dopo il login, e per chi cambia telefono.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasskeysScreen(
    onBack: () -> Unit,
    vm: PasskeysViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var pendingDelete by remember { mutableStateOf<PasskeyModel?>(null) }

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("views.passkeys.title", fallback = "Passkey")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        when {
            uiState.loading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            !uiState.supported -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = tr(
                        "views.passkeys.unsupported",
                        fallback = "Questo dispositivo non supporta le passkey. Puoi comunque accedere con la password.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            // Suffisso `.android`: vedi PasskeyEnrollmentPrompt,
                            // iOS usa `.ios` perché nomina Face ID / Touch ID.
                            // Il commento sta fuori da `tr(` perché
                            // l'estrattore di tools/tolgee-push.sh accetta solo
                            // whitespace tra `tr(` e la chiave.
                            text = tr(
                                "views.passkeys.explainer.android",
                                fallback = "Con una passkey entri con l'impronta o il volto, senza digitare la password. La password resta sempre valida.",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { vm.add(context) },
                            enabled = !uiState.adding,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                        ) {
                            if (uiState.adding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Text(
                                    text = tr(
                                        "views.passkeys.add",
                                        fallback = "Attiva su questo dispositivo",
                                    ),
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                            }
                        }

                        uiState.message?.let { message ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                }

                if (uiState.passkeys.isEmpty()) {
                    item {
                        Text(
                            text = tr(
                                "views.passkeys.empty",
                                fallback = "Nessuna passkey attiva.",
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                } else {
                    items(uiState.passkeys, key = { it.id }) { passkey ->
                        ListItem(
                            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
                            leadingContent = {
                                Icon(Icons.Outlined.Fingerprint, contentDescription = null)
                            },
                            headlineContent = { Text(passkey.name.ifBlank { "Passkey" }) },
                            supportingContent = if (passkey.ready) null else {
                                {
                                    Text(
                                        tr(
                                            "views.passkeys.not_ready",
                                            fallback = "Attivazione non completata",
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(onClick = { pendingDelete = passkey }) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null)
                                }
                            },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    pendingDelete?.let { passkey ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(tr("views.passkeys.remove_title", fallback = "Rimuovere la passkey?")) },
            text = {
                Text(
                    // Non e' un lockout e non va drammatizzato: la password resta
                    // sempre un percorso valido.
                    tr(
                        "views.passkeys.remove_body",
                        fallback = "Potrai ancora accedere con la password, e riattivarla quando vuoi.",
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.delete(passkey.id)
                    pendingDelete = null
                }) {
                    Text(tr("common.remove", fallback = "Rimuovi"))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(tr("common.cancel", fallback = "Annulla"))
                }
            },
        )
    }
}
