package it.mensa.app.features.notifications

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import it.mensa.app.features.notifications._components.NotificationRow
import it.mensa.app.navigation.DeepLinkHandler
import it.mensa.app.support.tr
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.MensaScaffold
import androidx.compose.material3.LargeTopAppBar
import it.mensa.shared.model.NotificationModel
import org.koin.androidx.compose.koinViewModel
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem

/**
 * NotificationsListScreen — M3 Expressive restyled.
 * MensaTopAppBar Large con kicker "NOVITÀ", FilterChip Tutte/Non lette,
 * SectionHeader per sezioni temporali, NotificationRow in MensaCard, swipe-to-dismiss.
 *
 * IconBadge discipline: Primary (non letto), Cyan (event/deal), Tertiary (sistema)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsListScreen(
    navController: NavController,
    onBack: () -> Unit = { navController.popBackStack() },
    vm: NotificationsListViewModel = koinViewModel(),
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    var menuExpanded by remember { mutableStateOf(false) }
    var showUnsupportedAlert by remember { mutableStateOf(false) }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    if (showUnsupportedAlert) {
        AlertDialog(
            onDismissRequest = { showUnsupportedAlert = false },
            title = { Text(tr("notifications.unsupported.title", fallback = "Notifica non navigabile")) },
            text = { Text(tr("notifications.unsupported.body", fallback = "Questa notifica non ha un contenuto collegato.")) },
            confirmButton = {
                TextButton(onClick = { showUnsupportedAlert = false }) { Text("OK") }
            },
        )
    }

    MensaScaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(tr("notifications.title", fallback = "Notifiche")) },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("common.back", fallback = "Indietro"))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = tr("notifications.actions.more", fallback = "Altre azioni"))
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(tr("notifications.actions.mark_all_read", fallback = "Leggi tutto")) },
                                leadingIcon = { Icon(Icons.Outlined.CheckCircleOutline, null) },
                                enabled = uiState.unreadCount > 0,
                                onClick = { menuExpanded = false; vm.markAllSeen() },
                            )
                            DropdownMenuItem(
                                text = { Text(tr("notifications.actions.preferences", fallback = "Preferenze")) },
                                leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                                onClick = { menuExpanded = false; navController.navigate(NotificationsRoutes.MANAGER) },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Filter chips: Tutte / Non lette
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 8.dp),
                ) {
                    items(NotificationFilter.values()) { filter ->
                        FilterChip(
                            selected = uiState.filter == filter,
                            onClick = { vm.setFilter(filter) },
                            label = { Text(tr(filter.labelKey, fallback = filter.fallback)) },
                        )
                    }
                }

                when {
                    uiState.loading && uiState.notifications.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) { LoadingDots() }
                    }

                    uiState.filtered.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = tr("notifications.empty.title", fallback = "Nessuna notifica"),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tr("notifications.empty.body", fallback = "Le tue notifiche appariranno qui"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection),
                            contentPadding = PaddingValues(bottom = 32.dp),
                        ) {
                            uiState.sections.forEach { section ->
                                item(key = "header_${section.group.name}") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 24.dp, bottom = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            tr(section.group.titleKey, fallback = section.group.fallback),
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                                items(
                                    items = section.items,
                                    key = { it.id },
                                ) { notification ->
                                    SwipeableNotificationRow(
                                        notification = notification,
                                        onTap = {
                                            vm.markSeen(notification)
                                            val target = notificationTarget(notification)
                                            if (target != null) {
                                                DeepLinkHandler.handleNotificationTarget(
                                                    target = target,
                                                    navController = navController,
                                                )
                                            } else {
                                                navController.navigate(
                                                    NotificationsRoutes.detail(notification.id),
                                                )
                                            }
                                        },
                                        onMarkSeen = { vm.markSeen(notification) },
                                        onDelete = { vm.delete(notification) },
                                    )
                                    HorizontalDivider(
                                        modifier = Modifier.padding(start = 68.dp),
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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

// ─── Swipeable wrapper ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableNotificationRow(
    notification: NotificationModel,
    onTap: () -> Unit,
    onMarkSeen: () -> Unit,
    onDelete: () -> Unit,
) {
    val isUnread = notification.seen == null

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (isUnread) onMarkSeen()
                    false
                }
                else -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = isUnread,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            SwipeDismissBackground(
                currentValue = dismissState.dismissDirection,
                isUnread = isUnread,
            )
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .clickable(onClick = onTap),
        ) {
            NotificationRow(
                notification = notification,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeDismissBackground(
    currentValue: SwipeToDismissBoxValue,
    isUnread: Boolean,
) {
    val isDelete = currentValue == SwipeToDismissBoxValue.EndToStart
    val isMarkRead = currentValue == SwipeToDismissBoxValue.StartToEnd && isUnread

    val bgColor by animateColorAsState(
        targetValue = when {
            isDelete -> MaterialTheme.colorScheme.errorContainer
            isMarkRead -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surface
        },
        label = "swipe_bg",
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isDelete) Arrangement.End else Arrangement.Start,
    ) {
        when {
            isDelete -> Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = tr("notifications.actions.delete", fallback = "Elimina"),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            isMarkRead -> Icon(
                imageVector = Icons.Outlined.MarkEmailRead,
                contentDescription = tr("notifications.actions.mark_read", fallback = "Segna come letta"),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}
