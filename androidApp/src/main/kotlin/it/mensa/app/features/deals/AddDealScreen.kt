package it.mensa.app.features.deals

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.tr
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * AddDealScreen — Create / Edit deal form.
 *
 * iOS parity: AddDealView.swift
 * Sections:
 * 1. Informazioni (name, sector, VAT, link)
 * 2. Validità (toggle + date pickers)
 * 3. Dettagli (description, eligibility picker, howToGet)
 * 4. Contatto principale (name, email, phone, note)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDealScreen(
    dealId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddDealViewModel = koinViewModel(parameters = { parametersOf(dealId) }),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Dismiss on success
    LaunchedEffect(state.dismissed) {
        if (state.dismissed) onBack()
    }

    // Error dialog
    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(state.error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            },
        )
    }

    // Delete confirm
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(tr("addons.deals.delete.confirm.title", fallback = "Eliminare il deal?")) },
            text = { Text(tr("addons.deals.delete.confirm.body", fallback = "Questa azione non può essere annullata.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.delete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(tr("app.delete", fallback = "Elimina")) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(tr("app.cancel", fallback = "Annulla"))
                }
            },
        )
    }

    // Start date picker
    if (showStartDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.startDate.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.setStartDate(Instant.fromEpochMilliseconds(it))
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
        ) { DatePicker(state = pickerState) }
    }

    // End date picker
    if (showEndDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.endDate.toEpochMilliseconds())
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        viewModel.setEndDate(Instant.fromEpochMilliseconds(it))
                    }
                    showEndDatePicker = false
                }) { Text("OK") }
            },
        ) { DatePicker(state = pickerState) }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (viewModel.isEditing)
                            tr("addons.deals.edit.title", fallback = "Modifica deal")
                        else
                            tr("addons.deals.add.title", fallback = "Nuovo deal")
                    )
                },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !state.saving && !state.deleting,
                    ) {
                        Text(tr("app.cancel", fallback = "Annulla"))
                    }
                },
                actions = {
                    // Delete (edit mode)
                    if (viewModel.isEditing) {
                        if (state.deleting) {
                            CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = { showDeleteConfirm = true },
                                enabled = !state.saving,
                            ) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = tr("app.delete", fallback = "Elimina"),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    // Save
                    if (state.saving) {
                        CircularProgressIndicator(modifier = Modifier.padding(end = 16.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(
                            onClick = { viewModel.save() },
                            enabled = state.canSave && !state.deleting,
                        ) {
                            Text(
                                text = tr("app.save", fallback = "Salva"),
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            // Section 1: Info
            FormSection(title = tr("addons.deals.add.section.info", fallback = "Informazioni")) {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text(tr("addons.deals.add.field.name", fallback = "Nome convenzione")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = state.name.isBlank() && state.saving,
                )
                OutlinedTextField(
                    value = state.commercialSector,
                    onValueChange = viewModel::setCommercialSector,
                    label = { Text(tr("addons.deals.add.field.sector", fallback = "Settore")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    isError = state.commercialSector.isBlank() && state.saving,
                )
                OutlinedTextField(
                    value = state.vatNumber,
                    onValueChange = viewModel::setVatNumber,
                    label = { Text(tr("addons.deals.add.field.vat", fallback = "P. IVA")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = state.link,
                    onValueChange = viewModel::setLink,
                    label = { Text(tr("addons.deals.add.field.link", fallback = "Link")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Next,
                    ),
                )
            }

            // Section 2: Validity
            FormSection(title = tr("addons.deals.add.section.validity", fallback = "Validità")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = tr("addons.deals.add.field.has_validity", fallback = "Imposta date di validità"),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Switch(
                        checked = state.hasValidity,
                        onCheckedChange = viewModel::setHasValidity,
                    )
                }
                if (state.hasValidity) {
                    OutlinedTextField(
                        value = formatInstantDisplay(state.startDate),
                        onValueChange = {},
                        label = { Text(tr("app.deals.from", fallback = "Dal")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                        },
                    )
                    OutlinedTextField(
                        value = formatInstantDisplay(state.endDate),
                        onValueChange = {},
                        label = { Text(tr("app.deals.until", fallback = "Fino al")) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEndDatePicker = true },
                        readOnly = true,
                        trailingIcon = {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                        },
                    )
                }
            }

            // Section 3: Details
            FormSection(title = tr("addons.deals.add.section.description", fallback = "Dettagli")) {
                OutlinedTextField(
                    value = state.details,
                    onValueChange = viewModel::setDetails,
                    label = { Text(tr("addons.deals.add.field.details", fallback = "Dettagli del deal")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 8,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                // Eligibility picker
                EligibilityDropdown(
                    selected = state.selectedEligibility,
                    onSelected = viewModel::setSelectedEligibility,
                )
                OutlinedTextField(
                    value = state.howToGet,
                    onValueChange = viewModel::setHowToGet,
                    label = { Text(tr("addons.deals.add.field.howtoget", fallback = "Come ottenere il deal")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                )
            }

            // Section 4: Contact
            FormSection(
                title = tr("addons.deals.add.section.contact", fallback = "Contatto principale"),
                footer = tr("addons.deals.add.section.contact.footer", fallback = "(Nascosto al pubblico)"),
            ) {
                OutlinedTextField(
                    value = state.contactName,
                    onValueChange = viewModel::setContactName,
                    label = { Text(tr("addons.deals.add.field.contact_name", fallback = "Nome")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                )
                OutlinedTextField(
                    value = state.contactEmail,
                    onValueChange = viewModel::setContactEmail,
                    label = { Text(tr("addons.deals.add.field.contact_email", fallback = "Email")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = !state.emailLooksValid,
                    supportingText = if (!state.emailLooksValid) {
                        { Text(tr("addons.deals.add.error.invalid_email", fallback = "Email non valida")) }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        capitalization = KeyboardCapitalization.None,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = state.contactPhone,
                    onValueChange = viewModel::setContactPhone,
                    label = { Text(tr("addons.deals.add.field.contact_phone", fallback = "Telefono")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Next,
                    ),
                )
                OutlinedTextField(
                    value = state.contactNote,
                    onValueChange = viewModel::setContactNote,
                    label = { Text(tr("addons.deals.add.field.contact_note", fallback = "Note")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 1,
                    maxLines = 4,
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun FormSection(
    title: String,
    footer: String? = null,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
        )
        HorizontalDivider()
        content()
        footer?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EligibilityDropdown(
    selected: String,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf(
        "active_members" to tr("addons.deals.add.who.active_members", fallback = "Soci attivi"),
        "active_members and relatives" to tr("addons.deals.add.who.active_members_relatives", fallback = "Soci attivi e familiari"),
    )
    val displayValue = options.firstOrNull { it.first == selected }?.second ?: selected

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = displayValue,
            onValueChange = {},
            readOnly = true,
            label = { Text(tr("addons.deals.add.field.who", fallback = "A chi è rivolto")) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { (key, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelected(key)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatInstantDisplay(instant: Instant): String {
    return try {
        val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        "${local.dayOfMonth}/${local.monthNumber}/${local.year}"
    } catch (_: Exception) { "—" }
}
