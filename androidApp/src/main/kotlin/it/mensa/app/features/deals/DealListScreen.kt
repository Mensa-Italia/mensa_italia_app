package it.mensa.app.features.deals

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.deals._components.DealCardView
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import org.koin.androidx.compose.koinViewModel

/**
 * DealListScreen — M3 Expressive restyled.
 * MensaTopAppBar Large con kicker "CONVENZIONI", DealCardView con MensaCard, stagger animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DealListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAdd: () -> Unit,
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DealListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showSectorMenu by remember { mutableStateOf(false) }

    if (state.error != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(state.error ?: "") },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) { Text("OK") }
            }
        )
    }

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("addons.deals.title", fallback = "Deal & Convenzioni"),
                scrollBehavior = scrollBehavior,
                query = state.searchText,
                onQueryChange = { viewModel.setSearchText(it) },
                searchPlaceholder = tr("app.deals.search_placeholder", fallback = "Cerca convenzioni"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
                extraActions = {
                    if (state.sectors.size > 1) {
                        Box {
                            IconButton(onClick = { showSectorMenu = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.FilterList,
                                    contentDescription = tr("app.deals.filter.label", fallback = "Filtra per settore"),
                                    tint = if (state.selectedSector != null)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            DropdownMenu(
                                expanded = showSectorMenu,
                                onDismissRequest = { showSectorMenu = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text(tr("app.deals.filter.all", fallback = "Tutti")) },
                                    onClick = {
                                        viewModel.setSelectedSector(null)
                                        showSectorMenu = false
                                    },
                                    trailingIcon = if (state.selectedSector == null) ({
                                        Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }) else null,
                                )
                                state.sectors.forEach { sector ->
                                    DropdownMenuItem(
                                        text = { Text(sector) },
                                        onClick = {
                                            viewModel.setSelectedSector(sector)
                                            showSectorMenu = false
                                        },
                                        trailingIcon = if (state.selectedSector == sector) ({
                                            Icon(Icons.Outlined.FilterList, contentDescription = null, modifier = Modifier.size(16.dp))
                                        }) else null,
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.canAddDeal,
                enter = fadeIn() + slideInVertically(animationSpec = spring()),
            ) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAdd,
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(tr("addons.deals.add", fallback = "Aggiungi deal")) },
                )
            }
        },
    ) { innerPadding ->
        val pullRefreshState = rememberPullToRefreshState()

        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.refresh() },
            state = pullRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sector chips row
                if (state.sectors.size > 1) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        item {
                            FilterChip(
                                selected = state.selectedSector == null,
                                onClick = { viewModel.setSelectedSector(null) },
                                label = { Text(tr("app.deals.filter.all", fallback = "Tutti")) },
                            )
                        }
                        items(state.sectors.size) { index ->
                            val sector = state.sectors[index]
                            FilterChip(
                                selected = state.selectedSector == sector,
                                onClick = {
                                    viewModel.setSelectedSector(
                                        if (state.selectedSector == sector) null else sector
                                    )
                                },
                                label = { Text(sector) },
                            )
                        }
                    }
                }

                // Main content
                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        state.allDeals.isEmpty() && state.refreshing -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LoadingDots()
                            }
                        }

                        state.allDeals.isEmpty() -> {
                            EmptyDealsPlaceholder(
                                message = tr("app.deals.empty", fallback = "Nessun deal"),
                                description = tr("app.deals.empty_description", fallback = "Non ci sono deal disponibili al momento."),
                                isSearch = false,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        state.filteredDeals.isEmpty() && state.isFilterActive -> {
                            EmptyDealsPlaceholder(
                                message = tr("app.deals.no_results", fallback = "Nessun deal trovato"),
                                description = tr("app.deals.no_results_description", fallback = "Prova a cambiare filtro o ricerca."),
                                isSearch = true,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }

                        else -> {
                            LazyColumn(
                                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(
                                    items = state.filteredDeals,
                                    key = { _, deal -> deal.id },
                                ) { _, deal ->
                                    DealCardView(
                                        deal = deal,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .animateItem(),
                                        onClick = { onNavigateToDetail(deal.id) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDealsPlaceholder(
    message: String,
    description: String,
    isSearch: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.LocalOffer,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp, start = 32.dp, end = 32.dp),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp, start = 32.dp, end = 32.dp),
        )
    }
}
