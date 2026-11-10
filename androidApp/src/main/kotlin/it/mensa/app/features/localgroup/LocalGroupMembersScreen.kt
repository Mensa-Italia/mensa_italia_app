package it.mensa.app.features.localgroup

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.mensa.app.features.members.MembersRoutes
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.localgroup.ExportKind
import it.mensa.shared.localgroup.LocalGroupExportFile
import it.mensa.shared.localgroup.LocalGroupPeriod
import it.mensa.shared.localgroup.LocalGroupRules
import it.mensa.shared.model.MemberChangeKind
import it.mensa.shared.model.MemberChangeModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File

/**
 * "Soci" of the admin area: who joined or moved in ("Entrati") and who left
 * or moved out ("Usciti") in a period, and the .xlsx of either list or of
 * every active member, shared through the system sheet.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocalGroupMembersScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: LocalGroupViewModel = viewModel { LocalGroupViewModel() },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val period by viewModel.period.collectAsStateWithLifecycle()
    val changes by viewModel.changes.collectAsStateWithLifecycle()
    var arrivals by rememberSaveable { mutableStateOf(true) }
    var exporting by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val officeId = snapshot?.office?.id
    // Le segreterie vedono solo le uscite degli ultimi giorni: niente scelta del periodo.
    val exitsLocked = !arrivals && snapshot?.canSeeOldExits != true
    LaunchedEffect(exitsLocked) { viewModel.setExitsLocked(exitsLocked) }
    LaunchedEffect(officeId) { officeId?.let(viewModel::loadChanges) }
    val failedText = tr("local_group.export.failed", "Esportazione non riuscita")
    val export: (ExportKind) -> Unit = { kind ->
        scope.launch {
            exporting = true
            try {
                share(context, viewModel.export(kind, context.cacheDir))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snackbar.showSnackbar(failedText)
            } finally {
                exporting = false
            }
        }
    }

    MensaScaffold(
        snackbarHostState = snackbar,
        topBar = {
            BackTopBar(title = tr("local_group.admin.members", "Soci"), onBack = onBack) {
                if (officeId != null) ExportMenu(enabled = !exporting, arrivals = arrivals, onExport = export)
            }
        },
    ) { padding ->
        if (officeId == null) {
            CenteredLoading(Modifier.padding(padding))
            return@MensaScaffold
        }
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (exporting) LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            DirectionRow(arrivals = arrivals, onChange = { arrivals = it })
            if (exitsLocked) {
                Text(
                    tr(
                        "local_group.members.exits_window",
                        "Le uscite sono visibili solo per gli ultimi {days} giorni.",
                        "days" to LocalGroupRules.EXITS_WINDOW_DAYS,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            } else {
                PeriodRow(period = period, onChange = viewModel::selectPeriod)
            }
            when (val current = changes) {
                Loadable.Loading -> CenteredLoading()
                Loadable.Failed -> CenteredMessage(
                    text = tr("local_group.state.failed", "Non siamo riusciti a caricare il gruppo locale."),
                    action = tr("local_group.retry", "Riprova") to { viewModel.loadChanges(officeId) },
                )
                is Loadable.Loaded -> ChangesList(
                    changes = current.value.filter { it.isArrival == arrivals },
                    onMemberClick = { onNavigate(MembersRoutes.detail(it)) },
                )
            }
        }
    }
}

@Composable
private fun ExportMenu(enabled: Boolean, arrivals: Boolean, onExport: (ExportKind) -> Unit) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, enabled = enabled) {
        Icon(Icons.Outlined.Share, contentDescription = tr("local_group.export", "Esporta"))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(tr("local_group.export.changes", "Esporta l'elenco")) },
            onClick = {
                open = false
                onExport(if (arrivals) ExportKind.NEW else ExportKind.LEFT)
            },
        )
        DropdownMenuItem(
            text = { Text(tr("local_group.export.active", "Esporta tutti gli iscritti")) },
            onClick = {
                open = false
                onExport(ExportKind.ACTIVE)
            },
        )
    }
}

@Composable
private fun DirectionRow(arrivals: Boolean, onChange: (Boolean) -> Unit) {
    val options = listOf(
        true to tr("local_group.members.arrived", "Entrati"),
        false to tr("local_group.members.left", "Usciti"),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = arrivals == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label) }
        }
    }
}

@Composable
private fun PeriodRow(period: LocalGroupPeriod, onChange: (LocalGroupPeriod) -> Unit) {
    val options = listOf(
        LocalGroupPeriod.DAYS_7 to tr("local_group.period.days_7", "7 giorni"),
        LocalGroupPeriod.DAYS_30 to tr("local_group.period.days_30", "30 giorni"),
        LocalGroupPeriod.DAYS_90 to tr("local_group.period.days_90", "90 giorni"),
        LocalGroupPeriod.SEASON to tr("local_group.period.season", "Stagione"),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                selected = period == value,
                onClick = { onChange(value) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = {},
            ) { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

@Composable
private fun ChangesList(changes: List<MemberChangeModel>, onMemberClick: (String) -> Unit) {
    if (changes.isEmpty()) {
        CenteredMessage(tr("local_group.members.empty", "Nessun movimento nel periodo"))
        return
    }
    val labels = mapOf(
        MemberChangeKind.JOINED to tr("local_group.change.joined", "Nuovo socio"),
        MemberChangeKind.MOVED_IN to tr("local_group.change.moved_in", "Arrivato da altra regione"),
        MemberChangeKind.LEFT to tr("local_group.change.left", "Non rinnovato"),
        MemberChangeKind.MOVED_OUT to tr("local_group.change.moved_out", "Trasferito in altra regione"),
    )
    // Lista semplice e pigra: in una stagione i movimenti possono essere centinaia.
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)) {
        // Senza id la chiave porta anche tipo e posizione: lo stesso socio puo'
        // uscire e rientrare nello stesso giorno, e chiavi doppie fanno cadere la lista.
        itemsIndexed(
            changes,
            key = { index, change -> change.id.ifBlank { "${change.member}-${change.kind}-${change.day}-$index" } },
        ) { index, change ->
            if (index > 0) RowDivider()
            val label = change.changeKind?.let(labels::get)
            ListItem(
                headlineContent = { Text(personName(change.name)) },
                supportingContent = { Text(listOfNotNull(label, shortDate(change.dayIso)).joinToString(" · ")) },
                colors = cardItemColors(),
                modifier = Modifier.clickable(enabled = change.member.isNotBlank()) { onMemberClick(change.member) },
            )
        }
    }
}

/** Opens the system share sheet on [file], through the app's FileProvider. */
private fun share(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = LocalGroupExportFile.MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, uri)
        // Senza ClipData il foglio di condivisione non ha il permesso di leggere il file per l'anteprima.
        clipData = ClipData.newRawUri(file.name, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(send, file.name))
}
