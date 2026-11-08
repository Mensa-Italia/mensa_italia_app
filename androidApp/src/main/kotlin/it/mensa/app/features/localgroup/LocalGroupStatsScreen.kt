package it.mensa.app.features.localgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.mensa.app.support.rememberAppLocale
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.LocalOfficeMonthlyStatsModel
import it.mensa.shared.model.LocalOfficeStatsModel

/**
 * "Statistiche" of the admin area: the office's figures now, then month by
 * month from October of the previous season (events, attendance, active
 * members, joined and left).
 */
@Composable
fun LocalGroupStatsScreen(
    onBack: () -> Unit,
    viewModel: LocalGroupViewModel = viewModel { LocalGroupViewModel() },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val monthly by viewModel.monthly.collectAsStateWithLifecycle()
    val officeId = snapshot?.office?.id
    LaunchedEffect(officeId) { officeId?.let(viewModel::loadMonthly) }

    MensaScaffold(
        topBar = { BackTopBar(title = tr("local_group.admin.stats", "Statistiche"), onBack = onBack) },
    ) { padding ->
        if (officeId == null) {
            CenteredLoading(Modifier.padding(padding))
            return@MensaScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            snapshot?.stats?.let { stats -> item(key = "figures") { FiguresCard(stats) } }
            when (val current = monthly) {
                Loadable.Loading -> item(key = "loading") { CenteredLoading(Modifier.padding(top = 48.dp)) }
                Loadable.Failed -> item(key = "failed") {
                    CenteredMessage(
                        text = tr("local_group.state.failed", "Non siamo riusciti a caricare il gruppo locale."),
                        action = tr("local_group.retry", "Riprova") to { viewModel.loadMonthly(officeId) },
                    )
                }
                is Loadable.Loaded -> if (current.value.isEmpty()) {
                    item(key = "empty") {
                        CenteredMessage(tr("local_group.stats.empty", "Ancora nessun dato per questa stagione"))
                    }
                } else {
                    item(key = "charts") { MonthlyCharts(current.value) }
                }
            }
        }
    }
}

@Composable
private fun FiguresCard(stats: LocalOfficeStatsModel) {
    val figures = listOf(
        tr("local_group.stats.members_active", "Iscritti in regione") to stats.membersActive,
        tr("local_group.stats.new_30", "Nuovi (30 giorni)") to stats.membersNew30d,
        tr("local_group.stats.left_30", "Usciti (30 giorni)") to stats.membersLeft30d,
        tr("local_group.stats.events_season", "Eventi in stagione") to stats.eventsSeason,
        tr("local_group.stats.attendance_season", "Presenze in stagione") to stats.attendanceSeason,
        tr("local_group.stats.deals", "Convenzioni attive") to stats.dealsActive,
    )
    // I totali sono della stagione in corso: il titolo e' la stagione.
    LocalGroupSection(title = stats.season.takeIf { it.isNotBlank() }) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            figures.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { (label, value) -> Figure(label, value, Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun Figure(label: String, value: Int, modifier: Modifier) {
    Column(modifier) {
        Text(value.toString(), style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MonthlyCharts(months: List<LocalOfficeMonthlyStatsModel>) {
    val locale = rememberAppLocale()
    val labels = monthLabels(months, locale)
    val primary = MaterialTheme.colorScheme.primary
    // I grafici partono dall'ottobre della stagione prima: il titolo dice da quando.
    val start = months.first().takeIf { it.year > 0 && it.monthNumber > 0 }?.let { monthAndYear(it, locale) }
    Column {
        if (start != null) SectionTitle(tr("local_group.stats.since", "Da {month}", "month" to start))
        ChartCard(tr("local_group.stats.events", "Eventi per mese")) {
            MonthlyBars(months.map { it.events }, labels, primary)
        }
        ChartCard(tr("local_group.stats.attendance", "Presenze per mese")) {
            MonthlyBars(months.map { it.attendance }, labels, MaterialTheme.colorScheme.tertiary)
        }
        ChartCard(tr("local_group.stats.members", "Soci attivi")) {
            MonthlyLine(months.map { it.membersActive }, labels)
        }
        ChartCard(tr("local_group.stats.flows", "Entrati e usciti")) {
            MonthlyPairedBars(
                a = months.map { it.membersNew },
                b = months.map { it.membersLeft },
                labels = labels,
                labelA = tr("local_group.members.arrived", "Entrati"),
                labelB = tr("local_group.members.left", "Usciti"),
            )
        }
    }
}

@Composable
private fun ChartCard(title: String, chart: @Composable () -> Unit) {
    LocalGroupSection(title = title) {
        Column(Modifier.padding(16.dp)) { chart() }
    }
}

