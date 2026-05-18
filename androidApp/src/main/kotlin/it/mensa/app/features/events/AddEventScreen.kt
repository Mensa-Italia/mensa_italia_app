package it.mensa.app.features.events

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.events._components.EventCardBuilderSheet
import it.mensa.app.features.events._components.LocationPickerSheet
import it.mensa.app.features.events._components.ScheduleListSheet
import it.mensa.app.support.FilesUrl
import it.mensa.app.ui.components.CachedAsyncImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * AddEventScreen — Android equivalent of iOS AddEventView.swift.
 * Full CRUD form for creating or editing an event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventScreen(
    eventId: String? = null,
    onDismiss: () -> Unit = {},
    vm: AddEventViewModel = koinViewModel { parametersOf(eventId) },
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val dtFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN) }

    var showImageOptions by remember { mutableStateOf(false) }
    var showPhotoPicker by remember { mutableStateOf(false) }
    var showCardBuilder by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showScheduleList by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                val ext = if (mimeType.contains("png")) "png" else "jpg"
                vm.updateImage(bytes, "cover.$ext", mimeType)
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it); vm.clearError() }
    }
    LaunchedEffect(state.dismissed) {
        if (state.dismissed) onDismiss()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEditing) "Modifica evento" else "Nuovo evento") },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Annulla") } },
                actions = {
                    if (vm.isEditing && state.canControlEvents) {
                        IconButton(onClick = { showDeleteConfirm = true }, enabled = !state.saving) {
                            Icon(Icons.Default.Delete, contentDescription = "Elimina", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (state.saving) { CircularProgressIndicator(modifier = Modifier.size(24.dp)) }
                    else { TextButton(onClick = { vm.save() }) { Text(if (vm.isEditing) "Aggiorna" else "Salva", fontWeight = FontWeight.SemiBold) } }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Cover image section (admin only)
            if (state.canControlEvents) {
                Text("Copertina", style = MaterialTheme.typography.labelLarge)
                CoverImagePicker(state = state, onClick = { showImageOptions = true })
                HorizontalDivider()
            }

            // Type section (admin only)
            if (state.canControlEvents) {
                Text("Tipo evento", style = MaterialTheme.typography.labelLarge)
                ToggleRow("Online", state.isOnline) { vm.updateIsOnline(it) }
                ToggleRow("Evento nazionale", state.isNational) { vm.updateIsNational(it) }
                ToggleRow("Spot", state.isSpot) { vm.updateIsSpot(it) }
                HorizontalDivider()
            }

            // Details section
            Text("Dettagli", style = MaterialTheme.typography.labelLarge)
            OutlinedTextField(value = state.name, onValueChange = vm::updateName, label = { Text("Nome evento") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = state.description, onValueChange = vm::updateDescription, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth(), minLines = 3, maxLines = 8)
            OutlinedTextField(value = state.infoLink, onValueChange = vm::updateInfoLink, label = { Text("Link info (opzionale)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            HorizontalDivider()

            // Location (only if not online)
            if (!state.isOnline) {
                Text("Dove", style = MaterialTheme.typography.labelLarge)
                Surface(
                    modifier = Modifier.fillMaxWidth().clickable { showLocationPicker = true },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 2.dp,
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (state.position != null) {
                                Text(state.position!!.name.ifBlank { "Posizione" }, style = MaterialTheme.typography.bodyMedium)
                                if (state.position!!.address.isNotBlank()) Text(state.position!!.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                Text("Seleziona una posizione", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                }
                HorizontalDivider()
            }

            // When section
            Text("Quando", style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Inizio", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showStartDatePicker = true }) { Text(dtFormatter.format(Date(state.startDateMillis)), color = MaterialTheme.colorScheme.primary) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Fine", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { showEndDatePicker = true }) { Text(dtFormatter.format(Date(state.endDateMillis)), color = MaterialTheme.colorScheme.primary) }
            }
            HorizontalDivider()

            // Schedule section
            Text("Programma", style = MaterialTheme.typography.labelLarge)
            Surface(modifier = Modifier.fillMaxWidth().clickable { showScheduleList = true }, shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh, tonalElevation = 2.dp) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("Sessioni", style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${state.schedules.count { !it.isDeleted }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }

        // Sheets
        if (showImageOptions) {
            AlertDialog(
                onDismissRequest = { showImageOptions = false },
                title = { Text("Scegli una copertina") },
                text = { Text("Come vuoi aggiungere la copertina?") },
                confirmButton = { TextButton(onClick = { showImageOptions = false; showPhotoPicker = true }) { Text("Dalla libreria") } },
                dismissButton = { TextButton(onClick = { showImageOptions = false; showCardBuilder = true }) { Text("Genera con AI") } },
            )
        }
        if (showPhotoPicker) { photoPickerLauncher.launch("image/*"); showPhotoPicker = false }
        if (showCardBuilder) {
            EventCardBuilderSheet(
                onConfirmed = { bytes -> vm.updateImage(bytes, "event_card.png", "image/png"); showCardBuilder = false },
                onDismiss = { showCardBuilder = false },
            )
        }
        if (showLocationPicker) {
            LocationPickerSheet(
                onPicked = { loc -> vm.updatePosition(loc); showLocationPicker = false },
                onDismiss = { showLocationPicker = false },
            )
        }
        if (showScheduleList) {
            ScheduleListSheet(
                schedules = state.schedules,
                onSchedulesChange = vm::updateSchedules,
                onClose = { showScheduleList = false },
            )
        }
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Eliminare l'evento?") },
                text = { Text("L'azione non può essere annullata.") },
                confirmButton = { TextButton(onClick = { showDeleteConfirm = false; vm.delete() }) { Text("Elimina", color = MaterialTheme.colorScheme.error) } },
                dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") } },
            )
        }
        if (showStartDatePicker) {
            DateTimePickerDialog(
                initialMillis = state.startDateMillis,
                onPicked = { vm.updateStartDate(it); showStartDatePicker = false },
                onDismiss = { showStartDatePicker = false },
            )
        }
        if (showEndDatePicker) {
            DateTimePickerDialog(
                initialMillis = state.endDateMillis,
                onPicked = { vm.updateEndDate(it); showEndDatePicker = false },
                onDismiss = { showEndDatePicker = false },
            )
        }
    }
}

@Composable
private fun CoverImagePicker(state: AddEventUiState, onClick: () -> Unit) {
    val imageBytes = state.imageBytes
    val aspect = 1528f / 603f
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(aspect).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick)
    ) {
        if (imageBytes != null) {
            val bmp = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bmp != null) {
                androidx.compose.foundation.Image(bitmap = bmp.asImageBitmap(), contentDescription = "Cover", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.Image, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Tocca per scegliere una copertina", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateTimePickerDialog(initialMillis: Long, onPicked: (Long) -> Unit, onDismiss: () -> Unit) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
    var showTimePicker by remember { mutableStateOf(false) }

    if (!showTimePicker) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = { TextButton(onClick = { showTimePicker = true }) { Text("Avanti") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        ) { DatePicker(state = dateState) }
    } else {
        val timeState = rememberTimePickerState(initialHour = cal.get(Calendar.HOUR_OF_DAY), initialMinute = cal.get(Calendar.MINUTE))
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Ora") },
            text = { TimePicker(state = timeState) },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = dateState.selectedDateMillis ?: initialMillis
                    val dayCal = Calendar.getInstance().apply { timeInMillis = selectedDate; set(Calendar.HOUR_OF_DAY, timeState.hour); set(Calendar.MINUTE, timeState.minute); set(Calendar.SECOND, 0) }
                    onPicked(dayCal.timeInMillis)
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } },
        )
    }
}
