package it.mensa.app.features.events

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.FilterListOff
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.events._components.EventFiltersSheet
import it.mensa.app.features.events._components.EventRowCard
import it.mensa.app.features.events.util.EventFilterState
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import org.koin.androidx.compose.koinViewModel

/**
 * EventListScreen — M3 Expressive restyled.
 * MensaTopAppBar (Large) con kicker "VITA MENSA" + stagger entrance animations.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventListScreen(
    onEventClick: (String) -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onMapClick: () -> Unit = {},
    onAddClick: () -> Unit = {},
    onBack: () -> Unit = {},
    vm: EventListViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFilters by remember { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val listState = rememberLazyListState()

    val upcoming = vm.upcoming(state)
    val past = vm.past(state)

    LaunchedEffect(state.error) {
        state.error?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            vm.clearError()
        }
    }

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("events.list.title", fallback = "Eventi"),
                kicker = tr("events.list.kicker", fallback = "VITA MENSA"),
                scrollBehavior = scrollBehavior,
                query = state.query,
                onQueryChange = vm::setQuery,
                searchPlaceholder = tr("events.search.hint", fallback = "Cerca eventi"),
                onBack = onBack,
                searchContentDescription = tr("events.search.hint", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
                extraActions = {
                    BadgedBox(
                        badge = {
                            if (!state.filter.isEmpty) Badge { Text("${state.filter.activeCount}") }
                        }
                    ) {
                        IconButton(onClick = { showFilters = true }) {
                            Icon(
                                imageVector = if (state.filter.isEmpty) Icons.Outlined.FilterList else Icons.Outlined.FilterListOff,
                                contentDescription = tr("events.filter.label", fallback = "Filtri"),
                            )
                        }
                    }
                    IconButton(onClick = onMapClick) {
                        Icon(Icons.Outlined.Map, contentDescription = tr("events.map.label", fallback = "Mappa"))
                    }
                    IconButton(onClick = onCalendarClick) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = tr("events.calendar.label", fallback = "Calendario"))
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = state.canAddEvent,
                enter = fadeIn() + slideInVertically(animationSpec = spring()),
            ) {
                ExtendedFloatingActionButton(
                    text = { Text(tr("events.add.label", fallback = "Nuovo evento")) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = onAddClick,
                )
            }
        },
        snackbarHostState = snackbarHostState,
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = { vm.refresh() },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    state.loading && state.events.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            LoadingDots()
                        }
                    }
                    state.events.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                tr("events.empty.body", fallback = "Nessun evento disponibile."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    upcoming.isEmpty() && past.isEmpty() -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                tr("events.filter.empty", fallback = "Nessun risultato. Prova a cambiare filtro."),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            contentPadding = PaddingValues(bottom = 80.dp, top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            if (upcoming.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(tr("events.section.upcoming", fallback = "Imminenti"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                    }
                                }
                                itemsIndexed(upcoming, key = { _, e -> e.id }) { index, event ->
                                    EventRowCard(
                                        event = event,
                                        onClick = { onEventClick(event.id) },
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .fillMaxWidth(),
                                    )
                                }
                            }
                            if (past.isNotEmpty()) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(tr("events.section.past", fallback = "Passati"), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                    }
                                }
                                itemsIndexed(past, key = { _, e -> e.id }) { index, event ->
                                    EventRowCard(
                                        event = event,
                                        onClick = { onEventClick(event.id) },
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp)
                                            .fillMaxWidth(),
                                    )
                                }
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }

        if (showFilters) {
            EventFiltersSheet(
                state = state.filter,
                onApply = { newFilter ->
                    vm.setFilter(newFilter)
                    showFilters = false
                },
                onDismiss = { showFilters = false },
            )
        }
    }
}
