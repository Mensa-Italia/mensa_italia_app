package it.mensa.app.features.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.app.ui.components.ColumnSplitBox
import it.mensa.app.ui.theme.DarkStage
import it.mensa.app.ui.theme.StageSurface
import it.mensa.shared.leaderboard.LeaderboardBoard
import it.mensa.shared.leaderboard.LeaderboardKind
import it.mensa.shared.leaderboard.LeaderboardRepository
import it.mensa.shared.leaderboard.LeaderboardRow
import it.mensa.shared.leaderboard.enabledLeaderboards
import it.mensa.shared.leaderboard.RegionRow
import it.mensa.shared.leaderboard.SecretaryRow
import it.mensa.shared.time.Season

/**
 * The members' ranking: where the member stands and how far the next place
 * is, then the first ten. Material 3 Expressive components throughout
 * (flexible app bar, segmented list, loading indicator). Mirrors iOS
 * `LeaderboardView`.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LeaderboardScreen(
    onNavigateBack: () -> Unit,
    onMemberClick: (String) -> Unit = {},
    /** Opens with the hall of fame already up: for the preview harness. */
    startWithHall: Boolean = false,
) = DarkStage {
    val state = rememberLeaderboard()
    val board = state.board
    val regionBoards = rememberRegionBoards()
    // The rankings switched on in `configs`; the switch shows only those.
    val featureFlags = remember { koinAccess().featureFlags }
    val flags by featureFlags.flags.collectAsState()
    val kinds = remember(flags) { featureFlags.enabledLeaderboards() }
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val kind = kinds.getOrNull(tab) ?: kinds.firstOrNull()
    var showInfo by remember { mutableStateOf(false) }
    var showHall by remember { mutableStateOf(startWithHall) }
    var showSettings by remember { mutableStateOf(false) }
    val hall = rememberHall().hall
    // The season the rankings are for, as the rows say; the current one before they are read.
    val seasonLabel = Season.label(board?.season ?: regionBoards.regions?.firstOrNull { it.season > 0 }?.season ?: Season.current())
    // A region's or secretary's row, tapped: its calculation in a sheet.
    var detail by remember { mutableStateOf<Any?>(null) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val tabs = kinds.map {
        when (it) {
            LeaderboardKind.MEMBERS -> tr("leaderboard.tab.members", "Soci")
            LeaderboardKind.REGIONS -> tr("leaderboard.tab.regions", "Regioni")
            LeaderboardKind.SECRETARIES -> tr("leaderboard.tab.secretaries", "Segretari")
        }
    }

    // A screen this wide (a foldable open, a tablet): two columns, the
    // podium and the member's place on the left, sized so it never scrolls,
    // the switch and the rankings on the right; on a foldable they meet at
    // the hinge. Mirrors iOS `columnSplit`.
    ColumnSplitBox(Modifier.fillMaxSize()) { columns ->
    val split = columns?.takeIf { LeaderboardKind.MEMBERS in kinds }
    val wide = split != null
    val navigationIcon: @Composable () -> Unit = {
        IconButton(onClick = onNavigateBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("app.back", "Indietro"))
        }
    }
    val actions: @Composable RowScope.() -> Unit = {
        IconButton(onClick = { showHall = true }) {
            Icon(Icons.Outlined.WorkspacePremium, contentDescription = tr("leaderboard.hall.title", "Albo d'oro"))
        }
        IconButton(onClick = { showInfo = true }) {
            Icon(Icons.Outlined.Info, contentDescription = tr("leaderboard.how_it_works", "Come funziona"))
        }
        // Leaving the ranking only concerns the members' one.
        if (LeaderboardKind.MEMBERS in kinds) {
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Outlined.Settings, contentDescription = tr("leaderboard.settings", "Impostazioni"))
            }
        }
    }
    Scaffold(
        // Over two columns the bar neither collapses, which moved the column
        // that wasn't scrolling, nor has a title, which belongs to neither.
        modifier = if (wide) Modifier else Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            if (wide) {
                TopAppBar(
                    title = {},
                    navigationIcon = navigationIcon,
                    actions = actions,
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            } else LargeFlexibleTopAppBar(
                title = { Text(tr("leaderboard.home.section", "Classifica")) },
                subtitle = {
                    Text(
                        if (kind == LeaderboardKind.MEMBERS) {
                            tr(
                                "leaderboard.subtitle_top_v2",
                                "Stagione {season} · i primi {count}",
                                "season" to seasonLabel,
                                "count" to LeaderboardBoard.TOP_SIZE,
                            )
                        } else {
                            tr("leaderboard.season", "Stagione {season}", "season" to seasonLabel)
                        },
                    )
                },
                navigationIcon = navigationIcon,
                actions = actions,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = StageSurface,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        val contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + 24.dp,
        )
        // Soci, regioni, segretari: one page, one switch at the top; no
        // switch when only one ranking is on.
        val switch: @Composable () -> Unit = {
            if (tabs.size > 1) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    tabs.forEachIndexed { index, label ->
                        SegmentedButton(
                            selected = kind == kinds[index],
                            onClick = { tab = index },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                        ) { Text(label) }
                    }
                }
            }
        }
        val rankings: @Composable (PaddingValues, Boolean) -> Unit = { listPadding, withSummary ->
            when {
                kind == LeaderboardKind.REGIONS -> RegionsList(regionBoards, seasonLabel, listPadding, switch) { detail = it }
                kind == LeaderboardKind.SECRETARIES -> SecretariesList(regionBoards, seasonLabel, listPadding, switch) { detail = it }
                board != null -> MembersList(board, listPadding, switch, withSummary, onMemberClick)
                state.failed -> Text(
                    text = tr("leaderboard.load_error", "Non riesco a leggere la classifica. Riprova più tardi."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxSize().padding(listPadding).padding(32.dp),
                )
                else -> Box(Modifier.fillMaxSize().padding(listPadding), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }
            }
        }
        when {
            split != null -> Row(Modifier.fillMaxSize()) {
                PodiumColumn(
                    board = board,
                    // The spotlights hang from the top of the screen; the
                    // podium itself starts under the bar.
                    top = padding.calculateTopPadding(),
                    bottom = padding.calculateBottomPadding() + 16.dp,
                    onMemberClick = onMemberClick,
                    modifier = Modifier.width(split.leftWidth).fillMaxHeight(),
                )
                Spacer(Modifier.width(split.gap))
                Box(Modifier.weight(1f)) {
                    rankings(
                        PaddingValues(
                            start = 0.dp,
                            end = 16.dp,
                            top = contentPadding.calculateTopPadding(),
                            bottom = contentPadding.calculateBottomPadding(),
                        ),
                        false,
                    )
                }
            }
            kind == null -> Text(
                text = tr("leaderboard.disabled", "La classifica non è attiva."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            )
            else -> rankings(contentPadding, true)
        }
    }
    }

    if (showInfo) LeaderboardInfoSheet(kinds = kinds, onDismiss = { showInfo = false })
    if (showHall) LeaderboardHallSheet(hall = hall, kinds = kinds, onDismiss = { showHall = false })
    if (showSettings) LeaderboardSettingsSheet(onDismiss = { showSettings = false })
    when (val d = detail) {
        is RegionRow -> RegionScoreSheet(score = d, subtitle = null, onOpenMember = null, onDismiss = { detail = null })
        is SecretaryRow -> RegionScoreSheet(
            score = d,
            subtitle = d.displayName,
            onOpenMember = if (d.member != null && !d.isMe) {
                { detail = null; onMemberClick(d.id) }
            } else null,
            onDismiss = { detail = null },
        )
    }
}

private const val PODIUM_SIZE = 3

/**
 * The members' ranking: on a phone the member's place and the podium first
 * ([withSummary]), then the rest of the ten and the member's own row.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MembersList(
    board: LeaderboardBoard,
    contentPadding: PaddingValues,
    switch: @Composable () -> Unit,
    withSummary: Boolean,
    onMemberClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "switch") { switch() }
        val top = board.top
        if (withSummary) {
            item(key = "me") { MyPlace(board) }
            item(key = "label") {
                SectionTitle(tr("leaderboard.top_label", "I primi {count}", "count" to LeaderboardBoard.TOP_SIZE))
            }
            // The first three on the podium, the rest of the ten below.
            item(key = "podium") {
                LeaderboardPodium(
                    rows = top.take(PODIUM_SIZE),
                    onMemberClick = onMemberClick,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
        val rest = top.drop(PODIUM_SIZE)
        itemsIndexed(rest, key = { _, row -> row.id }) { index, row ->
            RankItem(row, index, rest.size, onMemberClick)
        }
        // Below the first ten only the member's own row, after a gap
        // that says places are skipped.
        board.meBelowTop?.let { me ->
            item(key = "gap") {
                Icon(
                    Icons.Outlined.MoreHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                )
            }
            item(key = "me_row") { RankItem(me, 0, 1, onMemberClick) }
        }
        item(key = "footer") {
            Text(
                text = tr("leaderboard.footer_v2", "Dal 1° ottobre al 30 settembre. A pari punti, pari posizione."),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 12.dp),
            )
        }
    }
}

/**
 * The left column on a wide screen: the podium on top, scaled to the height
 * the member's place leaves it, and the member's place under it. It never
 * scrolls. Mirrors iOS `podiumColumn`.
 */
@Composable
private fun PodiumColumn(
    board: LeaderboardBoard?,
    top: Dp,
    bottom: Dp,
    onMemberClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (board == null) {
        Box(modifier)
        return
    }
    Column(modifier.padding(start = 16.dp, bottom = bottom), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        FitHeight(Modifier.weight(1f).fillMaxWidth()) {
            LeaderboardPodium(
                rows = board.top.take(PODIUM_SIZE),
                onMemberClick = onMemberClick,
                spotlightReach = top,
            )
        }
        MyPlaceCard(board)
    }
}

/**
 * Lays its content out at its own height, then scales it to the height it
 * is offered (up to [maxScale]), keeping it on top and as wide as itself.
 */
@Composable
private fun FitHeight(modifier: Modifier = Modifier, maxScale: Float = 1.3f, content: @Composable () -> Unit) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val child = measurables.first()
        val width = constraints.maxWidth
        val natural = child.maxIntrinsicHeight(width).coerceAtLeast(1)
        val scale = (constraints.maxHeight.toFloat() / natural).coerceAtMost(maxScale)
        val placeable = child.measure(Constraints.fixed((width / scale).toInt(), natural))
        layout(width, constraints.maxHeight) {
            placeable.placeWithLayer((width - placeable.width) / 2, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0f)
            }
        }
    }
}

// ── Regions and secretaries ──────────────────────────────────────────────────

@Composable
private fun RegionsList(
    state: RegionBoardsState,
    seasonLabel: String,
    contentPadding: PaddingValues,
    switch: @Composable () -> Unit,
    onClick: (RegionRow) -> Unit,
) {
    RankingList(state.regions, state.failed, seasonLabel, contentPadding, switch, isMine = { it.score < LeaderboardRepository.VISIBLE_MIN_SCORE }) { rows ->
        itemsIndexed(rows, key = { _, row -> row.id }) { index, row -> RegionItem(row, index, rows.size) { onClick(row) } }
    }
}

@Composable
private fun SecretariesList(
    state: RegionBoardsState,
    seasonLabel: String,
    contentPadding: PaddingValues,
    switch: @Composable () -> Unit,
    onClick: (SecretaryRow) -> Unit,
) {
    RankingList(state.secretaries, state.failed, seasonLabel, contentPadding, switch, isMine = { it.score < LeaderboardRepository.VISIBLE_MIN_SCORE }) { rows ->
        itemsIndexed(rows, key = { _, row -> row.id }) { index, row -> SecretaryItem(row, index, rows.size) { onClick(row) } }
    }
}

/**
 * The switch, then the rows; the loading indicator or the error while there
 * are none. Rows under the public threshold come only to their own staff
 * (see `LeaderboardRepository.VISIBLE_MIN_SCORE`): they go after a gap that
 * says places are skipped, like the member's own row in the members' list.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun <T> RankingList(
    rows: List<T>?,
    failed: Boolean,
    seasonLabel: String,
    contentPadding: PaddingValues,
    switch: @Composable () -> Unit,
    isMine: (T) -> Boolean,
    items: LazyListScope.(List<T>) -> Unit,
) {
    LazyColumn(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = "switch") { switch() }
        when {
            rows != null -> {
                val (mine, visible) = rows.partition(isMine)
                item(key = "label") { SectionTitle(tr("leaderboard.season", "Stagione {season}", "season" to seasonLabel)) }
                items(visible)
                if (mine.isNotEmpty()) {
                    item(key = "gap") {
                        Icon(
                            Icons.Outlined.MoreHoriz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        )
                    }
                    items(mine)
                }
                item(key = "footer") {
                    Text(
                        text = tr(
                            "leaderboard.footer_regions_v4",
                            "In classifica le regioni con almeno {min} punti; chi fa parte dello staff di una regione vede anche la sua, qui sotto. A pari punti, pari posizione. Tocca una riga per il conto.",
                            "min" to LeaderboardRepository.VISIBLE_MIN_SCORE,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 12.dp),
                    )
                }
            }
            failed -> item(key = "error") {
                Text(
                    text = tr("leaderboard.load_error", "Non riesco a leggere la classifica. Riprova più tardi."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
            else -> item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(top = 64.dp), contentAlignment = Alignment.Center) { LoadingIndicator() }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
    )
}

// ── The member's place ───────────────────────────────────────────────────────

@Composable
private fun MyPlace(board: LeaderboardBoard) {
    Column {
        SectionTitle(tr("leaderboard.my_place", "La tua posizione"))
        MyPlaceCard(board)
    }
}

@Composable
private fun MyPlaceCard(board: LeaderboardBoard) {
    val me = board.me
    Card(Modifier.fillMaxWidth()) {
        if (me == null) {
            ListItem(
                leadingContent = { Icon(Icons.Outlined.EmojiEvents, contentDescription = null) },
                headlineContent = { Text(tr("leaderboard.unranked.title", "Non sei ancora in classifica")) },
                supportingContent = {
                    Text(tr("leaderboard.unranked.detail", "Raccogli il primo timbro a un evento: da lì inizi a fare punti."))
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
        } else {
            RankedPlace(board, me)
        }
    }
}

@Composable
private fun RankedPlace(board: LeaderboardBoard, me: LeaderboardRow) {
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RankAvatar(row = me, size = 48.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${me.position}°", style = MaterialTheme.typography.displaySmall)
                Text(
                    me.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${me.score}", style = MaterialTheme.typography.headlineSmall)
                Text(
                    tr("leaderboard.points_label", "punti"),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.size(12.dp))
        val gap = board.pointsToNext
        val next = board.nextPosition
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (gap == null) Icons.Outlined.EmojiEvents else Icons.AutoMirrored.Outlined.TrendingUp,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = if (gap != null && next != null) {
                    tr(
                        "leaderboard.to_next",
                        "Ti mancano {points} punti per salire al {position}° posto",
                        "points" to gap,
                        "position" to next,
                    )
                } else {
                    tr("leaderboard.leading", "Sei in testa alla classifica")
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Breakdown(me)
    }
}

/**
 * Where the points come from, as one stacked bar with its legend: each part
 * as wide as its share of the score. Mirrors iOS `Breakdown` (a Swift
 * Charts bar with the chart's legend).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Breakdown(row: LeaderboardRow) {
    val scheme = MaterialTheme.colorScheme
    val parts = listOf(
        Triple(tr("leaderboard.source.stamps", "Timbri"), row.stampsPoints, scheme.primary),
        Triple(tr("leaderboard.source.medals", "Medaglie"), row.medalPoints, scheme.tertiary),
        Triple(tr("leaderboard.source.events", "Eventi organizzati"), row.eventsPoints, scheme.secondary),
    ).filter { it.second > 0 }
    if (parts.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            parts.forEach { (_, points, color) ->
                Box(
                    Modifier
                        .weight(points.toFloat())
                        .fillMaxHeight()
                        .background(color),
                )
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            parts.forEach { (label, points, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "$label $points",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

// ── A row ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun RankItem(row: LeaderboardRow, index: Int, count: Int, onMemberClick: (String) -> Unit) {
    val opens = row.member != null && !row.isMe
    SegmentedListItem(
        onClick = { if (opens) onMemberClick(row.id) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = if (row.isMe) {
            ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Place(row.position, row.trend)
                Spacer(Modifier.width(12.dp))
                RankAvatar(row = row, size = 40.dp)
            }
        },
        trailingContent = {
            Text(
                tr("leaderboard.points", "{points} pt", "points" to row.score),
                style = MaterialTheme.typography.titleMedium,
            )
        },
    ) {
        Text(
            text = if (row.isMe) "${row.displayName} · ${tr("leaderboard.you", "Tu")}" else row.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
