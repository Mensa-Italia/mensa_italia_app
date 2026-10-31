package it.mensa.app.features.leaderboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.mensa.app.features.card._components.MembershipCardHero
import it.mensa.app.features.tableport.medals.MedalCabinetScreen
import it.mensa.app.features.today._components.SectionLabel
import it.mensa.app.support.koinAccess
import it.mensa.app.ui.components.ChangelogSheet

/**
 * Debug harness: the leaderboard on its made-up ranking, mounted without the
 * login so its screenshots can be captured on any emulator
 * (`--es mensa_screen leaderboard_preview --es mensa_leaderboard_fixture 1`,
 * `--es mensa_leaderboard_me 1` to stand on the podium).
 * `leaderboard_preview_card` shows Today's membership card, with the podium
 * stamp on its front, and the ranking's row instead; `leaderboard_preview_hall`
 * opens the hall of fame sheet; `medals_preview`
 * (with `--es mensa_medals_fixture 1`, and `--es mensa_region_medals_fixture 1` for
 * the region's page) the medal collection. Reached only through
 * [it.mensa.app.support.LaunchHarness], which is inert on release builds.
 */
@Composable
fun LeaderboardPreviewHarness(screen: String) {
    if (screen == "leaderboard_preview_card") {
        val board = rememberLeaderboard().board
        Column(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Today", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(bottom = 12.dp))
            MembershipCardHero(
                fullName = "Giorgia Bianchi",
                memberId = "12345",
                podiumPlace = board?.myPodiumPlace,
                modifier = Modifier.fillMaxWidth(),
            )
            SectionLabel(title = "Iscrizione", icon = Icons.Outlined.Badge, modifier = Modifier.padding(top = 12.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                board?.let { LeaderboardMembershipRow(board = it, onClick = {}) }
            }
        }
    } else if (screen == "medals_preview") {
        MedalCabinetScreen(onNavigateBack = {})
    } else if (screen == "changelog_preview") {
        // The release's "Novita'" sheet, to proof its texts before tagging.
        ChangelogSheet(items = koinAccess().changelog.items, onDone = {})
    } else {
        // `leaderboard_preview_hall` opens the hall of fame ("Albo d'oro") over the page.
        LeaderboardScreen(onNavigateBack = {}, startWithHall = screen == "leaderboard_preview_hall")
    }
}
