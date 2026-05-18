package it.mensa.app.features.events._components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.LocationModel
import kotlinx.coroutines.launch

/**
 * MapLocationPickerSheet — Android equivalent of iOS MapLocationPickerSheet.swift.
 * Google Maps picker with draggable marker for creating a new saved location.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapLocationPickerSheet(
    onCreated: (LocationModel) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val repo = remember { koinAccess().locations }

    // Italy center as default
    var markerLat by remember { mutableDoubleStateOf(41.9028) }
    var markerLon by remember { mutableDoubleStateOf(12.4964) }
    var locationName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(markerLat, markerLon), 6f)
    }
    val markerState = remember { MarkerState(position = LatLng(markerLat, markerLon)) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) { Text("Annulla") }
                Text("Nuova posizione", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                TextButton(
                    onClick = {
                        if (locationName.isBlank()) { error = "Il nome è obbligatorio"; return@TextButton }
                        scope.launch {
                            saving = true
                            try {
                                val loc = repo.createAndAddLocal(
                                    name = locationName.trim(),
                                    address = address.trim(),
                                    lat = markerLat,
                                    lon = markerLon,
                                )
                                onCreated(loc)
                            } catch (e: Exception) {
                                error = e.message
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving,
                ) { Text("Salva", fontWeight = FontWeight.SemiBold) }
            }

            Spacer(Modifier.height(8.dp))

            // Map with draggable marker
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapLongClick = { latLng ->
                        markerLat = latLng.latitude
                        markerLon = latLng.longitude
                        markerState.position = latLng
                    },
                ) {
                    Marker(
                        state = markerState,
                        draggable = true,
                        title = locationName.ifBlank { "Posizione" },
                        onInfoWindowClick = {},
                    )
                }
                Text(
                    "Tieni premuto per spostare il marker",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = locationName,
                onValueChange = { locationName = it },
                label = { Text("Nome posizione *") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Indirizzo (opzionale)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
