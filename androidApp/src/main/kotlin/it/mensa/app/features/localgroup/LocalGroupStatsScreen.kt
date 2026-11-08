package it.mensa.app.features.localgroup

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold

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
            snapshot?.stats?.let { stats -> item(key = "figures") { StatsFiguresCard(stats) } }
            monthlyStatsItems(monthly, onRetry = { viewModel.loadMonthly(officeId) })
        }
    }
}
