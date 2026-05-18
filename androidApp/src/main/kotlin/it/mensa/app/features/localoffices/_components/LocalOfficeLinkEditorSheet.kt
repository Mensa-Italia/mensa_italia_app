package it.mensa.app.features.localoffices._components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import kotlinx.coroutines.launch

sealed class LinkEditorMode {
    object CreateSection : LinkEditorMode()
    object CreateLink : LinkEditorMode()
    data class Edit(val existing: LocalOfficeLinktreeRowModel) : LinkEditorMode()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalOfficeLinkEditorSheet(
    officeId: String,
    siblings: List<LocalOfficeLinktreeRowModel>,
    parentCandidates: List<LocalOfficeLinktreeRowModel>, // kind == "section"
    mode: LinkEditorMode,
    onDismiss: () -> Unit,
) {
    val repo = remember { koinAccess().localOffices }
    val scope = rememberCoroutineScope()

    val maxOrder = siblings.maxOfOrNull { it.sortOrder } ?: 0

    var kind by remember {
        mutableStateOf(
            when (mode) {
                is LinkEditorMode.CreateSection -> "section"
                is LinkEditorMode.CreateLink -> "link"
                is LinkEditorMode.Edit -> mode.existing.kind
            }
        )
    }
    var title by remember {
        mutableStateOf(if (mode is LinkEditorMode.Edit) mode.existing.title else "")
    }
    var url by remember {
        mutableStateOf(if (mode is LinkEditorMode.Edit) mode.existing.url else "")
    }
    var icon by remember {
        mutableStateOf(if (mode is LinkEditorMode.Edit) mode.existing.icon else "")
    }
    var parentId by remember {
        mutableStateOf(if (mode is LinkEditorMode.Edit) mode.existing.parent else "")
    }
    var active by remember {
        mutableStateOf(true)
    }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val isCreating = mode !is LinkEditorMode.Edit
    val navTitle = when (mode) {
        is LinkEditorMode.CreateSection -> tr("local_office.links.add_section", fallback = "Aggiungi sezione")
        is LinkEditorMode.CreateLink -> tr("local_office.links.add_link", fallback = "Aggiungi link")
        is LinkEditorMode.Edit -> tr("local_office.links.edit", fallback = "Modifica voce")
    }

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
                Text(navTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (saving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    TextButton(
                        onClick = {
                            val trimTitle = title.trim()
                            if (trimTitle.isEmpty()) return@TextButton
                            saving = true
                            scope.launch {
                                try {
                                    if (isCreating) {
                                        repo.createLinkFromFields(
                                            officeId = officeId,
                                            kind = kind,
                                            parent = parentId,
                                            title = trimTitle,
                                            url = url.trim(),
                                            icon = icon.trim(),
                                            sortOrder = maxOrder + 1,
                                            active = active,
                                        )
                                    } else {
                                        val existing = (mode as LinkEditorMode.Edit).existing
                                        repo.updateLinkFields(
                                            officeId = officeId,
                                            id = existing.id,
                                            kind = kind,
                                            parent = parentId,
                                            title = trimTitle,
                                            url = url.trim(),
                                            icon = icon.trim(),
                                            active = active,
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
                        enabled = title.trim().isNotEmpty(),
                    ) {
                        Text(
                            tr("local_office.editor.save", fallback = "Salva"),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            // Kind segmented picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("section" to tr("local_office.editor.kind.section", fallback = "Sezione"),
                    "link" to tr("local_office.editor.kind.link", fallback = "Link")).forEach { (k, label) ->
                    FilterChip(
                        selected = kind == k,
                        onClick = { kind = k },
                        label = { Text(label) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Title
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(tr("local_office.editor.title", fallback = "Titolo")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // URL (only for links)
            if (kind == "link") {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text(tr("local_office.editor.url", fallback = "URL")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )
            }

            // Icon
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                OutlinedTextField(
                    value = icon,
                    onValueChange = { icon = it },
                    label = { Text(tr("local_office.editor.icon", fallback = "Icona")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text(
                    tr("local_office.editor.icon.hint", fallback = "Inserisci un emoji o il nome di un SF Symbol"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Parent picker
            if (parentCandidates.isNotEmpty()) {
                Text(
                    tr("local_office.editor.parent", fallback = "Sezione padre"),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { parentId = "" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = parentId.isEmpty(), onClick = { parentId = "" })
                    Text(tr("local_office.editor.parent.none", fallback = "Nessuna (root)"))
                }
                parentCandidates.forEach { sec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { parentId = sec.id },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = parentId == sec.id, onClick = { parentId = sec.id })
                        Text(sec.title)
                    }
                }
            }

            // Active toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(tr("local_office.editor.active", fallback = "Attivo"))
                Switch(checked = active, onCheckedChange = { active = it })
            }
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
