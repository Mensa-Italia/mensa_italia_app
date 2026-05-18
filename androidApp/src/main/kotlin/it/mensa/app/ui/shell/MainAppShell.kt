package it.mensa.app.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import it.mensa.app.features.addonshub.AddonsHubRoutes
import it.mensa.app.features.addonshub.addonsHubNavGraph
import it.mensa.app.features.boutique.BoutiqueRoutes
import it.mensa.app.features.boutique.boutiqueNavGraph
import it.mensa.app.features.card.CardScreen
import it.mensa.app.features.contacts.ContactsRoutes
import it.mensa.app.features.contacts.contactsNavGraph
import it.mensa.app.features.deals.DealsRoute
import it.mensa.app.features.deals.dealsNavGraph
import it.mensa.app.features.discover.DiscoverCategory
import it.mensa.app.features.discover.DiscoverScreen
import it.mensa.app.features.documents.DocumentsRoutes
import it.mensa.app.features.documents.documentsNavGraph
import it.mensa.app.features.events.EventRoutes
import it.mensa.app.features.events.eventsNavGraph
import it.mensa.app.features.external.ExternalRoutes
import it.mensa.app.features.external.externalNavGraph
import it.mensa.app.features.localoffices.LocalOfficesRoutes
import it.mensa.app.features.localoffices.localOfficesNavGraph
import it.mensa.app.features.members.MembersRoutes
import it.mensa.app.features.members.membersNavGraph
import it.mensa.app.features.notifications.AccountConfirmationController
import it.mensa.app.features.notifications.AccountConfirmationSheet
import it.mensa.app.features.notifications.NotificationsRoutes
import it.mensa.app.features.notifications.notificationsNavGraph
import it.mensa.app.features.notifications.notificationTarget
import it.mensa.app.navigation.DeepLinkHandler
import it.mensa.app.features.podcasts.PodcastsRoutes
import it.mensa.app.features.podcasts.podcastsNavGraph
import it.mensa.app.features.profile.ProfileNavGraph
import it.mensa.app.features.quid.QuidRoute
import it.mensa.app.features.quid.quidNavGraph
import it.mensa.app.features.receipts.ReceiptsRoutes
import it.mensa.app.features.receipts.receiptsNavGraph
import it.mensa.app.features.search.SearchScreen
import it.mensa.app.features.search.toDetailRoute
import it.mensa.app.features.sigs.SigsRoutes
import it.mensa.app.features.sigs.sigsNavGraph
import it.mensa.app.features.tableport.TableportRoutes
import it.mensa.app.features.tableport.tableportNavGraph
import it.mensa.app.features.testassistant.TestAssistantRoutes
import it.mensa.app.features.testassistant.testAssistantNavGraph
import it.mensa.app.features.tickets.TicketsRoutes
import it.mensa.app.features.tickets.ticketsNavGraph
import it.mensa.app.features.today.TodayScreen
import it.mensa.app.navigation.toRoute
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaNavItem
import it.mensa.app.ui.components.MensaNavigationBar
import it.mensa.app.ui.components.MiniAudioPlayer
import it.mensa.app.ui.components.NowPlayingFullScreenView
import org.koin.compose.koinInject

/**
 * MainAppShell — authenticated 5-tab shell with master NavHost.
 *
 * Shell architecture:
 * - Single [rememberNavController] shared by ALL destinations (tabs + drill).
 * - 5 tab composables: today / discover / search / card / profile
 * - 17 drill nav graphs registered at root level.
 * - NavigationBar hidden via AnimatedVisibility when on drill destinations.
 * - MiniAudioPlayer floats above NavigationBar.
 * - NowPlayingFullScreenView shown as overlay Dialog.
 *
 * Mirrors iOS MainTabView.swift with NavigationStack nested inside each tab.
 */

// ─── Tab descriptor ───────────────────────────────────────────────────────────

enum class MainTab(
    val route: String,
    val labelKey: String,
    val labelFallback: String,
    val icon: ImageVector,
) {
    Today("today", "app.tab.today", "Today", Icons.Outlined.AutoAwesome),
    Discover("discover", "app.tab.discover", "Discover", Icons.Outlined.GridView),
    Card("card", "app.tab.card", "Card", Icons.Outlined.CreditCard),
    Profile("profile", "app.tab.profile", "Profile", Icons.Outlined.AccountCircle),
}

/** Route for the global search drill destination (no longer a tab). */
object SearchRoute {
    const val ROUTE = "search"
}

private val tabRoutes = MainTab.values().map { it.route }.toSet()

// ─── Shell ────────────────────────────────────────────────────────────────────

@Composable
fun MainAppShell() {
    val audioController = koinInject<AudioPlayerController>()
    val currentTrack by audioController.currentTrack.collectAsStateWithLifecycle()
    val isPresentingFullPlayer by audioController.isPresentingFullPlayer.collectAsStateWithLifecycle()

    val accountConfirmationController = koinInject<AccountConfirmationController>()
    val accountConfirmationRequest by accountConfirmationController.current.collectAsStateWithLifecycle()

    val navController = rememberNavController()

    // Track current route for tab highlight + bottom bar visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Profile uses its own nested NavController, so master only sees route="profile".
    // ProfileNavGraph signals via callback whether it's at its own root.
    var profileAtRoot by remember { mutableStateOf(true) }

    // Show bottom bar only on the 5 root tab destinations AND, when on Profile,
    // only when Profile's nested stack is at its own root (no drill destination).
    val shouldShowBottomBar = currentDestination?.route in tabRoutes &&
        (currentDestination?.route != MainTab.Profile.route || profileAtRoot)

    Scaffold(
        bottomBar = {
            AnimatedVisibility(
                visible = shouldShowBottomBar,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 400f),
                ),
            ) {
                val activeTabRoute = currentDestination?.hierarchy
                    ?.mapNotNull { dest -> tabRoutes.firstOrNull { it == dest.route } }
                    ?.firstOrNull()
                val navItems = remember {
                    MainTab.values().map { tab ->
                        MensaNavItem(
                            route = tab.route,
                            icon = tab.icon,
                            label = tab.labelFallback,
                        )
                    }
                }
                val localizedItems = navItems.map { item ->
                    val tab = MainTab.values().first { it.route == item.route }
                    item.copy(label = tr(tab.labelKey, tab.labelFallback))
                }
                MensaNavigationBar(
                    items = localizedItems,
                    selectedRoute = activeTabRoute,
                    onItemSelect = { navItem ->
                        navController.navigate(navItem.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        contentWindowInsets = WindowInsets(0),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Master NavHost ────────────────────────────────────────────────
            NavHost(
                navController = navController,
                startDestination = MainTab.Today.route,
            ) {
                // ── Tab roots ─────────────────────────────────────────────────

                composable(MainTab.Today.route) {
                    TodayScreen(
                        onNavigateToTab = { tab -> navController.navigateToTab(tab) },
                        onEventClick = { id -> navController.navigate(EventRoutes.detail(id)) },
                        onDealClick = { id -> navController.navigate(DealsRoute.detail(id)) },
                        onSigClick = { id -> navController.navigate(SigsRoutes.detail(id)) },
                        onNotificationsClick = { navController.navigate(NotificationsRoutes.LIST) },
                        onNotificationTap = { notif ->
                            val target = notificationTarget(notif)
                            if (target != null) {
                                DeepLinkHandler.handleNotificationTarget(target, navController)
                            } else {
                                navController.navigate(NotificationsRoutes.detail(notif.id))
                            }
                        },
                        onTableportClick = { navController.navigate(TableportRoutes.PASSPORT) },
                        onSearchTap = { navController.navigate(SearchRoute.ROUTE) },
                    )
                }

                composable(MainTab.Discover.route) {
                    DiscoverScreen(
                        onCategoryClick = { category ->
                            when (category) {
                                DiscoverCategory.Card -> navController.navigateToTab(MainTab.Card)
                                else -> {
                                    val route = category.toRoute()
                                    if (route != null) navController.navigate(route)
                                }
                            }
                        },
                        onSearchTap = { navController.navigate(SearchRoute.ROUTE) },
                    )
                }

                composable(SearchRoute.ROUTE) {
                    SearchScreen(
                        onItemClick = { hit ->
                            hit.toDetailRoute()?.let { navController.navigate(it) }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }

                composable(MainTab.Card.route) {
                    CardScreen(
                        onTicketsClick = { navController.navigate(TicketsRoutes.LIST) },
                        onReceiptsClick = { navController.navigate(ReceiptsRoutes.LIST) },
                        onShareClick = { /* TODO: share card as image */ },
                        onSearchTap = { navController.navigate(SearchRoute.ROUTE) },
                    )
                }

                composable(MainTab.Profile.route) {
                    ProfileNavGraph(
                        onSearchTap = { navController.navigate(SearchRoute.ROUTE) },
                        onAtRootChange = { profileAtRoot = it },
                    )
                }

                // ── Drill nav graphs ──────────────────────────────────────────

                eventsNavGraph(navController)
                dealsNavGraph(navController)
                ticketsNavGraph(navController)
                receiptsNavGraph(navController)
                sigsNavGraph(navController)
                membersNavGraph(navController)
                localOfficesNavGraph(navController)
                notificationsNavGraph(navController)
                quidNavGraph(navController)
                podcastsNavGraph(navController)
                documentsNavGraph(navController)
                contactsNavGraph(navController)
                externalNavGraph(navController)
                tableportNavGraph(navController)
                boutiqueNavGraph(navController)
                testAssistantNavGraph(navController)
                addonsHubNavGraph(
                    navController = navController,
                    onAddonClick = { addonId -> navController.navigateAddon(addonId) },
                )
            }

            // ── MiniAudioPlayer — floats above NavigationBar ──────────────────
            if (currentTrack != null) {
                MiniAudioPlayer(
                    controller = audioController,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
                )
            }
        }
    }

    // ── Full-screen now-playing sheet (Dialog overlay) ────────────────────────
    if (isPresentingFullPlayer) {
        NowPlayingFullScreenView(
            controller = audioController,
            onDismiss = { audioController.dismissFullPlayer() },
        )
    }

    // ── Account-confirmation modal (third-party identity check) ───────────────
    accountConfirmationRequest?.let { request ->
        AccountConfirmationSheet(
            request = request,
            onDismiss = { accountConfirmationController.dismiss() },
        )
    }
}

// ─── Navigation helpers ───────────────────────────────────────────────────────

private fun NavController.navigateToTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/**
 * Maps AddonsHub [addonId] strings to the correct drill destination.
 * Convention: addonId equals the feature slug returned by the backend AddonModel.id.
 */
private fun NavController.navigateAddon(addonId: String) {
    val route = when (addonId.lowercase()) {
        "tableport", "stamp"        -> TableportRoutes.PASSPORT
        "boutique"                  -> BoutiqueRoutes.LIST
        "podcasts", "podcast"       -> PodcastsRoutes.LIST
        "contacts"                  -> ContactsRoutes.LIST
        "quid"                      -> QuidRoute.ISSUES
        "documents", "docs"         -> DocumentsRoutes.LIST
        "testassistant", "test"     -> TestAssistantRoutes.DASHBOARD
        else -> ExternalRoutes.addon(addonId)  // fallback: open as external addon
    }
    navigate(route)
}
