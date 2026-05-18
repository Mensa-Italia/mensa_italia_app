package it.mensa.app.features.receipts

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.receipts._components.ReceiptRowCard
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import androidx.compose.material3.LargeTopAppBar
import org.koin.androidx.compose.koinViewModel

/**
 * ReceiptsListScreen — M3 Expressive restyled.
 * MensaTopAppBar Large con kicker "LE MIE RICEVUTE", ReceiptRowCard con MensaCard,
 * stagger entrance animations, amount right-aligned bold.
 *
 * IconBadge discipline: Primary (pagato), Tertiary (pendente), Cyan (info)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptsListScreen(
    onNavigateToDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val vm: ReceiptsListViewModel = koinViewModel()
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MensaScaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(tr("receipts.title", fallback = "Le mie ricevute")) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        when {
            uiState.loading && uiState.receipts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { LoadingDots() }
            }

            uiState.receipts.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) { EmptyReceiptsPlaceholder() }
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = uiState.loading,
                    onRefresh = vm::refresh,
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    ) {
                        itemsIndexed(
                            items = uiState.receipts,
                            key = { _, receipt -> receipt.id },
                        ) { index, receipt ->
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
                                ReceiptRowCard(
                                    receipt = receipt,
                                    modifier = Modifier
                                        .fillParentMaxWidth()
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null,
                                        ) { onNavigateToDetail(receipt.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyReceiptsPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Receipt,
            contentDescription = null,
            modifier = Modifier.padding(bottom = 4.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
        Text(
            text = tr("receipts.empty.title", fallback = "Nessuna ricevuta"),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = tr("receipts.empty.body", fallback = "Le tue ricevute appariranno qui"),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
