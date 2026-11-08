package it.mensa.app.features.localgroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold

/** "Area segreteria": the office's name and the three admin pages. */
@Composable
fun LocalGroupAdminScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: LocalGroupViewModel = viewModel { LocalGroupViewModel() },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    MensaScaffold(
        topBar = { BackTopBar(title = tr("local_group.admin.title", "Area segreteria"), onBack = onBack) },
    ) { padding ->
        val office = snapshot?.office
        if (office == null) {
            CenteredLoading(Modifier.padding(padding))
            return@MensaScaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                office.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 32.dp, end = 32.dp, top = 8.dp),
            )
            LocalGroupSection(title = null) {
                AdminPageRow(Icons.Outlined.Group, tr("local_group.admin.members", "Soci")) {
                    onNavigate(LocalGroupRoutes.MEMBERS)
                }
                RowDivider()
                AdminPageRow(Icons.Outlined.BarChart, tr("local_group.admin.stats", "Statistiche")) {
                    onNavigate(LocalGroupRoutes.STATS)
                }
                RowDivider()
                AdminPageRow(Icons.Outlined.Event, tr("local_group.admin.events", "Eventi della sede")) {
                    onNavigate(LocalGroupRoutes.EVENTS)
                }
            }
        }
    }
}

@Composable
private fun AdminPageRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(label) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Chevron() },
        colors = cardItemColors(),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
