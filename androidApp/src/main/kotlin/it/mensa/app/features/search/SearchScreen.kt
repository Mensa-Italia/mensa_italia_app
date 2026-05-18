package it.mensa.app.features.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Inventory
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.search._components.BoutiqueSearchResultRow
import it.mensa.app.features.search._components.DealSearchResultRow
import it.mensa.app.features.search._components.DocumentSearchResultRow
import it.mensa.app.features.search._components.EventSearchResultRow
import it.mensa.app.features.search._components.LeanPersonSearchResultRow
import it.mensa.app.features.search._components.LeanSearchResultRow
import it.mensa.app.features.search._components.OrgGroupSearchResultRow
import it.mensa.app.features.search._components.OrgRoleSearchResultRow
import it.mensa.app.features.search._components.PersonSearchResultRow
import it.mensa.app.features.search._components.SigSearchResultRow
import it.mensa.app.support.tr
import androidx.compose.ui.text.font.FontWeight
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.theme.MensaSystemBars
import org.koin.androidx.compose.koinViewModel

private const val PREVIEW_LIMIT = 6

// ─── Suggested-query chip model ───────────────────────────────────────────────

private data class SuggestionChip(
    val labelKey: String,
    val labelFallback: String,
    val query: String,
    val icon: ImageVector,
)

private data class FilterChipModel(
    val id: String,
    val labelKey: String,
    val labelFallback: String,
    val typeKey: String?,
    val icon: ImageVector,
)

private val filterChips = listOf(
    FilterChipModel("all", "views.community.chip.all", "Tutti", null, Icons.Outlined.AutoAwesome),
    FilterChipModel("user", "app.search.filter.people", "Persone", "user", Icons.Outlined.Person),
    FilterChipModel("event", "views.events.title", "Eventi", "event", Icons.Outlined.CalendarMonth),
    FilterChipModel("deal", "app.search.filter.deals", "Offerte", "deal", Icons.Outlined.LocalOffer),
    FilterChipModel("sig", "app.discover.groups", "SIG", "sig", Icons.Outlined.Groups),
    FilterChipModel("boutique", "addons.boutique.title", "Boutique", "boutique", Icons.Outlined.ShoppingBag),
    FilterChipModel("document", "addons.documents.title", "Documenti", "document", Icons.Outlined.Bookmark),
    FilterChipModel("org", "app.search.filter.org", "Organigramma", "org", Icons.Outlined.Apartment),
)

// ─── SearchScreen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    vm: SearchViewModel = koinViewModel(),
    onItemClick: (HydratedHit) -> Unit = {},
    onBack: () -> Unit = {},
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()

    // Past-events toggle (events chip only)
    var showPastEvents by remember { mutableStateOf(false) }

    // Reset past-events when chip changes
    LaunchedEffect(uiState.selectedType) {
        if (uiState.selectedType != "event") showPastEvents = false
    }

    MensaScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // ── Compact top: back arrow + pillowy search bar in a row ────
                SearchTopBar(
                    query = uiState.query,
                    onQueryChange = { vm.onQueryChange(it) },
                    onClear = { vm.onClearQuery() },
                    onBack = onBack,
                )

                // ── State content ────────────────────────────────────────────
                Box(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    AnimatedContent(
                        targetState = uiState.phase,
                        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                        modifier = Modifier.fillMaxSize(),
                        label = "SearchPhaseTransition",
                    ) { phase ->
                        when (phase) {
                            is SearchPhase.Idle -> IdleContent(
                                recent = uiState.recent,
                                selectedType = uiState.selectedType,
                                onQueryExample = { vm.onQueryChange(it) },
                                onRecentClick = { vm.onQueryChange(it) },
                                onClearRecent = { vm.clearRecent() },
                                onPickType = { vm.pickType(it) },
                            )
                            is SearchPhase.Loading -> LoadingContent()
                            is SearchPhase.Results -> {
                                if (phase.sections.isEmpty()) {
                                    EmptyResultsContent(query = uiState.query)
                                } else {
                                    ResultsContent(
                                        sections = phase.sections,
                                        selectedType = uiState.selectedType,
                                        showPastEvents = showPastEvents,
                                        onShowPastEventsToggle = { showPastEvents = !showPastEvents },
                                        onShowAll = { vm.pickType(it) },
                                        onPickType = { vm.pickType(it) },
                                        onItemClick = { hit ->
                                            vm.onItemClick(hit)
                                            onItemClick(hit)
                                        },
                                    )
                                }
                            }
                            is SearchPhase.Error -> ErrorContent(
                                message = phase.message,
                                onRetry = { vm.onQueryChange(uiState.query) },
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Top bar — back arrow + inline pillowy search bar ─────────────────────────

/**
 * SearchTopBar — compact top zone with back arrow and the search input on a
 * single row. Replaces the previous drenched hero — the global search drill
 * is a focused interaction, not a hero moment.
 */
@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    MensaSystemBars(darkIcons = true)

    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
    ) {
        // Back arrow — circular surfaceContainerHigh disc, 48dp
        androidx.compose.material3.Surface(
            onClick = onBack,
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 1.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = tr("common.back", "Indietro"),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        // Search input — fills remaining width
        ExpressiveSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onClear = onClear,
            modifier = Modifier.weight(1f),
        )
    }
}

// ─── Pillowy expressive search bar ────────────────────────────────────────────

/**
 * ExpressiveSearchBar — 28dp pillowy surface, surfaceContainerHigh fill, leading
 * search icon, trailing clear-X when text is present. Shape morphs subtly on press.
 */
@Composable
private fun ExpressiveSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 22.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        text = tr("search.placeholder", "Cerca soci, eventi, offerte…"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onQueryChange(query) }),
                )
            }

            AnimatedVisibility(
                visible = query.isNotEmpty(),
                enter = fadeIn(tween(160)),
                exit = fadeOut(tween(120)),
            ) {
                Surface(
                    onClick = onClear,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = tr("search.clear", "Cancella"),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── Idle state ───────────────────────────────────────────────────────────────

/**
 * IdleContent — no query yet.
 *
 * Layout:
 *  - "RICERCHE SUGGERITE" kicker + pillowy tertiary chips for example queries.
 *  - "CATEGORIE" kicker + horizontal row of category chips (filter shortcuts).
 *  - Recent searches list (if any).
 */
@Composable
private fun IdleContent(
    recent: List<String>,
    selectedType: String?,
    onQueryExample: (String) -> Unit,
    onRecentClick: (String) -> Unit,
    onClearRecent: () -> Unit,
    onPickType: (String?) -> Unit,
) {
    val suggestions = listOf(
        SuggestionChip("app.search.suggest.events_nearby", "Eventi vicini",
            tr("app.search.example.events_nearby", "eventi"), Icons.Outlined.CalendarMonth),
        SuggestionChip("app.search.suggest.members_milano", "Soci a Milano",
            tr("app.search.example.members_milano", "milano"), Icons.Outlined.Person),
        SuggestionChip("app.search.suggest.book_deals", "Convenzioni libri",
            tr("app.search.example.book_deals", "libri"), Icons.Outlined.LocalOffer),
        SuggestionChip("app.search.suggest.sigs", "Gruppi interesse",
            tr("app.search.example.sigs", "gruppi"), Icons.Outlined.Groups),
        SuggestionChip("app.search.suggest.council", "Consiglio",
            tr("app.search.example.council", "consiglio"), Icons.Outlined.Apartment),
        SuggestionChip("app.search.suggest.card", "Tessera",
            tr("app.search.example.card", "tessera"), Icons.Outlined.Bookmark),
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
    ) {
        // ── Suggested queries ────────────────────────────────────────────────
        item(key = "suggested_kicker") {
            ExpressiveSectionKicker(
                kicker = tr("app.search.idle.suggested.kicker", "RICERCHE SUGGERITE"),
                title = tr("app.search.idle.suggested.title", "Prova così"),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        item(key = "suggested_chips") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(suggestions) { index, chip ->
                    SuggestionChipView(
                        label = tr(chip.labelKey, chip.labelFallback),
                        icon = chip.icon,
                        onClick = { onQueryExample(chip.query) },
                        index = index,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // ── Categories (filter shortcuts) ────────────────────────────────────
        item(key = "categories_kicker") {
            ExpressiveSectionKicker(
                kicker = tr("app.search.idle.categories.kicker", "CATEGORIE"),
                title = tr("app.search.idle.categories.title", "Sfoglia per tipo"),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }
        item(key = "categories_chips") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(filterChips.drop(1)) { index, chip ->
                    CategoryChipView(
                        label = tr(chip.labelKey, chip.labelFallback),
                        icon = chip.icon,
                        selected = selectedType == chip.typeKey,
                        onClick = { onPickType(chip.typeKey) },
                        index = index,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // ── Recent searches ──────────────────────────────────────────────────
        if (recent.isNotEmpty()) {
            item(key = "recent_kicker") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = tr("app.search.recent.kicker", "RECENTI"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = tr("app.search.recent.title", "Le tue ricerche"),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    TextButton(onClick = onClearRecent) {
                        Text(tr("app.search.recent.clear", "Cancella"))
                    }
                }
            }
            itemsIndexed(recent, key = { _, q -> "recent_$q" }) { index, q ->
                RecentSearchRow(
                    query = q,
                    onClick = { onRecentClick(q) },
                    index = index,
                )
            }
        }
    }
}

// ─── Suggestion / category chip composables ──────────────────────────────────

@Composable
private fun SuggestionChipView(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    index: Int,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 24.dp,
        label = "suggest-chip-corner",
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(280, delayMillis = index * 40)) +
                slideInVertically(tween(280, delayMillis = index * 40)) { it / 4 },
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(corner),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            interactionSource = interactionSource,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun CategoryChipView(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    index: Int,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 22.dp,
        label = "cat-chip-corner",
    )

    val container = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val content = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(280, delayMillis = index * 40)) +
                slideInVertically(tween(280, delayMillis = index * 40)) { it / 4 },
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(corner),
            color = container,
            interactionSource = interactionSource,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                )
            }
        }
    }
}

@Composable
private fun RecentSearchRow(
    query: String,
    onClick: () -> Unit,
    index: Int,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 18.dp,
        label = "recent-row-corner",
    )

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(260, delayMillis = index * 30)) +
                slideInVertically(tween(260, delayMillis = index * 30)) { it / 6 },
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(corner),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            interactionSource = interactionSource,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = query,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── Loading ──────────────────────────────────────────────────────────────────

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LoadingDots()
        Spacer(Modifier.height(16.dp))
        Text(
            text = tr("app.search.loading", "Cerchiamo…"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ─── Empty results — drenched tertiary card ──────────────────────────────────

@Composable
private fun EmptyResultsContent(query: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 28.dp,
                topEnd = 18.dp,
                bottomEnd = 36.dp,
                bottomStart = 18.dp,
            ),
            color = MaterialTheme.colorScheme.tertiaryContainer,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.20f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(36.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text = tr("app.search.empty.kicker", "NESSUN RISULTATO"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = tr("app.search.empty.title", "Niente per \"$query\""),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = tr(
                        "app.search.empty.hint",
                        "Prova un'altra parola, controlla l'ortografia o sfoglia per categoria.",
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.80f),
                )
            }
        }
    }
}

// ─── Error ────────────────────────────────────────────────────────────────────

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Error,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = tr("app.search.error.title", "Ricerca non disponibile"),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text(tr("app.retry", "Riprova"))
        }
    }
}

// ─── Results ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultsContent(
    sections: List<HydratedSection>,
    selectedType: String?,
    showPastEvents: Boolean,
    onShowPastEventsToggle: () -> Unit,
    onShowAll: (String) -> Unit,
    onPickType: (String?) -> Unit,
    onItemClick: (HydratedHit) -> Unit,
) {
    val nowMs = System.currentTimeMillis()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // Filter chip rail at the top of results — lets users narrow without scroll
        item(key = "filter_rail") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(filterChips) { index, chip ->
                    CategoryChipView(
                        label = tr(chip.labelKey, chip.labelFallback),
                        icon = chip.icon,
                        selected = (selectedType ?: "all") == (chip.typeKey ?: "all"),
                        onClick = { onPickType(chip.typeKey) },
                        index = index,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        for (section in sections) {
            // People section — role-holders sorted first
            val hits = if (section.type == "user") {
                section.hits.sortedWith(Comparator { a, b ->
                    val aIsRole = (a.payload as? HydratedHit.Payload.User)?.orgRole?.isNotEmpty() == true
                    val bIsRole = (b.payload as? HydratedHit.Payload.User)?.orgRole?.isNotEmpty() == true
                    when {
                        aIsRole && !bIsRole -> -1
                        !aIsRole && bIsRole -> 1
                        else -> 0
                    }
                })
            } else section.hits

            // Event filtering
            val (visibleHits, pastHidden) = if (section.type == "event") {
                val upcoming = hits.filter { hit ->
                    val e = (hit.payload as? HydratedHit.Payload.Event)?.event
                    e == null || e.whenEnd.toEpochMilliseconds() >= nowMs
                }
                val hidingPast = selectedType == null || !showPastEvents
                if (hidingPast) {
                    upcoming to (hits.size - upcoming.size)
                } else {
                    hits to 0
                }
            } else {
                hits to 0
            }

            val onlyPastInTutti = section.type == "event" && selectedType == null &&
                    visibleHits.isEmpty() && pastHidden > 0

            val isAllChip = selectedType == null
            val preview = if (isAllChip && visibleHits.size > PREVIEW_LIMIT) {
                visibleHits.take(PREVIEW_LIMIT)
            } else visibleHits
            val hasMore = isAllChip && visibleHits.size > PREVIEW_LIMIT

            // ── Expressive section header ──
            item(key = "header_${section.type}") {
                ExpressiveResultSectionHeader(
                    type = section.type,
                    count = if (onlyPastInTutti) pastHidden else preview.size,
                )
            }

            if (onlyPastInTutti) {
                item(key = "past_cta_${section.type}") {
                    ShowPastEventsCta(
                        hidden = pastHidden,
                        onClick = { onShowAll("event") },
                    )
                }
            } else {
                itemsIndexed(preview, key = { _, it -> it.id + section.type }) { index, hit ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(280, delayMillis = index * 40)) +
                                slideInVertically(
                                    animationSpec = tween(280, delayMillis = index * 40),
                                ) { it / 10 },
                    ) {
                        ExpressiveResultRow(
                            hit = hit,
                            type = section.type,
                            onClick = { onItemClick(hit) },
                        )
                    }
                }

                if (hasMore) {
                    item(key = "more_${section.type}") {
                        ShowAllRow(
                            type = section.type,
                            total = visibleHits.size,
                            onClick = { onShowAll(section.type) },
                        )
                    }
                }

                if (section.type == "event" && selectedType == "event") {
                    if (!showPastEvents && pastHidden > 0) {
                        item(key = "toggle_past_events") {
                            TogglePastEventsRow(
                                hidden = pastHidden,
                                showing = false,
                                onClick = onShowPastEventsToggle,
                            )
                        }
                    } else if (showPastEvents) {
                        item(key = "toggle_past_events_hide") {
                            TogglePastEventsRow(
                                hidden = 0,
                                showing = true,
                                onClick = onShowPastEventsToggle,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Section header for results ───────────────────────────────────────────────

@Composable
private fun ExpressiveResultSectionHeader(type: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = sectionKicker(type),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = sectionTitle(type),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
        }
    }
}

private fun sectionKicker(type: String): String = when (type) {
    "user" -> "SOCI"
    "event" -> "EVENTI"
    "deal" -> "DEAL"
    "sig" -> "GRUPPI"
    "document" -> "DOCUMENTI"
    "boutique" -> "BOUTIQUE"
    "addon" -> "ADDON"
    "org" -> "ORGANIGRAMMA"
    "quid_issue" -> "QUID"
    "quid_article" -> "ARTICOLI"
    "linktree_link" -> "LOCALI"
    else -> type.uppercase()
}

private fun sectionTitle(type: String): String = when (type) {
    "user" -> "Persone"
    "event" -> "Eventi"
    "deal" -> "Offerte"
    "sig" -> "Gruppi e interessi"
    "document" -> "Documenti"
    "boutique" -> "Boutique"
    "addon" -> "Addon"
    "org" -> "Organigramma"
    "quid_issue" -> "Numeri Quid"
    "quid_article" -> "Articoli Quid"
    "linktree_link" -> "Gruppi locali"
    else -> type.replaceFirstChar { it.uppercase() }
}

// ─── Result row — pillowy tonal container with shape morph ───────────────────

@Composable
private fun ExpressiveResultRow(
    hit: HydratedHit,
    type: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val corner by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 22.dp,
        label = "result-row-corner",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(corner),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                ResultRowContent(hit = hit, type = type)
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ResultRowContent(hit: HydratedHit, type: String) {
    when (val p = hit.payload) {
        is HydratedHit.Payload.User -> PersonSearchResultRow(
            member = p.member,
            orgRole = p.orgRole,
            orgGroup = p.orgGroup,
            localOfficeAffiliations = p.localOfficeAffiliations,
        )
        is HydratedHit.Payload.Event -> EventSearchResultRow(event = p.event)
        is HydratedHit.Payload.Deal -> DealSearchResultRow(deal = p.deal)
        is HydratedHit.Payload.Sig -> SigSearchResultRow(sig = p.sig)
        is HydratedHit.Payload.Document -> DocumentSearchResultRow(document = p.document)
        is HydratedHit.Payload.Boutique -> BoutiqueSearchResultRow(product = p.product)
        is HydratedHit.Payload.Addon -> LeanSearchResultRow(
            title = p.addon.name,
            subtitle = p.addon.description,
            icon = Icons.Outlined.Extension,
        )
        is HydratedHit.Payload.OrgGroup -> OrgGroupSearchResultRow(group = p.group)
        is HydratedHit.Payload.OrgRole -> OrgRoleSearchResultRow(
            role = p.role,
            groupTitle = p.groupTitle,
            member = p.member,
        )
        is HydratedHit.Payload.Lean -> {
            if (type == "user") {
                LeanPersonSearchResultRow(
                    id = hit.id,
                    name = hit.leanTitle,
                    subtitle = hit.leanSubtitle,
                    imageFilename = hit.leanImage,
                )
            } else {
                LeanSearchResultRow(
                    title = hit.leanTitle,
                    subtitle = hit.leanSubtitle,
                    icon = iconForType(type),
                )
            }
        }
    }
}

private fun iconForType(type: String): ImageVector = when (type) {
    "user" -> Icons.Outlined.Person
    "event" -> Icons.Outlined.CalendarMonth
    "deal" -> Icons.Outlined.LocalOffer
    "sig" -> Icons.Outlined.Groups
    "document" -> Icons.Outlined.Bookmark
    "boutique" -> Icons.Outlined.ShoppingBag
    "org" -> Icons.Outlined.Apartment
    "quid_issue" -> Icons.Outlined.Inventory
    "quid_article" -> Icons.Outlined.Newspaper
    "linktree_link" -> Icons.Outlined.Group
    else -> Icons.Filled.Search
}

// ─── Footer rows ──────────────────────────────────────────────────────────────

@Composable
private fun ShowAllRow(type: String, total: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = tr("app.search.show_all", "Mostra tutti"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = "($total)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
            )
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ShowPastEventsCta(hidden: Int, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("app.search.events.show_past", "Mostra eventi passati"),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                val evtTxt = if (hidden == 1) "1 evento" else "$hidden eventi"
                Text(
                    text = evtTxt,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TogglePastEventsRow(hidden: Int, showing: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AccessTime,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = if (showing) tr("app.search.events.hide_past", "Nascondi eventi passati")
            else tr("app.search.events.show_past", "Mostra eventi passati"),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        if (!showing && hidden > 0) {
            Text(
                text = "($hidden)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Expressive section kicker (idle screen) ──────────────────────────────────

@Composable
private fun ExpressiveSectionKicker(
    kicker: String,
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp, top = 4.dp),
    ) {
        Text(
            text = kicker,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
