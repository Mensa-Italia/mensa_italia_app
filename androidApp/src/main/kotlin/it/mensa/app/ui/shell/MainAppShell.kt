package it.mensa.app.ui.shell

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import it.mensa.app.features.contacts.ContactsRoutes
import it.mensa.app.features.deals.DealsRoute
import it.mensa.app.features.discover.DiscoverTab
import it.mensa.app.features.documents.DocumentsRoutes
import it.mensa.app.features.events.EventRoutes
import it.mensa.app.features.localoffices.LocalOfficesRoutes
import it.mensa.app.features.leaderboard.LeaderboardRoutes
import it.mensa.app.features.members.MembersRoutes
import it.mensa.app.features.notifications.AccountConfirmationHost
import it.mensa.app.features.notifications.NotificationsRoutes
import it.mensa.app.features.notifications.notificationTarget
import it.mensa.app.navigation.DeepLinkHandler
import it.mensa.app.navigation.PendingAppLink
import it.mensa.app.navigation.drillNavGraphs
import it.mensa.app.features.podcasts.PodcastsRoutes
import it.mensa.app.features.profile.ProfileNavGraph
import it.mensa.app.features.quid.QuidRoute
import it.mensa.app.features.receipts.ReceiptsRoutes
import it.mensa.app.features.search.SearchTab
import it.mensa.app.features.search.toDetailRoute
import it.mensa.app.features.sigs.SigsRoutes
import it.mensa.app.features.tableport.TableportRoutes
import it.mensa.app.features.tickets.TicketsRoutes
import it.mensa.app.features.today.TodayScreen
import it.mensa.app.navigation.toRoute
import it.mensa.app.services.audio.AudioPlayerController
import it.mensa.app.services.push.PendingPushTarget
import it.mensa.app.services.push.PushPermissionRequester
import it.mensa.app.support.ExternalLinks
import it.mensa.app.support.LaunchHarness
import it.mensa.app.support.tr
import it.mensa.app.ui.components.MensaNavItem
import it.mensa.app.ui.components.MensaNavigationBar
import it.mensa.app.ui.components.MensaNavigationRail
import it.mensa.app.ui.components.TwoColumnMinWidth
import it.mensa.app.ui.components.MiniAudioPlayer
import it.mensa.app.ui.components.NowPlayingFullScreenView
import org.koin.compose.koinInject

/**
 * MainAppShell — authenticated 5-tab shell with master NavHost.
 *
 * Shell architecture:
 * - Single [rememberNavController] shared by ALL destinations (tabs + drill).
 * - 3 tab composables: today / discover / profile (search is a drill route)
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
    Today("today", "app.tab.today", "Oggi", Icons.Outlined.AutoAwesome),
    Discover("discover", "app.tab.discover", "Scopri", Icons.Outlined.GridView),
    Profile("profile", "app.tab.profile", "Profilo", Icons.Outlined.AccountCircle),
}

/** Route for the global search drill destination (no longer a tab). */
object SearchRoute {
    const val ROUTE = "search"
}

private val tabRoutes = MainTab.values().map { it.route }.toSet()

// ─── Automation entry points ──────────────────────────────────────────────────

/**
 * Maps a [LaunchHarness] `mensa_screen` alias to a route inside this shell.
 *
 * Aliases mirror the iOS `MENSA_LAUNCH_SCREEN` values in
 * `iosApp/iosApp/App/iosAppApp.swift` so a single scene id in
 * `tools/storekit/storekit.config.json` drives both platforms.
 * Returns null for unknown aliases, which leaves the shell on its default tab.
 */
private fun harnessRoute(alias: String): String? = when (alias.lowercase()) {
    "today" -> MainTab.Today.route
    "discover" -> MainTab.Discover.route
    // The card lives on Today now.
    "card" -> MainTab.Today.route
    "profile" -> MainTab.Profile.route
    "search" -> SearchRoute.ROUTE
    "events" -> EventRoutes.LIST
    "deals" -> DealsRoute.LIST
    "sigs" -> SigsRoutes.LIST
    "members" -> MembersRoutes.DIRECTORY
    "localoffices" -> LocalOfficesRoutes.LIST
    "documents" -> DocumentsRoutes.LIST
    "notifications" -> NotificationsRoutes.LIST
    "tableport" -> TableportRoutes.PASSPORT
    "leaderboard" -> LeaderboardRoutes.BOARD
    "quid" -> QuidRoute.ISSUES
    "podcasts" -> PodcastsRoutes.LIST
    "contacts" -> ContactsRoutes.LIST
    "tickets" -> TicketsRoutes.LIST
    "receipts" -> ReceiptsRoutes.LIST
    "addonshub", "addons" -> AddonsHubRoutes.HUB
    else -> null
}

// ─── Shell ────────────────────────────────────────────────────────────────────

@Composable
fun MainAppShell() {
    val audioController = koinInject<AudioPlayerController>()
    val currentTrack by audioController.currentTrack.collectAsStateWithLifecycle()
    val isPresentingFullPlayer by audioController.isPresentingFullPlayer.collectAsStateWithLifecycle()

    val navController = rememberNavController()

    // Permesso notifiche (Android 13+). Il composable esisteva da sempre ma non
    // lo chiamava nessuno, quindi il permesso non veniva mai chiesto e le push
    // non si sarebbero viste comunque. Sta qui, come su iOS in MainTabView:
    // dopo auth e onboarding, non sulla login. Il token FCM viene rilasciato
    // anche senza permesso, quindi la registrazione del device (MainActivity.
    // onResume) non dipende da questo.
    PushPermissionRequester()

    // Automation entry point (screenshot capture). A tab alias becomes the
    // NavHost start destination so the shell opens directly on it; a drill
    // alias starts on Today and pushes the destination once.
    val harnessTarget = remember { LaunchHarness.screen?.let(::harnessRoute) }
    val startDestination = harnessTarget?.takeIf { it in tabRoutes } ?: MainTab.Today.route
    LaunchedEffect(harnessTarget) {
        if (harnessTarget != null && harnessTarget !in tabRoutes) {
            navController.navigate(harnessTarget) { launchSingleTop = true }
        }
    }

    // Track current route for tab highlight + bottom bar visibility
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Tap su una push: MainActivity ha parcheggiato il bersaglio in
    // PendingPushTarget perche' quando l'Intent arriva il NavController non
    // esiste ancora. Si drena qui, ed e' il motivo per cui la condizione
    // guarda `navBackStackEntry`: finche' e' null il grafo non e' impostato e
    // navigare lancerebbe. `runCatching` perche' una rotta che non risolve non
    // deve far chiudere l'app a chi ha solo toccato una notifica.
    val pendingPush by PendingPushTarget.target.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LaunchedEffect(pendingPush, navBackStackEntry != null) {
        if (navBackStackEntry == null || pendingPush == null) return@LaunchedEffect
        val target = PendingPushTarget.consume() ?: return@LaunchedEffect
        runCatching {
            DeepLinkHandler.handlePushTarget(
                target = target,
                navController = navController,
                // Non l'`UriHandler` di Compose: su un indirizzo che non risolve
                // lancia, e l'indirizzo qui arriva dal payload della push.
                openUrl = { url -> ExternalLinks.open(context, url) },
            )
        }
    }

    // Link `svc.mensa.it/links/...` aperto dall'esterno: stessa trafila delle push.
    val pendingAppLink by PendingAppLink.link.collectAsStateWithLifecycle()
    LaunchedEffect(pendingAppLink, navBackStackEntry != null) {
        if (navBackStackEntry == null || pendingAppLink == null) return@LaunchedEffect
        val link = PendingAppLink.consume() ?: return@LaunchedEffect
        runCatching { DeepLinkHandler.handleAppLink(link, navController) }
    }

    // Profile uses its own nested NavController, so master only sees route="profile".
    // ProfileNavGraph signals via callback whether it's at its own root.
    var profileAtRoot by remember { mutableStateOf(true) }

    // Show bottom bar only on the 5 root tab destinations AND, when on Profile,
    // only when Profile's nested stack is at its own root (no drill destination).
    val shouldShowBottomBar = currentDestination?.route in tabRoutes &&
        (currentDestination?.route != MainTab.Profile.route || profileAtRoot)

    val activeTabRoute = currentDestination?.hierarchy
        ?.mapNotNull { dest -> tabRoutes.firstOrNull { it == dest.route } }
        ?.firstOrNull()
    // A drill pushed from a tab belongs to that tab: the rail, which stays on
    // screen over it, keeps the tab lit.
    var lastTabRoute by remember { mutableStateOf(MainTab.Today.route) }
    LaunchedEffect(activeTabRoute) { activeTabRoute?.let { lastTabRoute = it } }

    val navItems = MainTab.values().map { tab ->
        MensaNavItem(
            route = tab.route,
            icon = tab.icon,
            label = tr(tab.labelKey, tab.labelFallback),
        )
    }
    val onTabSelect: (MensaNavItem) -> Unit = { navItem ->
        // The tab already lit, over a drill pushed from it: back to its root.
        // (Restoring the tab's saved state would bring the drill back.)
        val backToRoot = activeTabRoute == null && navItem.route == lastTabRoute &&
            navController.popBackStack(navItem.route, inclusive = false)
        if (!backToRoot) {
            navController.navigate(navItem.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Phone: bottom navigation bar, slid away over drill destinations.
    val bottomBar: @Composable () -> Unit = {
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
            MensaNavigationBar(
                items = navItems,
                selectedRoute = activeTabRoute,
                onItemSelect = onTabSelect,
            )
        }
    }

    // A window at least TwoColumnMinWidth wide (iPhone Duo unfolded, iPad on
    // iOS: the system moves the tab bar to the side) swaps the bottom bar for
    // a navigation rail. Unlike the bar the rail takes no height from the
    // content, so it stays over drill destinations too.
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= TwoColumnMinWidth
        Row(Modifier.fillMaxSize()) {
            if (useRail) {
                MensaNavigationRail(
                    items = navItems,
                    selectedRoute = activeTabRoute ?: lastTabRoute,
                    onItemSelect = onTabSelect,
                )
            }
            Scaffold(
                modifier = Modifier.weight(1f),
                bottomBar = { if (!useRail) bottomBar() },
                contentWindowInsets = WindowInsets(0),
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        // La bottom bar copre gia' la navigation bar di sistema quando e'
                        // visibile: senza consumare l'inset, le schermate interne
                        // (MensaScaffold consuma navigationBars) lo pagherebbero due volte.
                        // Sulle destinazioni drill la bar non c'e', l'inset resta intero e
                        // la schermata se lo prende — cosi' il FAB non finisce sotto la
                        // barra di sistema.
                        .consumeWindowInsets(innerPadding)
                        // The rail already pads for the system bars and a cutout on
                        // its side; the screens beside it must not pay it again.
                        .consumeWindowInsets(
                            WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Start),
                        ),
                ) {
                    // ── Master NavHost ────────────────────────────────────────────────
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
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
                                onLeaderboardClick = { navController.navigate(LeaderboardRoutes.BOARD) },
                                onTicketsClick = { navController.navigate(TicketsRoutes.LIST) },
                                onMemberClick = { id -> navController.navigate(MembersRoutes.detail(id)) },
                                onSearchTap = { navController.navigate(SearchRoute.ROUTE) },
                            )
                        }

                        composable(MainTab.Discover.route) {
                            DiscoverTab(
                                onCategoryClick = { category ->
                                    category.toRoute()?.let { navController.navigate(it) }
                                },
                                onSearchTap = { navController.navigate(SearchRoute.ROUTE) },
                            )
                        }

                        composable(SearchRoute.ROUTE) {
                            SearchTab(
                                onItemClick = { hit ->
                                    hit.toDetailRoute()?.let { navController.navigate(it) }
                                },
                                onBack = { navController.popBackStack() },
                            )
                        }

                        composable(MainTab.Profile.route) {
                            ProfileNavGraph(
                                onSearchTap = { navController.navigate(SearchRoute.ROUTE) },
                                onReceiptsClick = { navController.navigate(ReceiptsRoutes.LIST) },
                                onAtRootChange = { profileAtRoot = it },
                            )
                        }

                        // ── Drill nav graphs ──────────────────────────────────────────

                        drillNavGraphs(navController)
                    }

                    // ── MiniAudioPlayer — floats above NavigationBar ──────────────────
                    if (currentTrack != null) {
                        MiniAudioPlayer(
                            controller = audioController,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                // Zero quando la bottom bar e' visibile (inset gia'
                                // consumato sopra), altezza piena della navigation bar
                                // sulle destinazioni drill.
                                .navigationBarsPadding()
                                .padding(bottom = 8.dp, start = 12.dp, end = 12.dp),
                        )
                    }
                }
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

    // ── Account-confirmation modal (identity check / data export) ─────────────
    AccountConfirmationHost()
}

// ─── Navigation helpers ───────────────────────────────────────────────────────

private fun NavController.navigateToTab(tab: MainTab) {
    navigate(tab.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
