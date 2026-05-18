package it.mensa.app.features.sigs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.shared.model.SigModel
import it.mensa.shared.repository.SigDraft

enum class SigGroupType(val rawValue: String, val label: String) {
    SigFacebook("sig_facebook", "SIG Facebook"),
    SigGeneric("sig", "SIG Generic"),
    Local("local", "Gruppo locale"),
    ChatWhatsapp("chat_whatsapp", "Chat WhatsApp"),
    ChatTelegram("chat_telegram", "Chat Telegram"),
    Chat("chat", "Chat");

    companion object {
        fun fromRaw(raw: String): SigGroupType =
            values().firstOrNull { it.rawValue == raw } ?: SigGeneric
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSigSheet(
    initial: SigModel?,
    onSubmitted: (SigDraft) -> Unit,
    onDeleteRequested: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val isEditing = initial != null
    val context = LocalContext.current

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var link by remember { mutableStateOf(initial?.link ?: "") }
    var groupType by remember {
        mutableStateOf(SigGroupType.fromRaw(initial?.groupType ?: "sig"))
    }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val canSubmit = name.trim().isNotEmpty() && link.trim().isNotEmpty()

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            imageBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        }
    }

    val remoteCoverUrl = remember(initial) {
        if (initial == null || initial.image.isEmpty()) null
        else if (initial.image.startsWith("http")) initial.image
        else FilesUrl.build("sigs", initial.id, initial.image, "1500x600")
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss) {
                    Text(tr("app.cancel", fallback = "Annulla"))
                }
                Text(
                    if (isEditing)
                        tr("sigs.edit.title", fallback = "Modifica community")
                    else
                        tr("sigs.add.title", fallback = "Nuova community"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = {
                        if (!canSubmit) return@TextButton
                        val draft = SigDraft(
                            name = name.trim(),
                            link = link.trim(),
                            groupType = groupType.rawValue,
                            description = "",
                            imageBytes = imageBytes,
                            imageFilename = if (imageBytes != null) "cover.jpg" else null,
                            imageContentType = if (imageBytes != null) "image/jpeg" else null,
                        )
                        onSubmitted(draft)
                    },
                    enabled = canSubmit,
                ) {
                    Text(
                        if (isEditing)
                            tr("app.update", fallback = "Aggiorna")
                        else
                            tr("app.create", fallback = "Crea"),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // Cover picker
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1528f / 603f)
                    .clip(RoundedCornerShape(22.dp))
                    .clickable { imagePickerLauncher.launch("image/*") },
            ) {
                when {
                    imageUri != null -> {
                        CachedAsyncImage(
                            model = imageUri,
                            contentDescription = "Copertina",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = { imageUri = null; imageBytes = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "Rimuovi",
                                tint = Color.White,
                            )
                        }
                    }
                    remoteCoverUrl != null -> {
                        CachedAsyncImage(
                            model = remoteCoverUrl,
                            contentDescription = "Copertina",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    else -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = 1.5.dp,
                                    brush = SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(22.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    tr("sigs.cover.pick", fallback = "Tocca per scegliere una copertina"),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(tr("sigs.field.name", fallback = "Nome")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Link field
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text(tr("sigs.field.link", fallback = "Link")) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Type picker
            Text(
                tr("sigs.section.type", fallback = "Tipo"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SigGroupType.values().forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { groupType = type }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(
                        selected = groupType == type,
                        onClick = { groupType = type },
                    )
                    Text(type.label, style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Delete button (edit only)
            if (isEditing) {
                Divider()
                TextButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(tr("sigs.delete", fallback = "Elimina community"))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(tr("sigs.delete.confirm.title", fallback = "Eliminare questa community?")) },
            text = { Text(tr("sigs.delete.confirm.body", fallback = "Questa azione non è annullabile.")) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteRequested()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) {
                    Text(tr("sigs.delete", fallback = "Elimina community"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(tr("app.cancel", fallback = "Annulla"))
                }
            },
        )
    }
}
