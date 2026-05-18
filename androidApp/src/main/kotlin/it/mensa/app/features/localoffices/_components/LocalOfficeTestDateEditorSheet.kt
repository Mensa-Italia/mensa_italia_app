package it.mensa.app.features.localoffices._components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.shared.model.LocalOfficeAssistantModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.util.*

sealed class TestDateEditorMode {
    object Create : TestDateEditorMode()
    data class Edit(val existing: LocalOfficeTestDateModel) : TestDateEditorMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalOfficeTestDateEditorSheet(
    officeId: String,
    assistantsCandidates: List<LocalOfficeAssistantModel>,
    mode: TestDateEditorMode,
    onDismiss: () -> Unit,
) {
    val repo = remember { koinAccess().localOffices }
    val scope = rememberCoroutineScope()

    val isCreating = mode is TestDateEditorMode.Create

    // Seed from existing (edit) or tomorrow (create)
    val initialDate: Date = when (mode) {
        is TestDateEditorMode.Create -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }.time
        is TestDateEditorMode.Edit -> Date(mode.existing.date.toEpochMilliseconds())
    }

    var dateMs by remember { mutableStateOf(initialDate.time) }
    var location by remember {
        mutableStateOf(if (mode is TestDateEditorMode.Edit) mode.existing.location else "")
    }
    var notes by remember {
        mutableStateOf(if (mode is TestDateEditorMode.Edit) mode.existing.notes else "")
    }
    var maxParticipants by remember {
        mutableStateOf(if (mode is TestDateEditorMode.Edit) mode.existing.maxParticipants else 0)
    }
    var selectedAssistants by remember {
        mutableStateOf(
            if (mode is TestDateEditorMode.Edit) mode.existing.assistants.toSet() else emptySet()
        )
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(tr("local_office.editor.cancel", fallback = "Annulla"))
                }
                Text(
                    if (isCreating)
                        tr("local_office.test_dates.add", fallback = "Aggiungi sessione")
                    else
                        tr("local_office.test_dates.edit", fallback = "Modifica sessione"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(
                        onClick = {
                            saving = true
                            scope.launch {
                                try {
                                    val instant = Instant.fromEpochMilliseconds(dateMs)
                                    val assistantsList = selectedAssistants.toList()
                                    if (isCreating) {
                                        repo.createTestDateFromFields(
                                            officeId = officeId,
                                            date = instant,
                                            location = location.trim(),
                                            notes = notes.trim(),
                                            maxParticipants = maxParticipants,
                                            assistants = assistantsList,
                                        )
                                    } else {
                                        val existing = (mode as TestDateEditorMode.Edit).existing
                                        repo.updateTestDateFields(
                                            officeId = officeId,
                                            id = existing.id,
                                            date = instant,
                                            location = location.trim(),
                                            notes = notes.trim(),
                                            maxParticipants = maxParticipants,
                                            assistants = assistantsList,
                                        )
                                    }
                                    onDismiss()
                                } catch (e: Exception) {
                                    error = e.message
                                } finally {
                                    saving = false
                                }
                            }
                        },
                    ) {
                        Text(
                            tr("local_office.editor.save", fallback = "Salva"),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Date picker trigger
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("${tr("local_office.editor.date", fallback = "Data e ora")}: ${formatDateShort(dateMs)}")
            }

            // Location
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(tr("local_office.editor.location", fallback = "Luogo")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(tr("local_office.editor.notes", fallback = "Note")) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 6,
            )

            // Max participants stepper
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tr("local_office.editor.max_participants", fallback = "Max partecipanti"))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = { if (maxParticipants > 0) maxParticipants-- },
                    ) { Text("−") }
                    Text("$maxParticipants", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    FilledTonalIconButton(
                        onClick = { if (maxParticipants < 500) maxParticipants++ },
                    ) { Text("+") }
                }
            }

            // Assistants checkboxes
            if (assistantsCandidates.isNotEmpty()) {
                Text(
                    tr("local_office.editor.assistants", fallback = "Assistenti"),
                    style = MaterialTheme.typography.labelLarge,
                )
                assistantsCandidates.forEach { assistant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedAssistants = if (selectedAssistants.contains(assistant.user))
                                    selectedAssistants - assistant.user
                                else
                                    selectedAssistants + assistant.user
                            }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Checkbox(
                            checked = selectedAssistants.contains(assistant.user),
                            onCheckedChange = { checked ->
                                selectedAssistants = if (checked)
                                    selectedAssistants + assistant.user
                                else
                                    selectedAssistants - assistant.user
                            },
                        )
                        Text(assistant.name)
                    }
                }
            }
        }
    }

    // Date/time picker dialog (Material3 DatePicker)
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateMs)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMs = it }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Annulla") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    error?.let { err ->
        AlertDialog(
            onDismissRequest = { error = null },
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("OK") } },
        )
    }
}

private fun formatDateShort(ms: Long): String {
    val date = Date(ms)
    val fmt = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.ITALIAN)
    return fmt.format(date)
}
