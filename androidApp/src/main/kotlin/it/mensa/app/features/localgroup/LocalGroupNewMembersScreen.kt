package it.mensa.app.features.localgroup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import it.mensa.app.features.members.MembersRoutes
import it.mensa.app.features.members._components.MemberCellCompact
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.model.RegSociModel

/**
 * "Nuovi soci": the members who joined the office in the last week, open to
 * every member of the tab. A row opens the member's card.
 */
@Composable
fun LocalGroupNewMembersScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: LocalGroupViewModel = viewModel { LocalGroupViewModel() },
) {
    val snapshot by viewModel.snapshot.collectAsStateWithLifecycle()
    val members by viewModel.newMembers.collectAsStateWithLifecycle()
    val officeId = snapshot?.office?.id
    LaunchedEffect(officeId) { officeId?.let(viewModel::loadNewMembers) }

    MensaScaffold(
        topBar = { BackTopBar(title = tr("local_group.new_members.title", "Nuovi soci"), onBack = onBack) },
    ) { padding ->
        val modifier = Modifier.padding(padding)
        if (officeId == null) {
            CenteredLoading(modifier)
            return@MensaScaffold
        }
        when (val current = members) {
            Loadable.Loading -> CenteredLoading(modifier)
            Loadable.Failed -> CenteredMessage(
                text = tr("local_group.state.failed", "Non siamo riusciti a caricare il gruppo locale."),
                modifier = modifier,
                action = tr("local_group.retry", "Riprova") to { viewModel.loadNewMembers(officeId) },
            )
            is Loadable.Loaded -> NewMembersList(
                members = current.value,
                modifier = modifier,
                onMemberClick = { onNavigate(MembersRoutes.detail(it)) },
            )
        }
    }
}

@Composable
private fun NewMembersList(members: List<RegSociModel>, modifier: Modifier, onMemberClick: (String) -> Unit) {
    if (members.isEmpty()) {
        CenteredMessage(tr("local_group.new_members.empty", "Nessun nuovo socio nell'ultima settimana"), modifier)
        return
    }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)) {
        itemsIndexed(members, key = { _, member -> member.id }) { index, member ->
            if (index > 0) RowDivider()
            MemberCellCompact(
                member = member,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMemberClick(member.id) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
