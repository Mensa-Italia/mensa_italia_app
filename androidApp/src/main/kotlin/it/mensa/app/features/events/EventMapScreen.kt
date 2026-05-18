package it.mensa.app.features.events

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import it.mensa.app.features.events._components.EventRowCard
import org.koin.androidx.compose.koinViewModel

/**
 * EventMapScreen — Android equivalent of iOS EventMapView.swift.
 * Google Maps with markers for upcoming geo-located events.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventMapScreen(
    onBack: () -> Unit = {},
    onEventClick: (String) -> Unit = {},
    vm: EventMapViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val geoEvents = remember(state.events) { vm.geoEvents() }
    val selectedEvent = remember(state.selectedEventId, state.events) { vm.selectedEvent() }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    // Default camera: Italy center
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(41.9, 12.5), 5.5f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mappa eventi") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro") } },
            )
        },
    ) { innerPadding ->
        GoogleMap(
            modifier = Modifier.padding(innerPadding),
            cameraPositionState = cameraPositionState,
        ) {
            geoEvents.forEach { event ->
                val pos = event.position ?: return@forEach
                Marker(
                    state = MarkerState(position = LatLng(pos.lat, pos.lon)),
                    title = event.name,
                    snippet = if (event.isNational) "Nazionale" else "Locale",
                    onClick = {
                        vm.selectEvent(event.id)
                        false
                    },
                )
            }
        }

        if (selectedEvent != null) {
            ModalBottomSheet(
                onDismissRequest = { vm.selectEvent(null) },
                sheetState = sheetState,
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                    EventRowCard(event = selectedEvent, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onEventClick(selectedEvent.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Vedi dettagli", style = MaterialTheme.typography.bodyMedium)
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}
