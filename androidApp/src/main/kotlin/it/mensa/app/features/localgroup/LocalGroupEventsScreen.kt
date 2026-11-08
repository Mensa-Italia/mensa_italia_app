package it.mensa.app.features.localgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.mensa.app.features.events.EventRoutes
import it.mensa.app.features.events._components.EventRowCard
import it.mensa.app.features.events._components.EventSectionHeader
import it.mensa.app.features.events.util.EventDateFormatter
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.EventModel

/**
 * "Eventi della sede": the office's events to come and past ones. Editing and
 * deleting are on the event's page, which offers them to the office's admins;
 * "+" opens the existing new-event page.
 */
@Composable
fun LocalGroupEventsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: LocalGroupViewModel = viewModel { LocalGroupViewModel() },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val officeId = snapshot?.office?.id
    LaunchedEffect(officeId) { officeId?.let(viewModel::loadEvents) }

    MensaScaffold(
        topBar = {
            BackTopBar(title = tr("local_group.admin.events", "Eventi della sede"), onBack = onBack) {
                IconButton(onClick = { onNavigate(EventRoutes.ADD) }) {
                    Icon(Icons.Outlined.Add, contentDescription = tr("local_group.events.new", "Nuovo evento"))
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        if (officeId == null) {
            CenteredLoading(content)
            return@MensaScaffold
        }
        when (val current = events) {
            Loadable.Loading -> CenteredLoading(content)
            Loadable.Failed -> CenteredMessage(
                text = tr("local_group.state.failed", "Non siamo riusciti a caricare il gruppo locale."),
                modifier = content,
                action = tr("local_group.retry", "Riprova") to { viewModel.loadEvents(officeId) },
            )
            is Loadable.Loaded -> if (current.value.isEmpty()) {
                CenteredMessage(tr("local_group.events.empty", "Nessun evento della sede"), content)
            } else {
                EventsList(current.value, content, onNavigate)
            }
        }
    }
}

@Composable
private fun EventsList(events: List<EventModel>, modifier: Modifier, onNavigate: (String) -> Unit) {
    // Un evento in corso resta fra i prossimi: passato vuol dire finito.
    val (past, upcoming) = events.partition(EventDateFormatter::isPast)
    val upcomingTitle = tr("local_group.events.upcoming", "Prossimi")
    val pastTitle = tr("local_group.events.past", "Passati")
    // Come EventListScreen: intestazione di sezione e una EventRowCard per evento.
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp, top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        eventSection("upcoming", upcomingTitle, upcoming.sortedBy { it.whenStart }, onNavigate)
        eventSection("past", pastTitle, past.sortedByDescending { it.whenStart }, onNavigate)
    }
}

private fun LazyListScope.eventSection(key: String, title: String, events: List<EventModel>, onNavigate: (String) -> Unit) {
    if (events.isEmpty()) return
    item(key = key) { EventSectionHeader(title) }
    items(events, key = { it.id }) { event ->
        EventRowCard(
            event = event,
            onClick = { onNavigate(EventRoutes.detail(event.id)) },
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
        )
    }
}
