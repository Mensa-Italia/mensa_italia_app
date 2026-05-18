package it.mensa.app.features.tickets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import it.mensa.app.features.tickets._components.TicketRowCard
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import androidx.compose.material3.LargeTopAppBar
import org.koin.androidx.compose.koinViewModel

/**
 * TicketsListScreen — M3 Expressive restyled.
 * MensaTopAppBar Large con kicker "I MIEI TICKET", FilterChip status (Tutti/Attivi/Usati/Scaduti),
 * TicketRowCard con MensaCard, stagger entrance animations.
 *
 * IconBadge discipline: Primary (attivo), Tertiary (usato), Cyan (info)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketsListScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: TicketsListViewModel = koinViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Status filter: Tutti / Attivi / Usati / Scaduti
    var selectedFilter by remember { mutableStateOf(TicketStatusFilter.Tutti) }

    val filteredTickets = when (selectedFilter) {
        TicketStatusFilter.Tutti -> uiState.tickets
        TicketStatusFilter.Attivi -> uiState.tickets.filter { it.statusComputed == TicketStatus.Pending }
        TicketStatusFilter.Completati -> uiState.tickets.filter { it.statusComputed == TicketStatus.Completed }
        TicketStatusFilter.Falliti -> uiState.tickets.filter { it.statusComputed == TicketStatus.Failed }
    }

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(tr("tickets.title", fallback = "I miei ticket")) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->

        when {
            uiState.loading && uiState.tickets.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { LoadingDots() }
            }

            uiState.tickets.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { EmptyTicketsPlaceholder() }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.loading,
                    onRefresh = vm::refresh,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Filter chips: Tutti / Attivi / Completati / Falliti
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            items(TicketStatusFilter.values().size) { idx ->
                                val f = TicketStatusFilter.values()[idx]
                                FilterChip(
                                    selected = selectedFilter == f,
                                    onClick = { selectedFilter = f },
                                    label = { Text(f.label) },
                                )
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                        ) {
                            itemsIndexed(
                                items = filteredTickets,
                                key = { _, ticket -> ticket.id },
                            ) { index, ticket ->
                                val interactionSource = remember { MutableInteractionSource() }
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(tween(300, delayMillis = index * 60)) +
                                        slideInVertically(
                                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 380f),
                                            initialOffsetY = { it / 8 },
                                        ),
                                    exit = fadeOut(),
                                ) {
                                    TicketRowCard(
                                        ticket = ticket,
                                        modifier = Modifier
                                            .fillParentMaxWidth()
                                            .clickable(
                                                interactionSource = interactionSource,
                                                indication = null,
                                            ) { onNavigateToDetail(ticket.id) },
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

// ─── Status filter enum ────────────────────────────────────────────────────────

enum class TicketStatusFilter(val label: String) {
    Tutti("Tutti"),
    Attivi("Attivi"),
    Completati("Completati"),
    Falliti("Falliti"),
}

@Composable
private fun EmptyTicketsPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.ConfirmationNumber,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = tr("tickets.empty.title", fallback = "Nessun ticket"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = tr("tickets.empty.body", fallback = "I tuoi ticket appariranno qui"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
