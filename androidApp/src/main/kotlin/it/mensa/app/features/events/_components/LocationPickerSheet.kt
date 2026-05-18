package it.mensa.app.features.events._components

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.LocationModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * LocationPickerSheet — Android equivalent of iOS LocationPickerSheet.swift.
 * Shows a list of saved locations with options to pick or delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerSheet(
    onPicked: (LocationModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val repo = remember { koinAccess().locations }
    val locations by repo.observeAll().collectAsState(initial = emptyList())
    var hasLoaded by remember { mutableStateOf(false) }
    var showMapPicker by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<LocationModel?>(null) }
    var deleteError by remember { mutableStateOf<String?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        try { repo.refresh() } catch (_: Exception) {}
        hasLoaded = true
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Annulla") }
                Text("Le tue posizioni", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                IconButton(onClick = { showMapPicker = true }) { Icon(Icons.Default.Add, contentDescription = "Aggiungi posizione") }
            }

            when {
                !hasLoaded -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                locations.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nessuna posizione salvata. Tocca '+' per crearne una.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                else -> LazyColumn(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                    items(locations, key = { it.id }) { loc ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value == SwipeToDismissBoxValue.EndToStart) {
                                    pendingDelete = loc; false
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                                    Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error, modifier = Modifier.padding(end = 16.dp))
                                }
                            },
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { onPicked(loc) }.padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(loc.name, style = MaterialTheme.typography.bodyMedium)
                                    if (loc.address.isNotBlank()) {
                                        Text(loc.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    if (showMapPicker) {
        MapLocationPickerSheet(
            onCreated = { showMapPicker = false },
            onDismiss = { showMapPicker = false },
        )
    }

    pendingDelete?.let { loc ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Eliminare questa posizione?") },
            text = { Text(loc.name) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    // TODO: repo.deleteOne(loc.id) — LocationsRepository.deleteOne not yet exposed
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Annulla") } },
        )
    }

    deleteError?.let {
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text("Errore") },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = { deleteError = null }) { Text("OK") } },
        )
    }
}
