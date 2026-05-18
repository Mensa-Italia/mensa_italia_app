package it.mensa.app.features.events._components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import it.mensa.app.features.events.ScheduleDraftUi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ScheduleEditorSheet — Android equivalent of iOS ScheduleEditorSheet.swift.
 * Bottom sheet form for creating or editing a schedule entry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorSheet(
    initial: ScheduleDraftUi?,
    onSaved: (ScheduleDraftUi) -> Unit,
    onDeleteRequested: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val isEditing = initial?.id != null && initial.id?.startsWith("DELETE:") == false
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var description by remember { mutableStateOf(initial?.description ?: "") }
    var infoLink by remember { mutableStateOf(initial?.infoLink ?: "") }
    var whenStart by remember { mutableStateOf(initial?.whenStart ?: System.currentTimeMillis()) }
    var whenEnd by remember { mutableStateOf(initial?.whenEnd ?: (System.currentTimeMillis() + 3600_000)) }
    var isSubscriptable by remember { mutableStateOf(initial?.isSubscriptable ?: false) }
    var maxGuests by remember { mutableStateOf(initial?.maxExternalGuests ?: 0) }
    var priceText by remember { mutableStateOf(if ((initial?.price ?: 0.0) > 0) initial!!.price.toString() else "") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val dtFormatter = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onDismiss) { Text("Annulla") }
                Text(if (initial == null) "Nuovo orario" else "Modifica orario", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Button(onClick = {
                    if (title.isBlank()) { validationError = "Il titolo è obbligatorio."; return@Button }
                    if (whenEnd < whenStart) { validationError = "La fine non può precedere l'inizio."; return@Button }
                    val parsedPrice = priceText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    onSaved(ScheduleDraftUi(
                        stableId = initial?.stableId ?: java.util.UUID.randomUUID().toString(),
                        id = initial?.id,
                        title = title.trim(),
                        description = description,
                        infoLink = infoLink.trim(),
                        whenStart = whenStart,
                        whenEnd = whenEnd,
                        maxExternalGuests = if (isSubscriptable) maxGuests else 0,
                        price = if (isSubscriptable) parsedPrice else 0.0,
                        isSubscriptable = isSubscriptable,
                    ))
                }) { Text("Salva") }
            }

            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Titolo") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next), singleLine = true)
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrizione") }, modifier = Modifier.fillMaxWidth(), minLines = 2, maxLines = 5)
            OutlinedTextField(value = infoLink, onValueChange = { infoLink = it }, label = { Text("Link info (opzionale)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))

            // Date pickers as text fields with formatted display
            Text("Quando", style = MaterialTheme.typography.labelLarge)
            Text("Inizio: ${dtFormatter.format(Date(whenStart))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Fine: ${dtFormatter.format(Date(whenEnd))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Prenotabile", style = MaterialTheme.typography.bodyMedium)
                Switch(checked = isSubscriptable, onCheckedChange = { isSubscriptable = it })
            }
            if (isSubscriptable) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Posti per esterni: $maxGuests", style = MaterialTheme.typography.bodyMedium)
                    Row {
                        TextButton(onClick = { if (maxGuests > 0) maxGuests-- }) { Text("-") }
                        TextButton(onClick = { if (maxGuests < 500) maxGuests++ }) { Text("+") }
                    }
                }
                OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Prezzo (€)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }

            if (isEditing) {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Elimina orario") }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminare questo orario?") },
            confirmButton = { TextButton(onClick = { showDeleteConfirm = false; onDeleteRequested() }) { Text("Elimina", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") } },
        )
    }

    if (validationError != null) {
        AlertDialog(
            onDismissRequest = { validationError = null },
            title = { Text("Dati non validi") },
            text = { Text(validationError ?: "") },
            confirmButton = { TextButton(onClick = { validationError = null }) { Text("OK") } },
        )
    }
}
