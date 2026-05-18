package it.mensa.app.features.notifications

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import org.koin.androidx.compose.koinViewModel

/**
 * NotificationManagerScreen — mirrors iOS NotificationManagerView.
 *
 * Toggles: notify_events, notify_messages, notify_general.
 * Region picker: notify_me_events (Tolgee key "notify_me_events").
 * Uses MetadataRepository for persist/load (same as iOS koin.metadata).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationManagerScreen(
    onBack: () -> Unit,
    vm: NotificationManagerViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    MensaScaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(tr("notifications.manager.title", fallback = "Preferenze notifiche"))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Indietro",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0f),
                ),
            )
        },
    ) { innerPadding ->
        if (uiState.loading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
            }
            return@MensaScaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Section: Tipi di notifica
            Text(
                text = tr("notifications.manager.section_types", fallback = "Tipi di notifica"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
            )

            ListItem(
                headlineContent = {
                    Text(tr("notifications.manager.events", fallback = "Eventi"))
                },
                trailingContent = {
                    Switch(
                        checked = uiState.notifyEvents,
                        onCheckedChange = { vm.setNotifyEvents(it) },
                    )
                },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            ListItem(
                headlineContent = {
                    Text(tr("notifications.manager.messages", fallback = "Messaggi"))
                },
                trailingContent = {
                    Switch(
                        checked = uiState.notifyMessages,
                        onCheckedChange = { vm.setNotifyMessages(it) },
                    )
                },
            )
            HorizontalDivider(modifier = Modifier.padding(start = 16.dp))

            ListItem(
                headlineContent = {
                    Text(tr("notifications.manager.general", fallback = "Generali"))
                },
                trailingContent = {
                    Switch(
                        checked = uiState.notifyGeneral,
                        onCheckedChange = { vm.setNotifyGeneral(it) },
                    )
                },
            )

            // Section: Regioni eventi (multi-select, matches Flutter)
            Text(
                text = tr("notifications.manager.section_region", fallback = "Regioni eventi"),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 4.dp),
            )

            Text(
                text = tr(
                    "notifications.manager.region_hint",
                    fallback = "Ricevi notifiche per eventi nelle regioni selezionate.",
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 8.dp),
            )

            italianRegions.forEachIndexed { index, region ->
                val enabled = region in uiState.selectedRegions
                ListItem(
                    headlineContent = { Text(region) },
                    trailingContent = {
                        Switch(
                            checked = enabled,
                            onCheckedChange = { vm.toggleRegion(region, it) },
                        )
                    },
                )
                if (index < italianRegions.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}
