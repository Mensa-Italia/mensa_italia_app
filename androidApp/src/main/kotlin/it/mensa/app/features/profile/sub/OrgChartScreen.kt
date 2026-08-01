package it.mensa.app.features.profile.sub

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            // "Tavola rotonda": nessuna hero — tutti hanno la stessa card a
            // griglia (2 colonne), i responsabili sono solo ordinati per primi
            // e distinti da bordo accent + badge dentro la card.
            val ordered = group.members.filter { it.isMaster } +
                group.members.filter { !it.isMaster }
            ordered.chunked(2).forEach { rowMembers ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rowMembers.forEach { member ->
                        OrgMemberCard(
                            member = member,
                            onClick = {},
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowMembers.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * Card unica per TUTTI i membri: foto del socio come background con scrim
 * graduale (contrasto del testo garantito), gradient brand + iniziali in
 * fallback. I responsabili (is_master) si distinguono solo per bordo accent
 * e badge — stessa dimensione di tutti gli altri.
 */
@Composable
private fun OrgMemberCard(
    member: OrgChartMember,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(18.dp)

    Box(
        modifier = modifier
            .height(168.dp)
            .clip(shape)
            .border(
                width = if (member.isMaster) 2.dp else 1.dp,
                color = if (member.isMaster) {
                    colorScheme.primary
                } else {
                    colorScheme.outlineVariant.copy(alpha = 0.4f)
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .alpha(if (member.inactive) 0.55f else 1f),
    ) {
        // Background: foto (thumb 0x500, lo stesso del dettaglio socio) o
        // gradient brand con iniziali.
        val photo = member.image.takeIf {
            it.isNotEmpty() && !it.contains("cloud32.it/Associazioni/img/Uomo-1.png")
        }
        if (photo != null) {
            val url = if (photo.startsWith("http")) {
                photo
            } else {
                FilesUrl.build(
                    collection = "members_registry",
                    recordId = member.userId,
                    filename = photo,
                    thumb = "0x500",
                )
            }
            CachedAsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Brush.linearGradient(listOf(colorScheme.primary, MensaCyan))),
                contentAlignment = Alignment.Center,
            ) {
                val initials = member.name
                    .split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                Text(
                    text = initials.ifEmpty { "?" },
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White.copy(alpha = 0.35f),
                )
            }
        }

        // Scrim graduale, sempre presente: testo bianco leggibile su qualsiasi foto.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.02f),
                        0.5f to Color.Black.copy(alpha = 0.30f),
                        1f to Color.Black.copy(alpha = 0.72f),
                    )
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp),
        ) {
            Text(
                member.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (member.role.isNotEmpty()) {
                Text(
                    member.role,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (member.isMaster) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .background(colorScheme.primary, RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Filled.Star,
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = Color.White,
                )
                Spacer(Modifier.width(3.dp))
                Text(
                    tr("app.org_chart.master_badge", fallback = "Responsabile").uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.3.sp,
                    color = Color.White,
                )
            }
        }

        if (member.inactive) {
            Text(
                tr("app.org_chart.inactive_badge", fallback = "Dimissionario").uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
                    .padding(horizontal = 7.dp, vertical = 3.dp),
            )
        }
    }
}
