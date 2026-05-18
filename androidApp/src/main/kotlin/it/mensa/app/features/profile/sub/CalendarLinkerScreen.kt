package it.mensa.app.features.profile.sub

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarLinkerScreen(
    onBack: () -> Unit,
    vm: CalendarLinkerViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("app.calendar_link.title", fallback = "Calendario")) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (uiState.loading && uiState.link == null) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ── Hero icon ─────────────────────────────────────────────
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                null,
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))

                    // Section header — titleSmall colore primary (SectionHeader eliminato)
                    Text(
                        text = tr("app.calendar_link.headline", fallback = "Sincronizza calendario"),
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )

                    Text(
                        tr(
                            "app.calendar_link.subhead",
                            fallback = "Aggiungi automaticamente eventi Mensa al tuo calendario preferito tramite un link iCal.",
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(22.dp))

                    val link = uiState.link
                    if (link != null) {
                        // ── Add to Calendar Button ─────────────────────────
                        Button(
                            onClick = {
                                val uri = Uri.parse(vm.webcalUrl(link))
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 56.dp),
                        ) {
                            Icon(Icons.Outlined.CalendarMonth, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tr("app.calendar_link.add_to_calendar", fallback = "Aggiungi al calendario"))
                        }

                        Spacer(Modifier.height(16.dp))

                        // ── Current link card ──────────────────────────────
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    tr("app.calendar_link.your_link", fallback = "Il tuo link iCal").uppercase(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    vm.httpsUrl(link),
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    maxLines = 2,
                                    color = colorScheme.onSurface,
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("ical_link", vm.httpsUrl(link)))
                                            vm.onCopied()
                                        },
                                    ) {
                                        Icon(
                                            if (uiState.copied) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            if (uiState.copied)
                                                tr("app.calendar_link.copied", fallback = "Copiato!")
                                            else
                                                tr("app.calendar_link.copy", fallback = "Copia"),
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, vm.httpsUrl(link))
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, null))
                                        },
                                    ) {
                                        Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text(tr("app.calendar_link.share", fallback = "Condividi"))
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(22.dp))

                        // ── Regions section ────────────────────────────────
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                tr("app.calendar_link.regions_section", fallback = "Regioni"),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            )
                            Spacer(Modifier.height(10.dp))

                            RegionToggleRow(
                                label = tr("app.calendar_link.national_events", fallback = "Eventi nazionali"),
                                subtitle = tr("app.calendar_link.not_editable", fallback = "Non modificabile"),
                                checked = true,
                                enabled = false,
                                onToggle = {},
                            )
                            HorizontalDivider()

                            vm.availableRegions.forEach { region ->
                                val isOn = link.state.any { it.equals(region, ignoreCase = true) }
                                RegionToggleRow(
                                    label = region,
                                    checked = isOn,
                                    onToggle = { vm.toggleRegion(region) },
                                )
                                if (region != vm.availableRegions.last()) {
                                    HorizontalDivider()
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))
                        Text(
                            tr(
                                "app.calendar_link.sync_notice",
                                fallback = "Gli aggiornamenti verranno applicati al tuo calendario ma potrebbero richiedere del tempo.",
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        // Empty state
                        Spacer(Modifier.height(16.dp))
                        Text(
                            tr("app.calendar_link.empty", fallback = "Nessun calendario configurato"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            tr("app.calendar_link.empty_message", fallback = "Il link al calendario verrà generato automaticamente."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }

    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::dismissError,
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun RegionToggleRow(
    label: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            enabled = enabled,
        )
    }
}
