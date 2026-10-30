package it.mensa.app.features.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.support.koinAccess
import it.mensa.app.support.tr
import it.mensa.app.ui.theme.DarkStage
import it.mensa.app.ui.theme.StageSurface
import it.mensa.shared.leaderboard.LeaderboardBoard
import it.mensa.shared.leaderboard.LeaderboardKind
import it.mensa.shared.leaderboard.LeaderboardRepository
import it.mensa.shared.leaderboard.LeaderboardRow
import it.mensa.shared.leaderboard.enabledLeaderboards
import it.mensa.shared.leaderboard.RegionRow
import it.mensa.shared.leaderboard.SecretaryRow

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
    var showSettings by remember { mutableStateOf(false) }
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(tr("leaderboard.home.section", "Classifica")) },
                subtitle = {
                    Text(
                        if (kind == LeaderboardKind.MEMBERS) {
                            tr(
                                "leaderboard.subtitle_top",
                                "Ultimi 12 mesi · i primi {count}",
                                "count" to LeaderboardBoard.TOP_SIZE,
                            )
                        } else {
                            tr("leaderboard.last_year", "Ultimi 12 mesi")
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("app.back", "Indietro"))
                    }
                },
                actions = {
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = tr("leaderboard.how_it_works", "Come funziona"))
                    }
                    // Leaving the ranking only concerns the members' one.
                    if (LeaderboardKind.MEMBERS in kinds) {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = tr("leaderboard.settings", "Impostazioni"))
                        }
                    }
                },
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
        when {
            kind == null -> Text(
                text = tr("leaderboard.disabled", "La classifica non è attiva."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            )
            kind == LeaderboardKind.REGIONS -> RegionsList(regionBoards, contentPadding, switch) { detail = it }
            kind == LeaderboardKind.SECRETARIES -> SecretariesList(regionBoards, contentPadding, switch) { detail = it }
            board != null -> LazyColumn(
                contentPadding = contentPadding,
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
                modifier = Modifier.fillMaxSize(),
            ) {
                item(key = "switch") { switch() }
                item(key = "me") { MyPlace(board) }
                item(key = "label") {
                    SectionTitle(tr("leaderboard.top_label", "I primi {count}", "count" to LeaderboardBoard.TOP_SIZE))
                }
                // The first three on the podium, the rest of the ten below.
                val top = board.top
                item(key = "podium") {
                    LeaderboardPodium(
                        rows = top.take(PODIUM_SIZE),
                        onMemberClick = onMemberClick,
                        modifier = Modifier.padding(bottom = 16.dp),
                    )
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
            }
            state.failed -> Text(
                text = tr("leaderboard.load_error", "Non riesco a leggere la classifica. Riprova più tardi."),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            )
            else -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                LoadingIndicator()
            }
        }
    }

    if (showInfo) LeaderboardInfoSheet(kinds = kinds, onDismiss = { showInfo = false })
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

// ── Regions and secretaries ──────────────────────────────────────────────────

@Composable
private fun RegionsList(
    state: RegionBoardsState,
    contentPadding: PaddingValues,
    switch: @Composable () -> Unit,
    onClick: (RegionRow) -> Unit,
) {
    RankingList(state.regions, state.failed, contentPadding, switch, isMine = { it.score < LeaderboardRepository.VISIBLE_MIN_SCORE }) { rows ->
        itemsIndexed(rows, key = { _, row -> row.id }) { index, row -> RegionItem(row, index, rows.size) { onClick(row) } }
    }
}

@Composable
private fun SecretariesList(
    state: RegionBoardsState,
    contentPadding: PaddingValues,
    switch: @Composable () -> Unit,
    onClick: (SecretaryRow) -> Unit,
) {
    RankingList(state.secretaries, state.failed, contentPadding, switch, isMine = { it.score < LeaderboardRepository.VISIBLE_MIN_SCORE }) { rows ->
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
                item(key = "label") { SectionTitle(tr("leaderboard.last_year", "Ultimi 12 mesi")) }
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
                Box(Modifier.widthIn(min = 28.dp), contentAlignment = Alignment.Center) {
                    val tier = podiumTier(row.position)
                    if (tier != null) {
                        Icon(Icons.Filled.MilitaryTech, contentDescription = "${row.position}°", tint = tier.metal.mid)
                    } else {
                        Text(
                            "${row.position}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
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
