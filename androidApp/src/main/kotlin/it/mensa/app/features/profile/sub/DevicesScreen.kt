package it.mensa.app.features.profile.sub

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhonelinkErase
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.DeviceModel
import org.koin.androidx.compose.koinViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onBack: () -> Unit,
    vm: DevicesViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text(tr("views.devices.title", fallback = "Dispositivi")) },
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
            when {
                uiState.loading && uiState.devices.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                uiState.devices.isEmpty() -> {
                    EmptyDevices(modifier = Modifier.align(Alignment.Center).padding(32.dp))
                }
                else -> {
                    var pendingDelete by remember { mutableStateOf<DeviceModel?>(null) }

                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            // Section header — titleSmall colore primary (SectionHeader eliminato)
                            Text(
                                text = tr("app.devices.section", fallback = "Dispositivi registrati"),
                                style = MaterialTheme.typography.titleSmall,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        items(uiState.devices, key = { it.id }) { device ->
                            DeviceListItem(
                                device = device,
                                onDeleteClick = { pendingDelete = device },
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 72.dp),
                                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                            )
                        }

                        item {
                            Spacer(Modifier.height(32.dp))
                        }
                    }

                    pendingDelete?.let { device ->
                        AlertDialog(
                            onDismissRequest = { pendingDelete = null },
                            title = { Text(tr("app.devices.delete", fallback = "Rimuovi dispositivo")) },
                            text = { Text(tr("app.devices.delete_confirm", fallback = "Rimuovere questo dispositivo?")) },
                            confirmButton = {
                                TextButton(onClick = {
                                    pendingDelete = null
                                    vm.delete(device.id)
                                }) {
                                    Text(
                                        tr("app.devices.delete", fallback = "Rimuovi"),
                                        color = colorScheme.error,
                                    )
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingDelete = null }) {
                                    Text(tr("views.make_donation.cancel", fallback = "Annulla"))
                                }
                            },
                        )
                    }
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
private fun DeviceListItem(
    device: DeviceModel,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ITALIAN)
        .withZone(ZoneId.systemDefault())
    val updated = try {
        fmt.format(java.time.Instant.ofEpochMilli(device.updated.toEpochMilliseconds()))
    } catch (e: Exception) { "—" }

    ListItem(
        modifier = modifier,
        leadingContent = {
            Surface(
                shape = CircleShape,
                color = colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.PhoneAndroid,
                        null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        },
        headlineContent = {
            Text(
                text = device.deviceName.ifEmpty {
                    tr("app.devices.unknown", fallback = "Dispositivo")
                },
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                text = tr("app.devices.last_seen", fallback = "Ultimo accesso") + ": " + updated,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = tr("app.devices.delete", fallback = "Rimuovi"),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
private fun EmptyDevices(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = colorScheme.primaryContainer,
            modifier = Modifier.size(64.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.PhonelinkErase,
                    null,
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            tr("app.devices.empty.title", fallback = "Nessun dispositivo"),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            tr("app.devices.empty.message", fallback = "I tuoi dispositivi registrati appariranno qui."),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
