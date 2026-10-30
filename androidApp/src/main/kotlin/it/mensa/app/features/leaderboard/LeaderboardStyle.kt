package it.mensa.app.features.leaderboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import it.mensa.app.features.members._components.MemberAvatar
import it.mensa.app.features.members._components.MemberHeroAvatar
import it.mensa.app.features.tableport.medals.MedalTier
import it.mensa.app.support.koinAccess
import it.mensa.shared.leaderboard.LeaderboardBoard
import it.mensa.shared.leaderboard.LeaderboardRow
import it.mensa.shared.leaderboard.RegionRow
import it.mensa.shared.leaderboard.SecretaryRow
import it.mensa.shared.model.RegSociModel

/** The podium's metal for a position: gold, silver, bronze; null past third. */
internal fun podiumTier(position: Int): MedalTier? = when (position) {
    1 -> MedalTier.Gold
    2 -> MedalTier.Silver
    3 -> MedalTier.Bronze
    else -> null
}

/** Ranking state for a screen: the board, and whether the last read failed with nothing to show. */
internal data class LeaderboardState(val board: LeaderboardBoard?, val failed: Boolean)

/** The ranking, reread when the caller appears. Shared through the repository. */
@Composable
internal fun rememberLeaderboard(): LeaderboardState {
    val repository = remember { koinAccess().leaderboard }
    val board by repository.observe().collectAsState(initial = null)
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(repository) {
        failed = runCatching { repository.refresh() }.isFailure && board == null
    }
    return LeaderboardState(board, failed)
}

/** The regions' and secretaries' rankings; null until read, [failed] when the first read failed. */
internal data class RegionBoardsState(
    val regions: List<RegionRow>?,
    val secretaries: List<SecretaryRow>?,
    val failed: Boolean,
)

/** The regions' and secretaries' rankings, reread when the caller appears. */
@Composable
internal fun rememberRegionBoards(): RegionBoardsState {
    val repository = remember { koinAccess().leaderboard }
    val regions by repository.observeRegions().collectAsState(initial = null)
    val secretaries by repository.observeSecretaries().collectAsState(initial = null)
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(repository) {
        failed = runCatching { repository.refreshRegions() }.isFailure && regions == null
    }
    return RegionBoardsState(regions, secretaries, failed)
}

/**
 * A member's face, as in the directory: the photo, or their initials when
 * there is none or the directory isn't cached yet. [hiRes] when it is large
 * on screen (the podium): the photo at full size over the list's thumbnail,
 * which looked grainy there.
 */
@Composable
internal fun RankAvatar(row: LeaderboardRow, size: Dp, modifier: Modifier = Modifier, hiRes: Boolean = false) {
    val member = row.member ?: remember(row.id) { RegSociModel(id = row.id, name = row.displayName) }
    if (hiRes) {
        MemberHeroAvatar(member = member, size = size, modifier = modifier)
    } else {
        MemberAvatar(member = member, size = size, modifier = modifier)
    }
}

@Composable
internal fun RankAvatar(row: SecretaryRow, size: Dp, modifier: Modifier = Modifier) {
    val member = row.member ?: remember(row.id) { RegSociModel(id = row.id, name = row.displayName) }
    MemberAvatar(member = member, size = size, modifier = modifier)
}
