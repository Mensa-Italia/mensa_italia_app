package it.mensa.app.features.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.card._components.MembershipCardHero
import it.mensa.app.features.events._components.EventRowCard
import it.mensa.app.features.today._components.NotificationsPreview
import it.mensa.app.features.today._components.SectionLabel
import it.mensa.app.support.FilesUrl
import it.mensa.app.support.tr
import it.mensa.app.ui.components.CachedAsyncImage
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import it.mensa.app.ui.components.MensaSearchAppBar
import it.mensa.app.ui.components.SearchAppBarNotificationsButton
import it.mensa.app.ui.shell.MainTab
import it.mensa.shared.model.NotificationModel
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * TodayScreen — landing personal screen, info parity with iOS [TodayView].
 *
 * Sections (matches Swift one-to-one):
 *  1. Greeting (avatar inside the M3 SearchBar — Gmail/Photos pattern)
 *  2. Membership card hero (distinctive brand element)
 *  3. Next event hero card
 *  4. Notifications preview (max 3 + "see all")
 *
 * Design language: M3 Expressive (not Liquid Glass) — tonal containers,
 * SectionHeader with kicker + emphasized title, Card components.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(
    modifier: Modifier = Modifier,
    onNavigateToTab: (MainTab) -> Unit = {},
    onEventClick: (String) -> Unit = {},
    onDealClick: (String) -> Unit = {},
    onSigClick: (String) -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onNotificationTap: (NotificationModel) -> Unit = {},
    onTableportClick: () -> Unit = {},
    onSearchTap: () -> Unit = {},
) {
    val viewModel: TodayViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val refreshState = rememberPullToRefreshState()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    MensaScaffold(
        modifier = modifier,
        topBar = {
            MensaSearchAppBar(
                placeholder = tr("today.search_placeholder", fallback = "Cerca soci, eventi, deal…"),
                onSearchTap = onSearchTap,
                avatar = {
                    UserAvatar(
                        user = uiState.user,
                        onClick = { onNavigateToTab(MainTab.Profile) },
                    )
                },
                inlineActions = {
                    SearchAppBarNotificationsButton(
                        onClick = onNotificationsClick,
                        badge = uiState.notifications.isNotEmpty(),
                    )
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            state = refreshState,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    viewModel.refresh()
                    isRefreshing = false
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val phase = uiState.phase) {
                is TodayPhase.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        LoadingDots()
                    }
                }

                is TodayPhase.Error -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = phase.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                is TodayPhase.Ready -> {
                    TodayContent(
                        uiState = uiState,
                        onNavigateToTab = onNavigateToTab,
                        onEventClick = onEventClick,
                        onNotificationsClick = onNotificationsClick,
                        onNotificationTap = { notif ->
                            viewModel.markNotificationSeen(notif.id)
                            onNotificationTap(notif)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TodayContent(
    uiState: TodayUiState,
    onNavigateToTab: (MainTab) -> Unit,
    onEventClick: (String) -> Unit,
    onNotificationsClick: () -> Unit,
    onNotificationTap: (NotificationModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp),
    ) {
        item(key = "greeting") {
            TodayGreeting(user = uiState.user)
        }

        item(key = "membership_card") {
            val user = uiState.user
            if (user != null) {
                val fullName = remember(user.name, user.username) {
                    user.name.ifBlank { user.username }.ifBlank { "Socio Mensa" }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MembershipCardHero(
                        fullName = fullName,
                        memberId = user.id,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .clickable(
                                onClick = { onNavigateToTab(MainTab.Card) },
                                role = Role.Button,
                            ),
                    )
                }
            }
        }

        item(key = "next_event") {
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SectionLabel(
                    title = tr("app.today.next_event", fallback = "Prossimo evento"),
                    icon = Icons.Outlined.CalendarMonth,
                )
                val nextEvent = uiState.nextEvent
                if (nextEvent != null) {
                    EventRowCard(
                        event = nextEvent,
                        onClick = { onEventClick(nextEvent.id) },
                    )
                } else {
                    EmptyEventHero(onClick = { onNavigateToTab(MainTab.Discover) })
                }
            }
        }

        if (uiState.notifications.isNotEmpty()) {
            item(key = "notifications") {
                Spacer(Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    NotificationsPreview(
                        notifications = uiState.notifications,
                        onNotificationClick = onNotificationTap,
                        onSeeAllClick = onNotificationsClick,
                    )
                }
            }
        }

        item(key = "spacer_end") { Spacer(Modifier.height(40.dp)) }
    }
}

// ── Greeting (matches iOS headerSection info content) ────────────────────────

@Composable
private fun TodayGreeting(user: UserModel?) {
    val firstName = user?.name?.split(" ")?.firstOrNull()?.ifBlank { null }
        ?: user?.username?.split(".")?.firstOrNull()?.replaceFirstChar { it.titlecase() }?.ifBlank { null }
        ?: "Socio"
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..12 -> "Buongiorno"
            in 13..17 -> "Buon pomeriggio"
            in 18..23 -> "Buonasera"
            else -> "Bentornato"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = firstName,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

// ── SearchBar trailing avatar ────────────────────────────────────────────────

@Composable
private fun UserAvatar(
    user: UserModel?,
    onClick: () -> Unit,
) {
    val avatarUrl = user?.let { u ->
        if (u.avatar.isNotBlank()) {
            FilesUrl.build(
                collection = "users",
                recordId = u.id,
                filename = u.avatar,
                thumb = "100x100",
            )
        } else null
    }
    val avatarLabel = tr("today.avatar.cd", fallback = "Vai al profilo")

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .clickable(
                onClick = onClick,
                role = Role.Button,
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            avatarUrl != null -> CachedAsyncImage(
                model = avatarUrl,
                contentDescription = avatarLabel,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            else -> Icon(
                imageVector = Icons.Outlined.Person,
                contentDescription = avatarLabel,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ── Empty state for next event ───────────────────────────────────────────────

@Composable
private fun EmptyEventHero(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tr("today.no_events_title", fallback = "Nessun evento in agenda"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = tr("today.no_events_subtitle", fallback = "Esplora gli eventi pubblici"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
