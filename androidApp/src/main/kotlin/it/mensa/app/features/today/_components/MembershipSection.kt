package it.mensa.app.features.today._components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.foundation.layout.size
import it.mensa.app.features.tableport.medals.rememberMedals
import it.mensa.app.features.leaderboard.LeaderboardMembershipRow
import it.mensa.app.features.leaderboard.rememberHall
import it.mensa.app.features.leaderboard.rememberLeaderboard
import it.mensa.app.ui.components.mensaImageLoader
import it.mensa.app.features.tableport.util.StampImagePrefetcher
import it.mensa.app.support.koinAccess
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import it.mensa.shared.leaderboard.LeaderboardKind
import it.mensa.shared.leaderboard.enabledLeaderboards
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import it.mensa.app.support.ExternalLinks
import it.mensa.app.support.tr
import it.mensa.shared.auth.Membership
import it.mensa.shared.model.UserModel
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Where the member renews, same page as the iOS badge. */
private const val RENEWAL_URL = "https://cloud32.mensa.it/rinnovo"

private enum class MembershipStatus { Active, ExpiringSoon, Expired }

/**
 * Expiry with its status, the Tableport passport and the tickets — what used
 * to be the Card tab (and Tableport's entry in Discover), now on Today under
 * the card itself.
 */
@Composable
fun MembershipSection(
    user: UserModel,
    onTableportClick: () -> Unit,
    onTicketsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLeaderboardClick: () -> Unit = {},
    /** Almeno un ticket ancora valido: solo allora la voce "I miei ticket". */
    showTickets: Boolean = true,
) {
    val context = LocalContext.current
    val medals = rememberMedals()
    // Le classifiche accese in `configs`: con tutte spente la riga non c'e'.
    // Quella dei soci solo quando e' stata letta: senza, non ha niente da dire.
    val flags by koinAccess().featureFlags.flags.collectAsState()
    val boards = remember(flags) { koinAccess().featureFlags.enabledLeaderboards() }
    val readBoard = rememberLeaderboard().board
    val membersBoard = readBoard?.takeIf { it.rows.isNotEmpty() }
    val board = membersBoard.takeIf { LeaderboardKind.MEMBERS in boards }
    // A season that has just started has no ranking yet (only who stamps is listed):
    // the row then shows the podium of the one that closed, if there is one.
    val seasonJustStarted = LeaderboardKind.MEMBERS in boards && readBoard != null && readBoard.rows.isEmpty()
    val lastSeason = rememberHall(enabled = seasonJustStarted).hall?.latest
        ?.takeIf { seasonJustStarted && !it.members.isEmpty }
    val showLeaderboard = boards.isNotEmpty() && (LeaderboardKind.MEMBERS !in boards || board != null || lastSeason != null)
    // I timbri del passaporto su disco prima che lo si apra: chi lo sfoglia
    // li trova gia' li'. Dal database; dalla rete solo se e' ancora vuoto.
    LaunchedEffect(user.id) {
        val repo = koinAccess().stamps
        var stamps = runCatching { repo.current() }.getOrDefault(emptyList())
        if (stamps.isEmpty()) {
            runCatching { repo.refresh() }
            stamps = runCatching { repo.current() }.getOrDefault(emptyList())
        }
        StampImagePrefetcher.warmCollection(stamps, context, mensaImageLoader(context))
    }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SectionLabel(
            title = tr("card.membership_kicker", fallback = "Iscrizione"),
            icon = Icons.Outlined.Badge,
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = tr("card.expiry_label", fallback = "Scadenza"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatExpiry(user),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                when (membershipStatus(user)) {
                    MembershipStatus.Expired -> FilledTonalButton(
                        onClick = { ExternalLinks.open(context, RENEWAL_URL) },
                    ) {
                        Text(tr("card.renew_cta", fallback = "Rinnova"))
                    }
                    MembershipStatus.Active -> SuggestionChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(tr("card.active", fallback = "Attiva")) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        ),
                        border = null,
                    )
                    MembershipStatus.ExpiringSoon -> AssistChip(
                        onClick = {},
                        enabled = false,
                        label = { Text(tr("card.expiring_soon", fallback = "In scadenza")) },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            disabledLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                        border = null,
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(start = 20.dp))
            SectionRow(
                title = tr("addons.stamp.short_title", fallback = "Tableport"),
                icon = Icons.Outlined.QrCodeScanner,
                onClick = onTableportClick,
                // The medals earned, beside the chevron: a reason to look
                // inside and find out what they are.
                medalCount = medals.earnedCount.takeIf { medals.medals.isNotEmpty() },
            )
            // La classifica sta con il Tableport: si fa con i suoi timbri.
            // Arriva dalla rete dopo il resto della card: entra sfumando e
            // aprendosi, invece di comparire di colpo.
            AnimatedVisibility(visible = showLeaderboard, enter = fadeIn() + expandVertically()) {
                Column {
                    HorizontalDivider(modifier = Modifier.padding(start = 20.dp))
                    LeaderboardMembershipRow(board = board, onClick = onLeaderboardClick, lastSeason = lastSeason)
                }
            }
            // Solo con un ticket ancora valido: una voce che apre una lista
            // vuota o di ticket scaduti era rumore.
            if (showTickets) {
                HorizontalDivider(modifier = Modifier.padding(start = 20.dp))
                SectionRow(
                    title = tr("card.nav_tickets", fallback = "I miei ticket"),
                    icon = Icons.Outlined.ConfirmationNumber,
                    onClick = onTicketsClick,
                )
            }
        }
    }
}

@Composable
private fun SectionRow(title: String, icon: ImageVector, onClick: () -> Unit, medalCount: Int? = null) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (medalCount != null) {
                    val medalsLabel = tr("medals.count_a11y", "%d medaglie").replace("%d", "$medalCount")
                    Icon(
                        imageVector = Icons.Filled.MilitaryTech,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = "$medalCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.semantics {
                            contentDescription = medalsLabel
                        },
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick, role = Role.Button),
    )
}

/** Expired by the same rule as the renewal wall (`Membership` in :shared). */
private fun membershipStatus(user: UserModel): MembershipStatus {
    if (Membership.isExpired(user)) return MembershipStatus.Expired
    val left = user.expireMembership.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds()
    val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
    return if (left < thirtyDaysMs) MembershipStatus.ExpiringSoon else MembershipStatus.Active
}

private fun formatExpiry(user: UserModel): String = try {
    val local = user.expireMembership.toLocalDateTime(TimeZone.currentSystemDefault())
    val day = local.dayOfMonth.toString().padStart(2, '0')
    val month = local.monthNumber.toString().padStart(2, '0')
    "$day/$month/${local.year}"
} catch (_: Exception) {
    "—"
}
