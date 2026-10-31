package it.mensa.app.features.leaderboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.mensa.app.support.tr
import it.mensa.shared.leaderboard.HallSeason
import it.mensa.shared.leaderboard.LeaderboardBoard

/**
 * The ranking's row in Today's membership card, right under Tableport: the
 * member's place beside the chevron (in its metal on the podium), and how
 * far the next place is. With no [board] (the members' ranking switched
 * off, only regions or secretaries on) it just opens the page. While the
 * season has no ranking yet (it starts on 1 October), [lastSeason] shows the
 * podium of the one that closed, under its name. Mirrors iOS
 * `LeaderboardMembershipRow`.
 */
@Composable
fun LeaderboardMembershipRow(board: LeaderboardBoard?, onClick: () -> Unit, lastSeason: HallSeason? = null) {
    val me = board?.me
    val gap = board?.pointsToNext
    val next = board?.nextPosition
    val detail = when {
        lastSeason != null -> null
        board == null -> tr("leaderboard.home.regions", "Regioni e segretari")
        me != null && gap != null && next != null ->
            tr("leaderboard.home.to_next", "{points} punti al {position}° posto", "points" to gap, "position" to next)
        me != null -> tr("leaderboard.leading", "Sei in testa alla classifica")
        else -> board.podium.firstOrNull()?.let {
            tr("leaderboard.home.leader", "In testa {name}", "name" to it.displayName)
        }
    }
    ListItem(
        headlineContent = { Text(tr("leaderboard.home.section", "Classifica")) },
        supportingContent = when {
            lastSeason != null -> ({ LastSeasonPodium(lastSeason) })
            detail != null -> ({ Text(detail) })
            else -> null
        },
        leadingContent = {
            Icon(Icons.Outlined.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                me?.let { PlaceBadge(it.position) }
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick, role = Role.Button),
    )
}

/** The closed season's name and its first three places, one line each. */
@Composable
private fun LastSeasonPodium(season: HallSeason) {
    Column {
        Text(
            text = tr("leaderboard.home.last_season", "Stagione {season}", "season" to season.label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        season.members.podium.forEach { entry ->
            Text(
                text = "${entry.position}° ${entry.displayName}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun PlaceBadge(position: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        podiumTier(position)?.let { tier ->
            Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = tier.metal.mid)
        }
        Text(
            text = "$position°",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
