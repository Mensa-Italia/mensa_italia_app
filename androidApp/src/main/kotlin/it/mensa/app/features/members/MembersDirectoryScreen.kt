package it.mensa.app.features.members

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.members._components.MemberCellCompact
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchableTopAppBar
import it.mensa.app.ui.theme.EasingEmphasizedDecelerate
import it.mensa.app.ui.theme.MensaMotion
import it.mensa.shared.members.MemberDirectory
import it.mensa.shared.members.MemberPlace
import org.koin.androidx.compose.koinViewModel
import it.mensa.app.support.RefreshOnReconnect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalFoundationApi::class)
@Composable
fun MembersDirectoryScreen(
    onMemberClick: (String) -> Unit = {},
    onBack: () -> Unit = {},
    vm: MembersDirectoryViewModel = koinViewModel(),
) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val filtered = remember(state) { vm.filtered(state) }
    val sectioned = remember(filtered) { vm.sectioned(filtered) }
    val regions = remember(state.members) { MemberDirectory.regions(state.members) }
    val provinces = remember(state.members, state.region) { MemberDirectory.provinces(state.members, state.region) }
    val filtering = state.query.isNotBlank() || state.region != null

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MensaScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MensaSearchableTopAppBar(
                title = tr("members.registry.title", fallback = "Registro Soci"),
                scrollBehavior = scrollBehavior,
                query = state.query,
                onQueryChange = vm::onQueryChange,
                searchPlaceholder = tr("members.search_placeholder", fallback = "Cerca un socio…"),
                onBack = onBack,
                searchContentDescription = tr("common.search", fallback = "Cerca"),
                backContentDescription = tr("common.back", fallback = "Indietro"),
            )
        },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            // Fuori dal contenuto animato: c'e' gia' mentre il registro carica
            // (le regioni sono fisse) e resta quando la regione non ha nessun
            // socio che corrisponde alla ricerca, per poterla togliere.
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PlaceFilterChip(
                    label = tr("members.filter.region", fallback = "Regione"),
                    allLabel = tr("members.filter.region.all", fallback = "Tutte le regioni"),
                    places = regions,
                    selected = state.region,
                    onSelect = vm::onRegionChange,
                    showCounts = state.members.isNotEmpty(),
                )
                // Il sottofiltro c'e' appena c'e' una regione: le sue province
                // sono fisse, non aspettano i soci. Entra ed esce con lo
                // spring spaziale del tema (expressive), allargandosi dalla
                // chip della regione.
                val motion = MaterialTheme.motionScheme
                AnimatedVisibility(
                    visible = provinces.isNotEmpty(),
                    enter = expandHorizontally(motion.fastSpatialSpec(), expandFrom = Alignment.Start) +
                        scaleIn(motion.fastSpatialSpec(), initialScale = 0.8f) +
                        fadeIn(motion.fastEffectsSpec()),
                    exit = shrinkHorizontally(motion.fastSpatialSpec(), shrinkTowards = Alignment.Start) +
                        scaleOut(motion.fastSpatialSpec(), targetScale = 0.8f) +
                        fadeOut(motion.fastEffectsSpec()),
                ) {
                    PlaceFilterChip(
                        label = tr("members.filter.province", fallback = "Provincia"),
                        allLabel = tr("members.filter.province.all", fallback = "Tutte le province"),
                        places = provinces,
                        selected = state.province,
                        onSelect = vm::onProvinceChange,
                        showCounts = state.members.isNotEmpty(),
                    )
                }
            }
            AnimatedContent(
                targetState = when {
                    state.loading && state.members.isEmpty() -> "loading"
                    filtered.isEmpty() -> "empty"
                    else -> "list"
                },
                transitionSpec = { MensaMotion.heroTransform() },
                label = "MembersContent",
                modifier = Modifier.fillMaxSize(),
            ) { contentState ->
                when (contentState) {
                    "loading" -> Box(Modifier.fillMaxSize()) {
                        LoadingDots(modifier = Modifier.align(Alignment.Center))
                    }
                    "empty" -> Column(
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
                                    imageVector = Icons.Outlined.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (!filtering)
                                tr("members.empty", fallback = "Directory vuota")
                            else
                                tr("members.no_results", fallback = "Nessun socio trovato"),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                !filtering ->
                                    tr("members.empty_description", fallback = "Trascina giù per aggiornare la directory.")
                                state.region != null ->
                                    tr("members.no_results_region_description", fallback = "Prova con un altro nome, regione o provincia.")
                                else ->
                                    tr("members.no_results_description", fallback = "Prova con un altro nome o città.")
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        var appeared by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { appeared = true }

                        RefreshOnReconnect { vm.refresh() }
                        PullToRefreshBox(
                            isRefreshing = state.loading,
                            onRefresh = { vm.refresh() },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 16.dp),
                            ) {
                                sectioned.forEach { (letter, members) ->
                                    // Sticky alphabetical header
                                    stickyHeader(key = "letter_$letter") {
                                        Surface(
                                            color = MaterialTheme.colorScheme.background,
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(
                                                text = letter,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(
                                                        start = 20.dp,
                                                        end = 20.dp,
                                                        top = 16.dp,
                                                        bottom = 4.dp,
                                                    ),
                                            )
                                        }
                                    }

                                    items(members, key = { it.id }) { member ->
                                        val entranceScale by animateFloatAsState(
                                            targetValue = if (appeared) 1f else 0.95f,
                                            animationSpec = tween(
                                                durationMillis = 300,
                                                easing = EasingEmphasizedDecelerate,
                                            ),
                                            label = "MemberEntrance${member.id}",
                                        )
                                        Column(
                                            modifier = Modifier
                                                .scale(entranceScale)
                                                .clickable { onMemberClick(member.id) },
                                        ) {
                                            MemberCellCompact(
                                                member = member,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                            )
                                            if (member != members.last()) {
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(start = 72.dp, end = 22.dp),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
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

/**
 * Il filtro per regione o provincia: una chip che apre il menu dei [places],
 * ognuno col suo numero di soci quando il registro e' caricato ([showCounts]).
 * Selezionata mostra il luogo scelto; [allLabel] lo toglie.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PlaceFilterChip(
    label: String,
    allLabel: String,
    places: List<MemberPlace>,
    selected: String?,
    onSelect: (String?) -> Unit,
    showCounts: Boolean,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val choose: (String?) -> Unit = { place ->
        expanded = false
        onSelect(place)
    }
    Box(modifier) {
        FilterChip(
            // "Regione" -> "Friuli-Venezia Giulia": la larghezza segue il testo.
            modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.fastSpatialSpec()),
            selected = selected != null,
            onClick = { expanded = true },
            label = { Text(selected ?: label) },
            leadingIcon = {
                Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        DropdownMenuPopup(expanded = expanded, onDismissRequest = { expanded = false }) {
            val check: @Composable () -> Unit = { Icon(Icons.Filled.Check, contentDescription = null) }
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(0, 2)) {
                DropdownMenuItem(
                    selected = selected == null,
                    onClick = { choose(null) },
                    text = { Text(allLabel) },
                    shapes = MenuDefaults.itemShape(0, 1),
                    selectedLeadingIcon = check,
                )
            }
            Spacer(Modifier.height(2.dp))
            DropdownMenuGroup(shapes = MenuDefaults.groupShape(1, 2)) {
                places.forEachIndexed { index, place ->
                    DropdownMenuItem(
                        selected = selected == place.name,
                        onClick = { choose(place.name) },
                        text = { Text(place.name) },
                        shapes = MenuDefaults.itemShape(index, places.size),
                        selectedLeadingIcon = check,
                        trailingIcon = if (showCounts) {
                            {
                                Text(
                                    place.count.toString(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }
}
