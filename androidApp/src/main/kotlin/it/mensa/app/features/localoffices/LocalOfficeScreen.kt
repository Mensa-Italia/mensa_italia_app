package it.mensa.app.features.localoffices

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.localoffices._components.LinkEditorMode
import it.mensa.app.features.localoffices._components.LocalOfficeLinkEditorSheet
import it.mensa.app.features.localoffices._components.LocalOfficeTestDateEditorSheet
import it.mensa.app.features.localoffices._components.TestDateEditorMode
import it.mensa.app.support.ExternalLinks
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.rememberAppLocale
import it.mensa.app.support.AppFormat
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeTestDateModel
import it.mensa.shared.model.SigModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import java.util.Date
import java.util.Locale
import java.util.TimeZone as JTimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalOfficeScreen(
    officeId: String,
    onBack: () -> Unit = {},
    onLinktreeClick: (String) -> Unit = {},
    onEventClick: (String) -> Unit = {},
    onSigClick: (String) -> Unit = {},
    onMemberClick: (String) -> Unit = {},
    vm: LocalOfficeViewModel = koinViewModel(parameters = { parametersOf(officeId) }),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val canEdit = remember(state) { vm.canEdit(state) }
    val context = LocalContext.current

    var showCreateTestDate by remember { mutableStateOf(false) }
    var editingTestDate by remember { mutableStateOf<LocalOfficeTestDateModel?>(null) }
    var deletingTestDate by remember { mutableStateOf<LocalOfficeTestDateModel?>(null) }
    var creatingLinkMode by remember { mutableStateOf<LinkEditorMode?>(null) }
    var editingLink by remember { mutableStateOf<LocalOfficeLinktreeRowModel?>(null) }
    var deletingLink by remember { mutableStateOf<LocalOfficeLinktreeRowModel?>(null) }
    var showPastEvents by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = state.office?.name
                            ?: tr("local_office.loading_title", fallback = "Gruppo locale"),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = tr("app.back", fallback = "Indietro"),
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when {
            state.error != null && state.office == null -> {
                Box(
                    Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        state.error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            state.office != null -> {
                val office = state.office!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState()),
                ) {
                    LocalOfficeHero(office = office)

                    // ── Linktree ────────────────────────────────────────────
                    if (state.linktree.isNotEmpty() || canEdit) {
                        SectionHeader(
                            title = tr("local_office.linktree.title", fallback = "Link utili"),
                            actionLabel = if (state.linktree.size > 2)
                                tr("local_office.linktree.see_all", fallback = "Vedi tutti")
                            else if (canEdit)
                                tr("local_office.linktree.edit", fallback = "Modifica")
                            else null,
                            onAction = { onLinktreeClick(officeId) },
                        )
                        LinktreeGroup(
                            linktree = state.linktree,
                            onLinkClick = { url ->
                                ExternalLinks.open(context, url)
                            },
                        )
                    }

                    // ── Test dates ──────────────────────────────────────────
                    if (state.testDates.isNotEmpty() || canEdit) {
                        SectionHeader(
                            title = tr(
                                "local_office.test_dates.title",
                                fallback = "Prossime sessioni di test",
                            ),
                            actionLabel = if (canEdit)
                                tr("local_office.test_dates.add", fallback = "Aggiungi")
                            else null,
                            actionIcon = if (canEdit) Icons.Outlined.Add else null,
                            onAction = { showCreateTestDate = true },
                        )
                        TestDatesGroup(
                            testDates = state.testDates,
                            canEdit = canEdit,
                            onEdit = { editingTestDate = it },
                            onDelete = { deletingTestDate = it },
                        )
                    }

                    // ── Events ──────────────────────────────────────────────
                    if (state.events.isNotEmpty()) {
                        SectionHeader(
                            title = tr("local_office.events.title", fallback = "Eventi del gruppo"),
                        )
                        EventsGroup(
                            events = state.events,
                            showPastEvents = showPastEvents,
                            onTogglePast = { showPastEvents = !showPastEvents },
                            onEventClick = onEventClick,
                        )
                    }

                    // ── SIGs ────────────────────────────────────────────────
                    if (state.sigs.isNotEmpty()) {
                        SectionHeader(
                            title = tr(
                                "local_office.sigs.title",
                                fallback = "Community del gruppo",
                            ),
                        )
                        SigsGroup(sigs = state.sigs, onSigClick = onSigClick)
                    }

                    // ── Admins ──────────────────────────────────────────────
                    if (state.admins.isNotEmpty()) {
                        SectionHeader(
                            title = tr("local_office.admins.title", fallback = "Referenti"),
                        )
                        PersonGroup(
                            people = state.admins.map {
                                PersonRowData(
                                    recordId = it.id,
                                    userId = it.user,
                                    name = it.name,
                                    image = it.image,
                                )
                            },
                            onPersonClick = onMemberClick,
                            collection = "local_office_admins",
                        )
                    }

                    // ── Assistants ──────────────────────────────────────────
                    if (state.assistants.isNotEmpty()) {
                        SectionHeader(
                            title = tr("local_office.assistants.title", fallback = "Assistenti"),
                        )
                        PersonGroup(
                            people = state.assistants.map {
                                PersonRowData(
                                    recordId = it.id,
                                    userId = it.user,
                                    name = it.name,
                                    image = it.image,
                                )
                            },
                            onPersonClick = onMemberClick,
                            collection = "local_office_assistants",
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
            else -> Box(
                Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { LoadingDots() }
        }
    }

    // ── Sheets & Dialogs ──────────────────────────────────────────────────────

    if (showCreateTestDate) {
        LocalOfficeTestDateEditorSheet(
            officeId = officeId,
            assistantsCandidates = state.assistants,
            mode = TestDateEditorMode.Create,
            onDismiss = { showCreateTestDate = false },
        )
    }

    editingTestDate?.let { td ->
        LocalOfficeTestDateEditorSheet(
            officeId = officeId,
            assistantsCandidates = state.assistants,
            mode = TestDateEditorMode.Edit(td),
            onDismiss = { editingTestDate = null },
        )
    }

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
            parentCandidates = state.linktree.filter {
                it.kind == "section" && it.id != link.id
            },
            mode = LinkEditorMode.Edit(link),
            onDismiss = { editingLink = null },
        )
    }

    deletingTestDate?.let { td ->
        AlertDialog(
            onDismissRequest = { deletingTestDate = null },
            title = {
                Text(
                    tr(
                        "local_office.test_dates.delete_confirm",
                        fallback = "Vuoi eliminare questa sessione?",
                    ),
                )
            },
            text = { Text(formatTestDate(td.date.toEpochMilliseconds(), rememberAppLocale())) },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteTestDate(td.id); deletingTestDate = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text(tr("local_office.editor.delete", fallback = "Elimina")) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTestDate = null }) {
                    Text(tr("local_office.editor.cancel", fallback = "Annulla"))
                }
            },
        )
    }

    deletingLink?.let { link ->
        AlertDialog(
            onDismissRequest = { deletingLink = null },
            title = {
                Text(
                    tr(
                        "local_office.links.delete_confirm",
                        fallback = "Vuoi eliminare questa voce?",
                    ),
                )
            },
            text = { Text(link.title) },
            confirmButton = {
                TextButton(
                    onClick = { vm.deleteLink(link.id); deletingLink = null },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
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
        if (state.office != null) {
            AlertDialog(
                onDismissRequest = vm::clearError,
                title = { Text(tr("app.error.title", fallback = "Errore")) },
                text = { Text(err) },
                confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
            )
        }
    }
}

// ─── Test dates ──────────────────────────────────────────────────────────────

@Composable
private fun TestDatesGroup(
    testDates: List<LocalOfficeTestDateModel>,
    canEdit: Boolean,
    onEdit: (LocalOfficeTestDateModel) -> Unit,
    onDelete: (LocalOfficeTestDateModel) -> Unit,
) {
    GroupedCard {
        Column {
            testDates.forEachIndexed { idx, td ->
                ListItem(
                    headlineContent = {
                        Text(formatTestDate(td.date.toEpochMilliseconds(), rememberAppLocale()))
                    },
                    supportingContent = {
                        val supporting = listOfNotNull(
                            td.location.takeIf { it.isNotBlank() },
                            td.notes.takeIf { it.isNotBlank() },
                        ).joinToString(" · ")
                        if (supporting.isNotEmpty()) {
                            Text(
                                supporting,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (td.maxParticipants > 0) {
                                AssistChip(
                                    onClick = {},
                                    enabled = false,
                                    label = {
                                        Text(
                                            // `{n}`, not a Kotlin template: the push
                                            // extractor ships the fallback verbatim, so a
                                            // template reaches Tolgee as literal text.
                                            tr(
                                                "local_office.test_dates.max_short",
                                                "max {n}",
                                                "n" to td.maxParticipants,
                                            ),
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurface,
                                    ),
                                )
                            }
                            if (canEdit) {
                                IconButton(onClick = { onEdit(td) }) {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = tr(
                                            "local_office.editor.edit",
                                            fallback = "Modifica",
                                        ),
                                    )
                                }
                                IconButton(onClick = { onDelete(td) }) {
                                    Icon(
                                        Icons.Outlined.Delete,
                                        contentDescription = tr(
                                            "local_office.editor.delete",
                                            fallback = "Elimina",
                                        ),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                )
                if (idx < testDates.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}

// ─── Events ──────────────────────────────────────────────────────────────────

@Composable
private fun EventsGroup(
    events: List<EventModel>,
    showPastEvents: Boolean,
    onTogglePast: () -> Unit,
    onEventClick: (String) -> Unit,
) {
    val nowMs = System.currentTimeMillis()
    val upcoming = events.filter { it.whenEnd.toEpochMilliseconds() >= nowMs }
    val past = events.filter { it.whenEnd.toEpochMilliseconds() < nowMs }
    val visible = upcoming + (if (showPastEvents) past else emptyList())

    GroupedCard {
        Column {
            visible.forEachIndexed { idx, ev ->
                val isPast = ev.whenEnd.toEpochMilliseconds() < nowMs
                ListItem(
                    headlineContent = {
                        Text(
                            ev.name,
                            color = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Event,
                            contentDescription = null,
                            tint = if (isPast) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onEventClick(ev.id) },
                )
                if (idx < visible.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }

    if (past.isNotEmpty()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            TextButton(onClick = onTogglePast) {
                Text(
                    if (showPastEvents)
                        tr("local_office.events.hide_past", fallback = "Nascondi eventi passati")
                    else
                        // The count outside the text: the key is shared with iOS,
                        // whose label has none.
                        tr("local_office.events.show_past", fallback = "Mostra eventi passati") +
                            " (${past.size})",
                )
            }
        }
    }
}

// ─── SIGs ────────────────────────────────────────────────────────────────────

@Composable
private fun SigsGroup(sigs: List<SigModel>, onSigClick: (String) -> Unit) {
    GroupedCard {
        Column {
            sigs.forEachIndexed { idx, sig ->
                ListItem(
                    headlineContent = { Text(sig.name) },
                    leadingContent = {
                        Icon(
                            Icons.Outlined.Group,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    modifier = Modifier.clickable { onSigClick(sig.id) },
                )
                if (idx < sigs.lastIndex) {
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}

/**
 * Test dates are stored as a UTC wall-clock instant, so the formatter is pinned
 * to UTC — rendering them in the device zone would shift the published hour.
 */
fun formatTestDate(ms: Long, locale: Locale): String {
    val fmt = AppFormat.formatter(AppFormat.Skeleton.FULL_WITH_TIME, locale)
    fmt.timeZone = JTimeZone.getTimeZone("UTC")
    return fmt.format(Date(ms))
}
