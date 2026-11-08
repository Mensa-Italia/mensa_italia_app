package it.mensa.app.features.localgroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.LocalOfficeStatsModel

/**
 * "Associazione", for the board: the association's figures and months, the
 * same blocks as an office's "Statistiche", then every office side by side.
 * Tapping an office shows it in the tab ([onOfficePicked]).
 */
@Composable
fun LocalGroupAssociationScreen(
    onBack: () -> Unit,
    onOfficePicked: () -> Unit,
    viewModel: LocalGroupViewModel = viewModel { LocalGroupViewModel() },
) {
    val stats by viewModel.associationStats.collectAsStateWithLifecycle()
    val monthly by viewModel.associationMonthly.collectAsStateWithLifecycle()
    val officeStats by viewModel.officeStats.collectAsStateWithLifecycle()
    val offices by viewModel.officeNames.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.loadAssociationStats()
        viewModel.loadAssociationMonthly()
        viewModel.loadAllOfficeStats()
        viewModel.loadOfficeNames()
    }
    val retry: () -> Unit = {
        viewModel.loadAllOfficeStats()
        viewModel.loadOfficeNames()
    }

    MensaScaffold(
        topBar = { BackTopBar(title = tr("local_group.association.title", "Associazione"), onBack = onBack) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            (stats as? Loadable.Loaded)?.value?.let { figures ->
                item(key = "figures") {
                    StatsFiguresCard(figures, membersLabel = tr("local_group.association.members", "Soci attivi"))
                }
            }
            monthlyStatsItems(monthly, onRetry = viewModel::loadAssociationMonthly)
            byOfficeItems(
                officeStats = officeStats,
                offices = offices,
                onRetry = retry,
                onOfficeClick = { id ->
                    viewModel.selectOffice(id)
                    onOfficePicked()
                },
            )
        }
    }
}

/** "Per gruppo locale": each office's members, events and attendance in the season, most members first. */
private fun LazyListScope.byOfficeItems(
    officeStats: Loadable<List<LocalOfficeStatsModel>>,
    offices: Loadable<Map<String, String>>,
    onRetry: () -> Unit,
    onOfficeClick: (String) -> Unit,
) {
    if (officeStats is Loadable.Failed || offices is Loadable.Failed) {
        item(key = "by_office_failed") {
            CenteredMessage(
                text = tr("local_group.state.failed", "Non siamo riusciti a caricare il gruppo locale."),
                action = tr("local_group.retry", "Riprova") to onRetry,
            )
        }
        return
    }
    if (officeStats !is Loadable.Loaded || offices !is Loadable.Loaded) {
        item(key = "by_office_loading") { CenteredLoading(Modifier.padding(top = 48.dp)) }
        return
    }
    val names = offices.value
    val rows = officeStats.value.sortedByDescending { it.membersActive }
    if (rows.isEmpty()) return
    item(key = "by_office") {
        LocalGroupSection(title = tr("local_group.association.by_office", "Per gruppo locale")) {
            rows.forEachIndexed { index, row ->
                if (index > 0) RowDivider()
                OfficeFiguresRow(
                    name = names[row.localOffice] ?: row.regionKey.ifBlank { row.localOffice },
                    stats = row,
                    onClick = { onOfficeClick(row.localOffice) },
                )
            }
        }
    }
}

@Composable
private fun OfficeFiguresRow(name: String, stats: LocalOfficeStatsModel, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(name) },
        supportingContent = {
            Text(
                tr(
                    "local_group.association.office_figures",
                    "{members} soci · {events} eventi · {attendance} presenze",
                    "members" to stats.membersActive,
                    "events" to stats.eventsSeason,
                    "attendance" to stats.attendanceSeason,
                ),
            )
        },
        trailingContent = { Chevron() },
        colors = cardItemColors(),
        modifier = Modifier.clickable(enabled = stats.localOffice.isNotBlank(), onClick = onClick),
    )
}
