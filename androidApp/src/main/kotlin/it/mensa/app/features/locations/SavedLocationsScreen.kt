package it.mensa.app.features.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.shared.model.LocationModel
import kotlinx.coroutines.launch

/**
 * SavedLocationsScreen — pagina 1 del picker: le posizioni gia' salvate.
 *
 * Tap su una riga = scelta fatta, il flusso si chiude subito. Il "+" porta alla
 * mappa per crearne una nuova.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedLocationsScreen(
    onPicked: (LocationModel) -> Unit,
    onAddClick: () -> Unit,
    onCancel: () -> Unit,
) {
    val repo = remember { koinAccess().locations }
    val locations by repo.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var hasLoaded by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<LocationModel?>(null) }

    LaunchedEffect(Unit) {
        // La lista locale c'e' comunque: un refresh fallito non merita un errore
        // in faccia, si continua con quello che il DB ha gia'.
        try { repo.refresh() } catch (_: Exception) {}
        hasLoaded = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("app.location.picker.title", fallback = "Le tue posizioni")) },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("app.location.cancel", fallback = "Annulla"),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddClick) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = tr("app.location.add", fallback = "Aggiungi"),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        when {
            !hasLoaded -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            locations.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = tr("app.location.empty.title", fallback = "Nessuna posizione salvata"),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = tr("app.location.empty.body", fallback = "Tocca 'Aggiungi' per crearne una."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }

            else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(locations, key = { it.id }) { loc ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        // Lo swipe apre la conferma e basta: la riga non deve
                        // sparire prima che il server abbia accettato.
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) pendingDelete = loc
                            false
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(end = 16.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = tr("app.delete", fallback = "Elimina"),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                    ) {
                        SavedLocationRow(location = loc, onClick = { onPicked(loc) })
                    }
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { loc ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(tr("app.location.delete.confirm.title", fallback = "Eliminare questa posizione?")) },
            text = { Text(loc.name) },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDelete = null
                        // Se il server rifiuta, la riga resta: il prossimo
                        // refresh() riallinea la lista, non serve un alert.
                        scope.launch { try { repo.deleteOne(loc.id) } catch (_: Exception) {} }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(tr("app.location.delete.confirm.confirm", fallback = "Elimina")) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(tr("app.location.delete.confirm.dismiss", fallback = "Annulla"))
                }
            },
        )
    }
}

@Composable
private fun SavedLocationRow(location: LocationModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Outlined.Place,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = location.name.ifBlank { tr("events.add.location.unnamed", fallback = "Posizione") },
                style = MaterialTheme.typography.bodyLarge,
            )
            if (location.address.isNotBlank()) {
                Text(
                    text = location.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
