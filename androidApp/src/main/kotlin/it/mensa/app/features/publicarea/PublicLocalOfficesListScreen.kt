package it.mensa.app.features.publicarea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.KickerLabel
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaCard
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import it.mensa.app.ui.theme.BackdropDark
import it.mensa.app.ui.theme.MensaBlue
import it.mensa.shared.model.LocalOfficeModel
import org.koin.androidx.compose.koinViewModel

/**
 * Pre-login variant of LocalOfficesListScreen. Uses [PublicLocalOfficesListViewModel]
 * (which calls `refreshAllOfficesPublic`) and pushes onto [PublicLocalOfficeDetailScreen].
 * No FAB, no admin affordances.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicLocalOfficesListScreen(
    onOfficeClick: (String) -> Unit,
    onBack: () -> Unit,
    vm: PublicLocalOfficesListViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val filtered = remember(state) { vm.filtered(state) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("public.local_offices.title", fallback = "Gruppi locali"),
                kicker = tr("local_offices.kicker_label", fallback = "MENSA ITALIA"),
                scrollBehavior = scrollBehavior,
                query = state.query,
                onQueryChange = vm::setQuery,
                searchPlaceholder = tr("public.local_offices.search_prompt", fallback = "Cerca per regione"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
                clearContentDescription = tr("common.clear", fallback = "Pulisci"),
            )
        },
    ) { innerPadding ->
        when {
            state.offices.isEmpty() && state.loading -> Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) { LoadingDots() }

            state.offices.isEmpty() -> EmptyState(
                title = tr("public.local_offices.empty.title", fallback = "Nessun gruppo locale"),
                description = tr(
                    "public.local_offices.empty.description",
                    fallback = "Non sono ancora disponibili gruppi locali.",
                ),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            filtered.isEmpty() -> EmptyState(
                title = tr("local_offices.no_results", fallback = "Nessun risultato"),
                description = tr(
                    "local_offices.no_results_description",
                    fallback = "Prova con un altro termine di ricerca.",
                ),
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = { vm.refresh() },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            ) {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(filtered, key = { it.id }) { office ->
                        PublicLocalOfficeRow(
                            office = office,
                            onClick = { onOfficeClick(office.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PublicLocalOfficeRow(
    office: LocalOfficeModel,
    onClick: () -> Unit,
) {
    val coverUrl = remember(office.id, office.image) {
        if (office.image.isEmpty()) null
        else FilesUrl.build("view_local_office", office.id, office.image, "800x450")
    }

    MensaCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 0.dp,
        onClick = onClick,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                if (coverUrl != null) {
                    CachedAsyncImage(
                        model = coverUrl,
                        contentDescription = office.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(Brush.verticalGradient(listOf(MensaBlue, BackdropDark))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Business,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                KickerLabel(
                    text = tr("public.local_office.kicker", fallback = "GRUPPO LOCALE"),
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = office.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (office.region.isNotBlank() &&
                    !office.region.equals(office.name, ignoreCase = true)
                ) {
                    Text(
                        text = office.region,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (office.bio.isNotBlank()) {
                    Text(
                        text = office.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.Business,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
