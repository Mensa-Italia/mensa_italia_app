package it.mensa.app.features.events._components

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import it.mensa.app.features.events.util.DistanceSteps
import it.mensa.app.features.events.util.EventFilterState
import it.mensa.app.features.events.util.EventType
import it.mensa.app.features.events.util.ItalianRegions

/**
 * EventFiltersSheet — Android equivalent of iOS EventFiltersSheet.swift.
 * ModalBottomSheet with type, distance, and region filter sections.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class, ExperimentalLayoutApi::class)
@Composable
fun EventFiltersSheet(
    state: EventFilterState,
    onApply: (EventFilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember { mutableStateOf(state) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val locationPermission = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { Box(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
            Box(Modifier.width(32.dp).height(4.dp).background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(2.dp)).align(Alignment.Center))
        }},
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = { draft = draft.reset() }) {
                    Text("Reset", color = if (draft.isEmpty) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary)
                }
                Text("Filtri", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Button(onClick = { onApply(draft); onDismiss() }) {
                    Text("Applica")
                }
            }

            // Type section
            FilterSectionLabel("Tipo evento")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                EventType.values().forEach { type ->
                    val selected = draft.types.contains(type)
                    FilterChip(
                        text = type.label,
                        selected = selected,
                        onClick = {
                            draft = draft.copy(
                                types = if (selected) draft.types - type else draft.types + type,
                            )
                        }
                    )
                }
            }
            if (draft.types.isEmpty()) {
                Text("Nessuna selezione = mostra tutto", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // Distance section
            FilterSectionLabel("Distanza")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Usa la mia posizione", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = draft.useMyLocation,
                    onCheckedChange = { enabled ->
                        draft = draft.copy(
                            useMyLocation = enabled,
                            maxDistanceKm = if (enabled && draft.maxDistanceKm == null) 50 else draft.maxDistanceKm,
                        )
                        if (enabled) locationPermission.launchPermissionRequest()
                    },
                )
            }
            if (draft.useMyLocation) {
                if (!locationPermission.status.isGranted) {
                    Text("Autorizza la posizione nelle impostazioni.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                } else {
                    val stepIndex = draft.maxDistanceKm?.let { km ->
                        DistanceSteps.kmValues.indexOf(km).takeIf { it >= 0 }
                    } ?: DistanceSteps.kmValues.size
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Raggio", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            DistanceSteps.label(draft.maxDistanceKm),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Slider(
                        value = stepIndex.toFloat(),
                        onValueChange = { v ->
                            val i = v.toInt()
                            draft = draft.copy(
                                maxDistanceKm = if (i >= DistanceSteps.kmValues.size) null else DistanceSteps.kmValues[i]
                            )
                        },
                        valueRange = 0f..DistanceSteps.kmValues.size.toFloat(),
                        steps = DistanceSteps.kmValues.size - 1,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("5 km", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Illimitato", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Region section
            FilterSectionLabel("Regione")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ItalianRegions.all.forEach { region ->
                    val selected = draft.regions.contains(region)
                    FilterChip(
                        text = region,
                        selected = selected,
                        onClick = {
                            draft = draft.copy(
                                regions = if (selected) draft.regions - region else draft.regions + region,
                            )
                        }
                    )
                }
            }
            Text("Confrontato con l'indirizzo dell'evento.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun FilterSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun FilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(50))
            .border(1.dp, borderColor, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = textColor)
    }
}
