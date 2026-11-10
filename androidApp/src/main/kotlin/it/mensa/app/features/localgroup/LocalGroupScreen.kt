package it.mensa.app.features.localgroup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Leaderboard
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.deals.DealsRoute
import it.mensa.app.features.deals._components.DealCardView
import it.mensa.app.features.events.EventRoutes
import it.mensa.app.features.events._components.EventRowCard
import it.mensa.app.features.leaderboard.LeaderboardRoutes
import it.mensa.app.features.localoffices.LinktreeGroup
import it.mensa.app.features.localoffices.LocalOfficeHero
import it.mensa.app.features.localoffices.LocalOfficesRoutes
import it.mensa.app.features.localoffices.PersonGroup
import it.mensa.app.features.localoffices.PersonRowData
import it.mensa.app.features.members.MembersRoutes
import it.mensa.app.features.tableport.TableportRoutes
import it.mensa.app.features.today._components.EmptyEventHero
import it.mensa.app.features.today._components.SectionLabel
import it.mensa.app.features.today._components.SectionRow
import it.mensa.app.features.today._components.SeeAllRow
import it.mensa.app.support.ExternalLinks
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.shared.localgroup.LocalGroupSnapshot
import it.mensa.shared.localgroup.LocalGroupState
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * The "Gruppo locale" tab: the member's local office, made only of blocks the
 * app already has. The office page's hero, people and links
 * (LocalOfficeBlocks), Today's section labels, membership rows, next-event
 * card and see-all rows, and the deals list's cards. For the office's admins,
 * the way into the admin area. Mirrors iOS `LocalGroupView`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocalGroupScreen(onNavigate: (String) -> Unit) {
    val repository = remember { koinAccess().localGroup }
    val state by repository.observe().collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    val refresh: () -> Unit = {
        scope.launch {
            refreshing = true
            // refresh() non lancia per errori di rete, ma un'eccezione sfuggita
            // non deve far cadere l'app ne' lasciare lo spinner acceso.
            try {
                runCatching { repository.refresh() }
            } finally {
                refreshing = false
            }
        }
    }
    // Il ritorno alla propria sede va fino in fondo anche se si lascia la tab:
    // il risultato e' lo stato del repository, non di questa schermata.
    val backToMine: () -> Unit = {
        scope.launch {
            refreshing = true
            try {
                withContext(NonCancellable) { runCatching { repository.selectOffice(null) } }
            } finally {
                refreshing = false
            }
        }
    }
    // Il NavHost smonta la tab quando si cambia tab: rileggere tutto a ogni
    // ritorno sarebbe uno spreco. Si carica solo se non c'e' niente da
    // mostrare (come `needsInitialLoad` su iOS); il resto e' pull-to-refresh.
    LaunchedEffect(Unit) {
        if (needsInitialLoad(repository.observe().value)) runCatching { repository.refresh() }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    MensaScaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(tr("local_group.title", "Gruppo locale")) },
                actions = {
                    shownSnapshot(state)?.takeIf { it.isBoard }?.let { snapshot ->
                        BoardMenu(isOverride = snapshot.isOverride, onNavigate = onNavigate, onBackToMine = backToMine)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = refresh,
            modifier = Modifier.fillMaxSize().padding(padding).nestedScroll(scrollBehavior.nestedScrollConnection),
        ) {
            when (val current = state) {
                LocalGroupState.Loading -> CenteredLoading()
                LocalGroupState.NoRegion ->
                    CenteredMessage(tr("local_group.state.no_region", "Il registro soci non indica la tua regione."))
                LocalGroupState.NoOffice ->
                    CenteredMessage(tr("local_group.state.no_office", "Nessun gruppo locale per la tua regione."))
                is LocalGroupState.Ready ->
                    LocalGroupContent(current.snapshot, failed = false, onRetry = refresh, onBackToMine = backToMine, onNavigate = onNavigate)
                is LocalGroupState.Failed -> current.cached?.let {
                    LocalGroupContent(it, failed = true, onRetry = refresh, onBackToMine = backToMine, onNavigate = onNavigate)
                } ?: CenteredMessage(
                    text = tr("local_group.state.failed", "Non siamo riusciti a caricare il gruppo locale."),
                    action = tr("local_group.retry", "Riprova") to refresh,
                )
            }
        }
    }
}

/** The snapshot on screen: the ready one, or the cached one under a failed refresh. */
private fun shownSnapshot(state: LocalGroupState): LocalGroupSnapshot? = when (state) {
    is LocalGroupState.Ready -> state.snapshot
    is LocalGroupState.Failed -> state.cached
    else -> null
}

/**
 * The board's menu: another office for this session, the association's
 * figures and, while another office is shown, the way back to one's own.
 */
@Composable
private fun BoardMenu(isOverride: Boolean, onNavigate: (String) -> Unit, onBackToMine: () -> Unit) {
    var open by remember { mutableStateOf(false) }
    val choose: (() -> Unit) -> () -> Unit = { action ->
        {
            open = false
            action()
        }
    }
    IconButton(onClick = { open = true }) {
        Icon(Icons.Outlined.MoreVert, contentDescription = tr("local_group.board.menu", "Altre azioni"))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text(tr("local_group.board.switch", "Cambia gruppo locale")) },
            leadingIcon = { Icon(Icons.Outlined.Business, contentDescription = null) },
            onClick = choose { onNavigate(LocalGroupRoutes.OFFICES) },
        )
        DropdownMenuItem(
            text = { Text(tr("local_group.board.association", "Statistiche dell'associazione")) },
            leadingIcon = { Icon(Icons.Outlined.BarChart, contentDescription = null) },
            onClick = choose { onNavigate(LocalGroupRoutes.ASSOCIATION) },
        )
        if (isOverride) {
            DropdownMenuItem(
                text = { Text(tr("local_group.board.back_to_mine", "Torna al mio gruppo locale")) },
                leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null) },
                onClick = choose(onBackToMine),
            )
        }
    }
}

/** Nothing to show yet: never read, or failed with no snapshot to fall back on. */
private fun needsInitialLoad(state: LocalGroupState): Boolean =
    state is LocalGroupState.Loading || (state is LocalGroupState.Failed && state.cached == null)

@Composable
private fun LocalGroupContent(
    snapshot: LocalGroupSnapshot,
    failed: Boolean,
    onRetry: () -> Unit,
    onBackToMine: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    // Composta come TodayScreen: ogni sezione e' un'etichetta (SectionLabel)
    // sopra il suo blocco, a 20dp dai bordi e 20dp dalla sezione prima.
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 40.dp)) {
        if (snapshot.isOverride) item(key = "override") { OverrideBanner(snapshot.office.name, onBackToMine) }
        if (failed) item(key = "failed") { FailedBanner(onRetry) }
        item(key = "header") { LocalOfficeHero(office = snapshot.office, showRegion = true) }
        if (snapshot.admins.isNotEmpty()) item(key = "secretariat") { SecretariatSection(snapshot, onNavigate) }
        if (snapshot.links.isNotEmpty()) item(key = "contacts") { ContactsSection(snapshot, onNavigate) }
        item(key = "ranking") { RankingSection(snapshot, onNavigate) }
        snapshot.stats?.let { stats ->
            item(key = "members") {
                Section(tr("local_group.section.members", "Soci"), Icons.Outlined.Group) {
                    RowsCard {
                        SectionRow(
                            title = tr("local_group.members.active", "Iscritti in regione"),
                            icon = Icons.Outlined.Groups,
                            onClick = null,
                            value = stats.membersActive.toString(),
                        )
                        RowsDivider()
                        SectionRow(
                            title = tr("local_group.members.new_week", "Nuovi nell'ultima settimana"),
                            icon = Icons.Outlined.PersonAdd,
                            onClick = { onNavigate(LocalGroupRoutes.NEW_MEMBERS) },
                            value = stats.membersNew7d.toString(),
                        )
                    }
                }
            }
        }
        item(key = "events") { EventsSection(snapshot, onNavigate) }
        dealsSection(snapshot, onNavigate)
        adminEntry(snapshot, onNavigate)
    }
}

/** A section as Today composes them: air above, the label, then its [content]. */
@Composable
private fun Section(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Spacer(Modifier.height(SECTION_GAP))
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SIDE_PADDING),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(title = title, icon = icon)
        content()
    }
}

/** The card of Today's membership rows ([SectionRow]). */
@Composable
private fun RowsCard(content: @Composable () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

/** The divider between two [SectionRow]s, as in MembershipSection. */
@Composable
private fun RowsDivider() {
    HorizontalDivider(modifier = Modifier.padding(start = 20.dp))
}

private val SECTION_GAP = 20.dp
private val SIDE_PADDING = 20.dp

/** A one-row notice over the page: a [RowsCard] between the side margins. */
@Composable
private fun Banner(content: @Composable () -> Unit) {
    Column(Modifier.padding(horizontal = SIDE_PADDING, vertical = 8.dp)) { RowsCard(content) }
}

@Composable
private fun FailedBanner(onRetry: () -> Unit) {
    Banner {
        SectionRow(
            title = tr("local_group.state.failed", "Non siamo riusciti a caricare il gruppo locale."),
            icon = Icons.Outlined.CloudOff,
            onClick = onRetry,
        )
    }
}

/** The board is looking at [officeName], not at its own office: say so, with the way back. */
@Composable
private fun OverrideBanner(officeName: String, onBackToMine: () -> Unit) {
    Banner {
        // Come una SectionRow, ma con un bottone al posto del valore.
        ListItem(
            headlineContent = { Text(tr("local_group.board.viewing", "Stai guardando {office}", "office" to officeName)) },
            leadingContent = { Icon(Icons.Outlined.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            trailingContent = {
                TextButton(onClick = onBackToMine) { Text(tr("local_group.board.back_short", "Torna al tuo")) }
            },
            colors = cardItemColors(),
        )
    }
}

@Composable
private fun SecretariatSection(snapshot: LocalGroupSnapshot, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    val secretary = tr("local_group.role.secretary", "Segretario")
    val coSecretary = tr("local_group.role.co_secretary", "Co-segretario")
    val people = (listOfNotNull(snapshot.secretary) + snapshot.coSecretaries).map {
        PersonRowData(
            recordId = it.id,
            userId = it.user,
            name = it.name,
            image = it.image,
            role = if (it.isTheOfficer) secretary else coSecretary,
            email = it.email,
        )
    }
    Section(tr("local_group.section.secretariat", "Segreteria"), Icons.Outlined.Groups) {
        PersonGroup(
            people = people,
            onPersonClick = { onNavigate(MembersRoutes.detail(it)) },
            collection = "local_office_admins",
            horizontalPadding = 0.dp,
            onMailClick = { ExternalLinks.open(context, "mailto:$it") },
        )
    }
}

@Composable
private fun ContactsSection(snapshot: LocalGroupSnapshot, onNavigate: (String) -> Unit) {
    val context = LocalContext.current
    Section(tr("local_group.section.contacts", "Contatti"), Icons.Outlined.Link) {
        Column {
            LinktreeGroup(
                linktree = snapshot.links,
                onLinkClick = { ExternalLinks.open(context, it) },
                horizontalPadding = 0.dp,
            )
            // Come la pagina della sede: oltre i primi due, "Vedi tutti" apre il linktree.
            if (snapshot.links.size > 2) {
                SeeAllRow(
                    onClick = { onNavigate(LocalOfficesRoutes.linktree(snapshot.office.id)) },
                    label = tr("local_office.linktree.see_all", fallback = "Vedi tutti"),
                )
            }
        }
    }
}

@Composable
private fun RankingSection(snapshot: LocalGroupSnapshot, onNavigate: (String) -> Unit) {
    Section(tr("local_group.section.ranking", "Classifica e medaglie"), Icons.Outlined.EmojiEvents) {
        RowsCard {
            snapshot.ranking?.let { ranking ->
                SectionRow(
                    title = tr("local_group.ranking.position", "Posizione in classifica"),
                    icon = Icons.Outlined.Leaderboard,
                    onClick = { onNavigate(LeaderboardRoutes.BOARD) },
                    value = "${ranking.position.roundToInt()}°",
                    supporting = tr("local_group.ranking.points", "{0} punti", "0" to ranking.score.roundToInt()),
                )
                RowsDivider()
            }
            SectionRow(
                title = tr("local_group.medals", "Medaglie della regione"),
                icon = Icons.Outlined.WorkspacePremium,
                onClick = { onNavigate(TableportRoutes.medals(region = true)) },
            )
        }
    }
}

@Composable
private fun EventsSection(snapshot: LocalGroupSnapshot, onNavigate: (String) -> Unit) {
    val next = snapshot.nextEvent
    val allEvents = tr("local_group.events.all", "Tutti gli eventi")
    Section(tr("local_group.events.next", "Prossimo evento"), Icons.Outlined.CalendarMonth) {
        if (next != null) {
            // Come il prossimo evento di Today.
            EventRowCard(event = next, onClick = { onNavigate(EventRoutes.detail(next.id)) })
            SeeAllRow(onClick = { onNavigate(EventRoutes.LIST) }, label = allEvents)
        } else {
            EmptyEventHero(
                onClick = { onNavigate(EventRoutes.LIST) },
                title = tr("local_group.events.none", "Nessun evento in programma"),
                subtitle = allEvents,
            )
        }
    }
}

private fun LazyListScope.dealsSection(snapshot: LocalGroupSnapshot, onNavigate: (String) -> Unit) {
    val deals = snapshot.deals.take(MAX_DEALS)
    item(key = "deals") {
        Section(tr("local_group.section.deals", "Convenzioni"), Icons.Outlined.LocalOffer) {
            if (deals.isEmpty()) {
                Text(
                    tr("local_group.deals.none", "Nessuna convenzione attiva in regione"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // La card della lista convenzioni (DealListScreen), una sotto l'altra.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                deals.forEach { deal ->
                    DealCardView(
                        deal = deal,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onNavigate(DealsRoute.detail(deal.id)) },
                    )
                }
            }
            SeeAllRow(onClick = { onNavigate(if (snapshot.regionKey.isBlank()) DealsRoute.LIST else DealsRoute.listInRegion(snapshot.regionKey)) }, label = tr("local_group.deals.all", "Tutte le convenzioni"))
        }
    }
}

private const val MAX_DEALS = 3

private fun LazyListScope.adminEntry(snapshot: LocalGroupSnapshot, onNavigate: (String) -> Unit) {
    if (!snapshot.isAdmin) return
    item(key = "admin") {
        // Una sola riga che si nomina da se', senza etichetta sopra (come su iOS).
        Spacer(Modifier.height(SECTION_GAP))
        Column(Modifier.padding(horizontal = SIDE_PADDING)) {
            RowsCard {
                SectionRow(
                    title = tr("local_group.admin.title", "Area segreteria"),
                    icon = Icons.Outlined.AdminPanelSettings,
                    onClick = { onNavigate(LocalGroupRoutes.ADMIN) },
                    supporting = tr("local_group.admin.subtitle", "Soci, statistiche ed eventi della sede"),
                )
            }
        }
    }
}
