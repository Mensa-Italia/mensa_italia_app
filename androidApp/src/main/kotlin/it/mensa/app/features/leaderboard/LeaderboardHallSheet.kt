package it.mensa.app.features.leaderboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.features.members._components.MemberAvatar
import it.mensa.app.support.tr
import it.mensa.shared.leaderboard.HallBoard
import it.mensa.shared.leaderboard.HallEntry
import it.mensa.shared.leaderboard.HallOfFame
import it.mensa.shared.leaderboard.HallSeason
import it.mensa.shared.leaderboard.LeaderboardKind
import it.mensa.shared.model.RegSociModel

/**
 * The hall of fame ("Albo d'oro"): each closed season with the first three
 * places of the rankings that are switched on, and the member's own place when
 * it falls outside them. A bottom sheet over the leaderboard, opened from the
 * bar beside ⓘ. Mirrors iOS `LeaderboardHallSheet`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LeaderboardHallSheet(hall: HallOfFame?, kinds: List<LeaderboardKind>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                text = tr("leaderboard.hall.title", "Albo d'oro"),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            val seasons = hall?.seasons.orEmpty()
            if (seasons.isEmpty()) {
                Text(
                    text = tr("leaderboard.hall.empty", "La prima stagione si chiude il 30 settembre."),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp, horizontal = 24.dp),
                )
            } else {
                seasons.forEach { season -> HallSeasonSection(season, kinds) }
            }
        }
    }
}

@Composable
private fun HallSeasonSection(season: HallSeason, kinds: List<LeaderboardKind>) {
    Text(
        text = season.label,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp),
    )
    kinds.forEach { kind ->
        val board = season.board(kind)
        if (!board.isEmpty) HallBoardSection(kind, board)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HallBoardSection(kind: LeaderboardKind, board: HallBoard) {
    Text(
        text = when (kind) {
            LeaderboardKind.MEMBERS -> tr("leaderboard.tab.members", "Soci")
            LeaderboardKind.REGIONS -> tr("leaderboard.tab.regions", "Regioni")
            LeaderboardKind.SECRETARIES -> tr("leaderboard.tab.secretaries", "Segretari")
        },
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 8.dp),
    )
    Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
        board.podium.forEachIndexed { index, entry -> HallItem(entry, kind, index, board.podium.size) }
        board.me?.let { me ->
            Icon(
                Icons.Outlined.MoreHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
            Text(
                text = tr("leaderboard.hall.you", "Il tuo posto"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
            HallItem(me, kind, 0, 1)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HallItem(entry: HallEntry, kind: LeaderboardKind, index: Int, count: Int) {
    SegmentedListItem(
        onClick = {},
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        colors = if (entry.isMe) {
            ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
        },
        leadingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Place(entry.position)
                if (kind != LeaderboardKind.REGIONS) {
                    Spacer(Modifier.width(12.dp))
                    val member = entry.member ?: remember(entry.id) { RegSociModel(id = entry.model.subject, name = entry.displayName) }
                    MemberAvatar(member = member, size = 40.dp)
                }
            }
        },
        trailingContent = {
            Text(tr("leaderboard.points", "{points} pt", "points" to entry.score), style = MaterialTheme.typography.titleMedium)
        },
    ) {
        Text(
            text = if (entry.isMe) "${entry.displayName} · ${tr("leaderboard.you", "Tu")}" else entry.displayName,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
