package it.mensa.app.features.profile.sub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AssistantPhoto
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import it.mensa.app.ui.theme.MensaCyan
import it.mensa.shared.model.OrgChartGroup
import it.mensa.shared.model.OrgChartMember
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrgChartScreen(
    onBack: () -> Unit,
    vm: OrgChartViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val filteredGroups = vm.filteredGroups()
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("app.org_chart.title", fallback = "Organigramma"),
                kicker = tr("app.org_chart.kicker", fallback = "MENSA ITALIA"),
                scrollBehavior = scrollBehavior,
                query = uiState.searchQuery,
                onQueryChange = vm::onSearchChange,
                searchPlaceholder = tr("app.org_chart.search_placeholder", fallback = "Cerca un gruppo"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when {
                uiState.loading && uiState.groups.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        if (filteredGroups.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        "Nessun risultato per \"${uiState.searchQuery}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(filteredGroups, key = { it.id }) { group ->
                                OrgGroupSection(group = group, vm = vm)
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    tr("app.org_chart.footer", fallback = "Aggiornato dal Consiglio Direttivo"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }

    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = vm::dismissError,
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = vm::dismissError) { Text("OK") } },
        )
    }
}

@Composable
private fun OrgGroupSection(group: OrgChartGroup, vm: OrgChartViewModel) {
    var expanded by remember { mutableStateOf(true) }
    val allInactive = group.members.all { it.inactive }
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp),
    ) {
        // Section header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(22.dp)
                    .then(
                        if (allInactive) Modifier.background(
                            colorScheme.outline,
                            RoundedCornerShape(50),
                        ) else Modifier.background(
                            Brush.verticalGradient(listOf(colorScheme.primary, MensaCyan)),
                            RoundedCornerShape(50),
                        )
                    ),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                vm.localizedTitle(group.title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = if (allInactive) colorScheme.onSurfaceVariant else colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .background(colorScheme.surfaceContainerHighest, RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    "${group.members.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(4.dp))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
            )
        }

        if (expanded) {
            group.members.filter { it.isMaster }.forEach { member ->
                OrgMemberHeroRow(member = member, onClick = {})
            }
            group.members.filter { !it.isMaster }.forEach { member ->
                OrgMemberRow(member = member, onClick = {})
            }
        }
    }
}

@Composable
private fun OrgMemberHeroRow(member: OrgChartMember, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(member = member, size = 48)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                member.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = if (member.inactive) colorScheme.onSurfaceVariant else colorScheme.onSurface,
            )
            if (member.role.isNotEmpty()) {
                Text(
                    member.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.primary,
                )
            }
        }
        AssistChip(onClick = {}, label = {
            Text(
                tr("app.org_chart.master_badge", fallback = "Capo"),
                style = MaterialTheme.typography.labelSmall,
            )
        })
    }
}

@Composable
private fun OrgMemberRow(member: OrgChartMember, onClick: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MemberAvatar(member = member, size = 36)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                member.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (member.inactive) colorScheme.onSurfaceVariant else colorScheme.onSurface,
            )
            if (member.role.isNotEmpty()) {
                Text(
                    member.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MemberAvatar(member: OrgChartMember, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MensaCyan))),
        contentAlignment = Alignment.Center,
    ) {
        if (member.image.isNotEmpty()) {
            val url = if (member.image.startsWith("http")) {
                member.image
            } else {
                FilesUrl.build(collection = "members_registry", recordId = member.userId, filename = member.image)
            }
            CachedAsyncImage(
                model = url,
                contentDescription = member.name,
                modifier = Modifier
                    .size(size.dp)
                    .clip(CircleShape),
            )
        } else {
            val initials = member.name
                .split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .joinToString("") { it.first().uppercase() }
            Text(
                text = initials.ifEmpty { "?" },
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
        }
    }
}
