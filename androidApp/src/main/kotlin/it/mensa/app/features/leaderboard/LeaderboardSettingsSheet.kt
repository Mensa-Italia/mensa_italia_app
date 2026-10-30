package it.mensa.app.features.leaderboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import kotlinx.coroutines.launch

/**
 * The ranking's settings, from the gear beside ⓘ: one switch, to leave the
 * members' ranking. Saved in `users_metadata` through the repository; the
 * view then skips the member. Mirrors iOS `LeaderboardSettingsSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LeaderboardSettingsSheet(onDismiss: () -> Unit) {
    val repository = remember { koinAccess().leaderboard }
    val hidden by repository.observeHidden().collectAsState(initial = false)
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(repository) { runCatching { repository.refreshHidden() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = tr("leaderboard.settings", "Impostazioni"),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Spacer(Modifier.height(8.dp))
            ListItem(
                headlineContent = { Text(tr("leaderboard.settings.hide", "Non apparire nella classifica dei soci")) },
                supportingContent = {
                    Text(
                        tr(
                            "leaderboard.settings.hide.detail",
                            "La tua riga non viene calcolata né mostrata, a nessuno: chi è dietro di te sale di posizione. I tuoi timbri contano comunque per le medaglie e per le classifiche di regioni e segretari. Puoi rientrare quando vuoi.",
                        ),
                    )
                },
                trailingContent = {
                    Switch(
                        checked = hidden,
                        enabled = !saving,
                        onCheckedChange = { value ->
                            scope.launch {
                                saving = true
                                failed = runCatching { repository.setHidden(value) }.isFailure
                                saving = false
                            }
                        },
                    )
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            if (failed) {
                Text(
                    text = tr("leaderboard.settings.failed", "Non sono riuscito a salvare. Riprova."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
