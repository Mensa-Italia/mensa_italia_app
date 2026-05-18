package it.mensa.app.features.sigs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import it.mensa.app.ui.theme.EasingEmphasizedDecelerate
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.model.SigModel
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SigListScreen(
    onSigClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    vm: SigListViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val filtered = remember(state) { vm.filtered(state) }
    val filterKeys = remember(state.sigs) { vm.availableFilterKeys(state) }

    var showCreate by remember { mutableStateOf(false) }
    var editingSig by remember { mutableStateOf<SigModel?>(null) }
    var deletingSig by remember { mutableStateOf<SigModel?>(null) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("community.list.title", fallback = "Community"),
                scrollBehavior = scrollBehavior,
                query = state.query,
                onQueryChange = vm::setQuery,
                searchPlaceholder = tr("community.search.prompt", fallback = "Cerca un gruppo"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
                extraActions = {
                    if (state.canControl) {
                        IconButton(onClick = { showCreate = true }) {
                            Icon(Icons.Filled.Add, contentDescription = tr("community.add", fallback = "Aggiungi community"))
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (state.canControl) {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text(tr("community.create", fallback = "Crea SIG")) },
                )
            }
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Filter chips horizontal
            if (filterKeys.size > 1) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filterKeys) { key ->
                        FilterChip(
                            selected = state.filterKey == key,
                            onClick = { vm.setFilter(key) },
                            label = { Text(vm.filterLabel(key)) },
                            leadingIcon = if (state.filterKey == key) ({
                                Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                            }) else null,
                        )
                    }
                }
            }

            // Content
            AnimatedContent(
                targetState = when {
                    state.loading && state.sigs.isEmpty() -> "loading"
                    state.sigs.isEmpty() -> "empty"
                    filtered.isEmpty() -> "no_match"
                    else -> "list"
                },
                transitionSpec = { MensaMotion.heroTransform() },
                label = "SigListContent",
                modifier = Modifier.fillMaxSize(),
            ) { contentState ->
                when (contentState) {
                    "loading" -> Box(Modifier.fillMaxSize()) {
                        LoadingDots(modifier = Modifier.align(Alignment.Center))
                    }
                    "empty" -> SigEmptyState(
                        title = tr("community.empty", fallback = "Nessun gruppo"),
                        description = tr("community.empty_description", fallback = "Non ci sono gruppi disponibili al momento."),
                        modifier = Modifier.fillMaxSize(),
                    )
                    "no_match" -> SigEmptyState(
                        title = tr("community.no_matches", fallback = "Nessun risultato"),
                        description = tr("community.no_matches_description", fallback = "Prova un altro filtro o un'altra ricerca."),
                        modifier = Modifier.fillMaxSize(),
                    )
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
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize(),
                            ) {
                                itemsIndexed(filtered, key = { _, s -> s.id }) { index, sig ->
                                    val entranceScale by animateFloatAsState(
                                        targetValue = if (appeared) 1f else 0.92f,
                                        animationSpec = tween(
                                            durationMillis = 350,
                                            delayMillis = (index * 60).coerceAtMost(720),
                                            easing = EasingEmphasizedDecelerate,
                                        ),
                                        label = "SigEntrance$index",
                                    )
                                    SigRowCard(
                                        sig = sig,
                                        shortLabel = vm.shortLabel(sig.groupType),
                                        onClick = { onSigClick(sig.id) },
                                        onEditClick = if (state.canControl) ({ editingSig = sig }) else null,
                                        onDeleteClick = if (state.canControl) ({ deletingSig = sig }) else null,
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

    // Create sheet
    if (showCreate) {
        AddSigSheet(
            initial = null,
            onSubmitted = { draft ->
                vm.create(draft)
                showCreate = false
            },
            onDismiss = { showCreate = false },
        )
    }

    editingSig?.let { sig ->
        AddSigSheet(
            initial = sig,
            onSubmitted = { draft ->
                vm.update(sig.id, draft)
                editingSig = null
            },
            onDeleteRequested = {
                vm.delete(sig.id)
                editingSig = null
            },
            onDismiss = { editingSig = null },
        )
    }

    deletingSig?.let { sig ->
        AlertDialog(
            onDismissRequest = { deletingSig = null },
            title = { Text(tr("sigs.delete.confirm.title", fallback = "Eliminare?")) },
            text = { Text(tr("sigs.delete.confirm.body", fallback = "L'azione non è annullabile.")) },
            confirmButton = {
                TextButton(
                    onClick = { vm.delete(sig.id); deletingSig = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(tr("sigs.action.delete", fallback = "Elimina")) }
            },
            dismissButton = {
                TextButton(onClick = { deletingSig = null }) {
                    Text(tr("app.cancel", fallback = "Annulla"))
                }
            },
        )
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

// ─── SigRowCard ───────────────────────────────────────────────────────────────

@Composable
fun SigRowCard(
    sig: SigModel,
    shortLabel: String,
    onClick: () -> Unit,
    onEditClick: (() -> Unit)?,
    onDeleteClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val imageUrl = remember(sig) {
        if (sig.image.isEmpty()) null
        else if (sig.image.startsWith("http")) sig.image
        else FilesUrl.build("sigs", sig.id, sig.image, "800x0")
    }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column {
            // Hero image zone
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
            ) {
                if (imageUrl != null) {
                    CachedAsyncImage(
                        model = imageUrl,
                        contentDescription = sig.name,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f),
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = sig.name.take(1).uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White.copy(alpha = 0.95f),
                        )
                    }
                }

                // Top scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .align(Alignment.TopStart)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )
                )

                // Type chip
                if (shortLabel.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.padding(12.dp).align(Alignment.TopStart),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f),
                    ) {
                        Text(
                            text = shortLabel,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                // Member badge bottom-end
                Row(
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.People,
                                contentDescription = tr("sigs.members", fallback = "Membri"),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
            }

            // Meta block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = sig.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (onEditClick != null || onDeleteClick != null) {
                        Row {
                            onEditClick?.let {
                                IconButton(onClick = it, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Outlined.Tag,
                                        contentDescription = tr("sigs.edit", fallback = "Modifica"),
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                if (sig.description.isNotBlank()) {
                    Text(
                        text = sig.description,
                        style = MaterialTheme.typography.bodySmall,
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
private fun SigEmptyState(title: String, description: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
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
                    imageVector = Icons.Outlined.People,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
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
