package it.mensa.app.features.addonshub

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.*
import it.mensa.app.ui.theme.*
import it.mensa.shared.model.AddonModel
import org.koin.androidx.compose.koinViewModel

/**
 * AddonsHubScreen — grid of addons available to the current user.
 * M3 Expressive restyling: MensaTopAppBar with kicker, MensaCard tiles,
 * stagger entrance, IconBadge Cyan default with rotating Primary/Tertiary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddonsHubScreen(
    onAddonClick: (addonId: String) -> Unit = {},
    onBack: () -> Unit = {},
    vm: AddonsHubViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val visible = vm.visibleAddons()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Stagger entrance
    val visibleIndices = remember { mutableStateListOf<Int>() }
    LaunchedEffect(visible) {
        visible.forEachIndexed { idx, _ ->
            if (idx !in visibleIndices) {
                kotlinx.coroutines.delay(minOf(idx, 12) * 60L)
                visibleIndices.add(idx)
            }
        }
    }

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaTopAppBar(
                title = tr("addons.hub.title", fallback = "Addon"),
                kicker = tr("addons.hub.kicker", fallback = "SERVIZI"),
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = tr("common.back", fallback = "Indietro"),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.loading,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            if (visible.isEmpty() && !uiState.loading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        IconBadge(
                            icon = Icons.Outlined.Extension,
                            variant = IconBadgeVariant.Tertiary,
                            size = 64.dp,
                            iconSize = 32.dp,
                        )
                        Text(
                            text = tr("addons.hub.empty", fallback = "Nessun addon disponibile"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 32.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(visible, key = { _, a -> a.id }) { idx, addon ->
                        AnimatedVisibility(
                            visible = idx in visibleIndices,
                            enter = slideInVertically(
                                animationSpec = spring(dampingRatio = 0.86f, stiffness = 300f),
                                initialOffsetY = { 40 },
                            ) + fadeIn(),
                        ) {
                            AddonGridTile(
                                addon = addon,
                                index = idx,
                                onClick = {
                                    vm.onAddonClick(addon.id)
                                    onAddonClick(addon.id)
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddonGridTile(
    addon: AddonModel,
    index: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val iconUrl = if (addon.icon.isNotEmpty()) {
        FilesUrl.build("addons", addon.id, addon.icon)
    } else null

    // Rotate badge variant for visual identity
    val badgeVariant = when (index % 3) {
        0 -> IconBadgeVariant.Cyan
        1 -> IconBadgeVariant.Primary
        else -> IconBadgeVariant.Tertiary
    }

    MensaCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Icon badge — using design system variant
            if (iconUrl != null) {
                CachedAsyncImage(
                    model = iconUrl,
                    contentDescription = addon.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                IconBadge(
                    icon = Icons.Outlined.Extension,
                    variant = badgeVariant,
                    size = 40.dp,
                    iconSize = 20.dp,
                    contentDescription = null,
                )
            }

            // Name
            Text(
                text = addon.name.ifEmpty { addon.id },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )

            // Description
            if (addon.description.isNotEmpty()) {
                Text(
                    text = addon.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
        }
    }
}
