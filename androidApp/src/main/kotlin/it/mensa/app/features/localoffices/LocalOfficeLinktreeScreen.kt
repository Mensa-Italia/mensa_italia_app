package it.mensa.app.features.localoffices

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.localoffices._components.LinkEditorMode
import it.mensa.app.features.localoffices._components.LocalOfficeLinkEditorSheet
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Full linktree page for a local office.
 * Visually strong: brand gradient background, large CTA link buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalOfficeLinktreeScreen(
    officeId: String,
    onBack: () -> Unit = {},
    vm: LocalOfficeLinktreeViewModel = koinViewModel(parameters = { parametersOf(officeId) }),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val canEdit = remember(state) { vm.canEdit(state) }
    val context = LocalContext.current

    var creatingLinkMode by remember { mutableStateOf<LinkEditorMode?>(null) }
    var editingLink by remember { mutableStateOf<LocalOfficeLinktreeRowModel?>(null) }
    var deletingLink by remember { mutableStateOf<LocalOfficeLinktreeRowModel?>(null) }

    val rootLinks = remember(state) { vm.rootLinks(state) }
    val sections = remember(state) { vm.sections(state) }

    // Brand gradient background
    val primary = MaterialTheme.colorScheme.primary
    val brandGradient = Brush.verticalGradient(
        colors = listOf(primary, Color(0xFF0D2E6B), Color(0xFF061F2E)),
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        tr("local_office.linktree.title", fallback = "Link utili"),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = tr("app.back", fallback = "Indietro"), tint = Color.White)
                    }
                },
                actions = {
                    if (canEdit) {
                        IconButton(onClick = { creatingLinkMode = LinkEditorMode.CreateLink }) {
                            Icon(Icons.Outlined.Add, contentDescription = tr("local_office.linktree.add", fallback = "Aggiungi"), tint = Color.White)
                        }
                        IconButton(onClick = { creatingLinkMode = LinkEditorMode.CreateSection }) {
                            Icon(Icons.Outlined.CreateNewFolder, contentDescription = tr("local_office.linktree.add_section", fallback = "Aggiungi sezione"), tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brandGradient),
        ) {
            when {
                state.loading -> LoadingDots(modifier = Modifier.align(Alignment.Center))
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            top = innerPadding.calculateTopPadding() + 16.dp,
                            start = 20.dp,
                            end = 20.dp,
                            bottom = 40.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        // Office hero header
                        state.office?.let { office ->
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    // Office logo
                                    if (office.image.isNotEmpty()) {
                                        val logoUrl = FilesUrl.build("local_offices", office.id, office.image, "200x200")
                                        Box(
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(20.dp)),
                                        ) {
                                            CachedAsyncImage(
                                                model = logoUrl,
                                                contentDescription = office.name,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }
                                    Text(
                                        office.name,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                    )
                                    if (office.bio.isNotEmpty()) {
                                        Text(
                                            office.bio,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.75f),
                                            textAlign = TextAlign.Center,
                                        )
                                    }
                                }
                            }
                        }

                        // Root links (large CTA buttons)
                        if (rootLinks.isNotEmpty()) {
                            items(rootLinks, key = { it.id }) { link ->
                                LinktreeLinkButton(
                                    link = link,
                                    canEdit = canEdit,
                                    onClick = {
                                        if (link.url.isNotEmpty()) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                                        }
                                    },
                                    onEdit = { editingLink = link },
                                    onDelete = { deletingLink = link },
                                )
                            }
                        }

                        // Sections with children
                        sections.forEach { section ->
                            item(key = "section_${section.id}") {
                                LinktreeSectionHeader(title = section.title, canEdit = canEdit, onEdit = { editingLink = section }, onDelete = { deletingLink = section })
                            }
                            val children = vm.children(section.id, state)
                            items(children, key = { it.id }) { child ->
                                LinktreeLinkButton(
                                    link = child,
                                    canEdit = canEdit,
                                    onClick = {
                                        if (child.url.isNotEmpty()) {
                                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(child.url)))
                                        }
                                    },
                                    onEdit = { editingLink = child },
                                    onDelete = { deletingLink = child },
                                )
                            }
                        }

                        if (rootLinks.isEmpty() && sections.isEmpty()) {
                            item {
                                Text(
                                    tr("local_office.linktree.empty", fallback = "Nessun link"),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Sheets & Dialogs
    creatingLinkMode?.let { mode ->
        LocalOfficeLinkEditorSheet(
            officeId = officeId,
            siblings = state.linktree,
            parentCandidates = state.linktree.filter { it.kind == "section" },
            mode = mode,
            onDismiss = { creatingLinkMode = null },
        )
    }

    editingLink?.let { link ->
        LocalOfficeLinkEditorSheet(
            officeId = officeId,
            siblings = state.linktree.filter { it.parent == link.parent },
            parentCandidates = state.linktree.filter { it.kind == "section" && it.id != link.id },
            mode = LinkEditorMode.Edit(link),
            onDismiss = { editingLink = null },
        )
    }

    deletingLink?.let { link ->
        AlertDialog(
            onDismissRequest = { deletingLink = null },
            title = { Text(tr("local_office.links.delete_confirm", fallback = "Vuoi eliminare questa voce?")) },
            text = { Text(link.title) },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteLink(link.id); deletingLink = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(tr("local_office.editor.delete", fallback = "Elimina")) }
            },
            dismissButton = {
                TextButton(onClick = { deletingLink = null }) {
                    Text(tr("local_office.editor.cancel", fallback = "Annulla"))
                }
            },
        )
    }

    state.error?.let { err ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text("Errore") },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
        )
    }
}

// ─── Large CTA link button (Material 3 Expressive) ────────────────────────────

@Composable
private fun LinktreeLinkButton(
    link: LocalOfficeLinktreeRowModel,
    canEdit: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.13f),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Icon / emoji
            Text(
                resolveIcon(link.icon),
                style = MaterialTheme.typography.titleLarge,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    link.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (link.url.isNotEmpty()) {
                    Text(
                        link.url,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (canEdit) {
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Modifica", modifier = Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.7f))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.Delete, contentDescription = "Elimina", modifier = Modifier.size(18.dp), tint = Color(0xFFFF6B6B))
                    }
                }
            } else {
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color.White.copy(alpha = 0.55f),
                )
            }
        }
    }
}

@Composable
private fun LinktreeSectionHeader(
    title: String,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        )
        if (canEdit) {
            Row {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Modifica", modifier = Modifier.size(16.dp), tint = Color.White.copy(alpha = 0.6f))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Elimina", modifier = Modifier.size(16.dp), tint = Color(0xFFFF6B6B))
                }
            }
        }
    }
    HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
}

private fun resolveIcon(raw: String): String {
    val trimmed = raw.trim()
    val lower = trimmed.lowercase()
    return when {
        trimmed.isEmpty() -> "🔗"
        trimmed.codePointCount(0, trimmed.length) == 1 &&
            trimmed.codePoints().findFirst().asInt > 127 -> trimmed
        lower == "instagram" -> "📷"
        lower == "facebook" -> "👤"
        lower == "telegram" -> "✈️"
        lower == "whatsapp" -> "💬"
        lower == "youtube" -> "▶️"
        lower == "email" -> "📧"
        lower == "linkedin" -> "💼"
        lower == "github" -> "💻"
        lower == "twitter" -> "🐦"
        else -> "🔗"
    }
}
