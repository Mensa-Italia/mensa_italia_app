package it.mensa.app.features.localoffices

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import it.mensa.app.ui.theme.EasingEmphasizedDecelerate
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.model.LocalOfficeModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalOfficesListScreen(
    onOfficeClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    vm: LocalOfficesListViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val filtered = remember(state) { vm.filtered(state) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("local_offices.title", fallback = "Gruppi locali"),
                scrollBehavior = scrollBehavior,
                query = state.query,
                onQueryChange = vm::setQuery,
                searchPlaceholder = tr("local_offices.search_prompt", fallback = "Cerca per regione"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AnimatedContent(
                targetState = when {
                    state.offices.isEmpty() && state.loading -> "loading"
                    state.offices.isEmpty() -> "empty"
                    filtered.isEmpty() -> "no_match"
                    else -> "list"
                },
                transitionSpec = { MensaMotion.heroTransform() },
                label = "LocalOfficesContent",
                modifier = Modifier.fillMaxSize(),
            ) { contentState ->
                when (contentState) {
                    "loading" -> Box(Modifier.fillMaxSize()) {
                        LoadingDots(modifier = Modifier.align(Alignment.Center))
                    }
                    "empty", "no_match" -> Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            modifier = Modifier.size(64.dp),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Business,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (contentState == "empty")
                                tr("local_offices.empty", fallback = "Nessun gruppo locale")
                            else
                                tr("local_offices.no_results", fallback = "Nessun risultato"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (contentState == "empty")
                                tr("local_offices.empty_description", fallback = "Non sono ancora disponibili gruppi locali.")
                            else
                                tr("local_offices.no_results_description", fallback = "Prova con un altro termine di ricerca."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        var appeared by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { appeared = true }

                        PullToRefreshBox(
                            isRefreshing = state.loading,
                            onRefresh = { vm.refresh() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(filtered, key = { _, o -> o.id }) { index, office ->
                                    val entranceScale by animateFloatAsState(
                                        targetValue = if (appeared) 1f else 0.94f,
                                        animationSpec = tween(
                                            durationMillis = 350,
                                            delayMillis = (index * 60).coerceAtMost(720),
                                            easing = EasingEmphasizedDecelerate,
                                        ),
                                        label = "OfficeEntrance$index",
                                    )
                                    LocalOfficeListCard(
                                        office = office,
                                        onClick = { onOfficeClick(office.id) },
                                        modifier = Modifier.scale(entranceScale),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    state.error?.let { err ->
        AlertDialog(
            onDismissRequest = vm::clearError,
            title = { Text(tr("app.error.title", fallback = "Errore")) },
            text = { Text(err) },
            confirmButton = { TextButton(onClick = vm::clearError) { Text("OK") } },
        )
    }
}

@Composable
private fun LocalOfficeListCard(
    office: LocalOfficeModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverUrl = remember(office) {
        if (office.image.isEmpty()) null
        else FilesUrl.build("local_offices", office.id, office.image, "800x450")
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            // 16:9 cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
            ) {
                if (coverUrl != null) {
                    CachedAsyncImage(
                        model = coverUrl,
                        contentDescription = office.name,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Business,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                        )
                    }
                }

                // Badges bottom-start
                Row(
                    modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = tr("local_offices.city", fallback = "Città"),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = tr("local_offices.active", fallback = "Attivo"),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // Meta
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = office.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
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
