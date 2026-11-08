package it.mensa.app.features.localgroup

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ListItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold

/**
 * "Gruppi locali", for the board: the office the tab shows, for this session
 * only. The member's own office first, then every other office by name; picking
 * one switches the tab and goes back to it ([onPicked]).
 */
@Composable
fun LocalGroupOfficePickerScreen(
    onBack: () -> Unit,
    onPicked: () -> Unit,
    viewModel: LocalGroupViewModel = viewModel { LocalGroupViewModel() },
) {
    val offices by viewModel.offices.collectAsStateWithLifecycle()
    val selected by viewModel.selectedOffice.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadOffices() }
    val pick: (String?) -> Unit = { id ->
        if (id != selected) viewModel.selectOffice(id)
        onPicked()
    }

    MensaScaffold(
        topBar = { BackTopBar(title = tr("local_group.offices.title", "Gruppi locali"), onBack = onBack) },
    ) { padding ->
        when (val current = offices) {
            Loadable.Loading -> CenteredLoading(Modifier.padding(padding))
            Loadable.Failed -> CenteredMessage(
                text = tr("local_group.state.failed", "Non siamo riusciti a caricare il gruppo locale."),
                modifier = Modifier.padding(padding),
                action = tr("local_group.retry", "Riprova") to viewModel::loadOffices,
            )
            is Loadable.Loaded -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
            ) {
                item(key = "mine") {
                    OfficeChoiceRow(
                        title = tr("local_group.board.mine", "Il mio gruppo locale"),
                        supporting = null,
                        selected = selected == null,
                        onClick = { pick(null) },
                    )
                }
                items(current.value, key = { it.id }) { office ->
                    RowDivider()
                    OfficeChoiceRow(
                        title = office.name,
                        supporting = office.region.takeIf { it.isNotBlank() },
                        selected = selected == office.id,
                        onClick = { pick(office.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun OfficeChoiceRow(title: String, supporting: String?, selected: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = { RadioButton(selected = selected, onClick = null) },
        colors = cardItemColors(),
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
