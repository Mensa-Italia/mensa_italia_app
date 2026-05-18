package it.mensa.app.features.events._components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.features.events.ScheduleDraftUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ScheduleListSheet — Android equivalent of iOS ScheduleListSheet.swift.
 * Manages the list of schedule entries for an event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleListSheet(
    schedules: List<ScheduleDraftUi>,
    onSchedulesChange: (List<ScheduleDraftUi>) -> Unit,
    onClose: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showAdd by remember { mutableStateOf(false) }
    var editingSchedule by remember { mutableStateOf<ScheduleDraftUi?>(null) }

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.ITALIAN) }
    val dayFormatter = remember { SimpleDateFormat("EEEE, d MMMM", Locale.ITALIAN) }

    val visible = schedules.filter { !it.isDeleted }.sortedBy { it.whenStart }

    ModalBottomSheet(onDismissRequest = onClose, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onClose) { Text("Fine") }
                Text("Programma", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                IconButton(onClick = { showAdd = true }) { Icon(Icons.Default.Add, contentDescription = "Aggiungi") }
            }

            if (visible.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nessun orario. Tocca '+' per aggiungere.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                    // Group by day
                    val grouped = visible.groupBy { s ->
                        val cal = java.util.Calendar.getInstance()
                        cal.timeInMillis = s.whenStart
                        cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                        cal.set(java.util.Calendar.MINUTE, 0)
                        cal.set(java.util.Calendar.SECOND, 0)
                        cal.set(java.util.Calendar.MILLISECOND, 0)
                        cal.timeInMillis
                    }.entries.sortedBy { it.key }

                    grouped.forEach { (dayMillis, dayItems) ->
                        item {
                            Text(
                                dayFormatter.format(Date(dayMillis)).replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(dayItems, key = { it.stableId }) { s ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        val updated = schedules.map { item ->
                                            if (item.stableId == s.stableId) {
                                                if (item.id != null) item.copy(id = "DELETE:${item.id}") else null
                                            } else item
                                        }.filterNotNull()
                                        onSchedulesChange(updated)
                                        true
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
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(s.title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                    TextButton(onClick = { editingSchedule = s }) {
                                        Text(timeFormatter.format(Date(s.whenStart)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        ScheduleEditorSheet(
            initial = null,
            onSaved = { draft ->
                onSchedulesChange(schedules + draft)
                showAdd = false
            },
            onDismiss = { showAdd = false },
        )
    }

    editingSchedule?.let { current ->
        ScheduleEditorSheet(
            initial = current,
            onSaved = { updated ->
                onSchedulesChange(schedules.map { if (it.stableId == updated.stableId) updated else it })
                editingSchedule = null
            },
            onDeleteRequested = {
                onSchedulesChange(schedules.map { item ->
                    if (item.stableId == current.stableId) {
                        if (item.id != null && !item.id!!.startsWith("DELETE:")) item.copy(id = "DELETE:${item.id}")
                        else item
                    } else item
                }.filter { it.id != null || it.stableId != current.stableId })
                editingSchedule = null
            },
            onDismiss = { editingSchedule = null },
        )
    }
}
