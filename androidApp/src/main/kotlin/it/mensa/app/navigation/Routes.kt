package it.mensa.app.navigation

/**
 * Routes — sealed class hierarchy for all navigation destinations.
 *
 * Structure:
 * - [Root] — app entry point (decides Login vs Main flow)
 * - [Auth] — authentication flow (Login, Onboarding)
 * - [Main] — authenticated main flow with bottom nav
 * - [Debug] — dev-only screens (SmokeTest, etc.)
 *
 * Add new destinations as nested objects/data classes as features are built.
 * Add new feature routes here and in [MensaNavGraph].
 */
sealed class Route(val path: String) {

    // ─── Root ─────────────────────────────────────────────────────────────────
    object Bootstrap : Route("bootstrap")

    // ─── Auth flow ────────────────────────────────────────────────────────────
    object Login : Route("auth/login")
    object Onboarding : Route("auth/onboarding")

    // ─── Main flow (bottom nav host) ─────────────────────────────────────────
    object Main : Route("main")

    // ─── Main tabs ────────────────────────────────────────────────────────────
    object Today : Route("main/today")
    object Discover : Route("main/discover")
    object Search : Route("main/search")
    object Card : Route("main/card")
    object Profile : Route("main/profile")

    // ─── Debug (dev only) ────────────────────────────────────────────────────
    object SmokeTest : Route("debug/smoke")

    // ─── Deep links ──────────────────────────────────────────────────────────
    data class EventDetail(val eventId: String) : Route("events/$eventId") {
        companion object {
            const val TEMPLATE = "events/{eventId}"
            const val ARG_EVENT_ID = "eventId"
        }
    }
    data class DealDetail(val dealId: String) : Route("deals/$dealId") {
        companion object {
            const val TEMPLATE = "deals/{dealId}"
            const val ARG_DEAL_ID = "dealId"
        }
    }
    data class TicketDetail(val ticketId: String) : Route("tickets/$ticketId") {
        companion object {
            const val TEMPLATE = "tickets/{ticketId}"
            const val ARG_TICKET_ID = "ticketId"
        }
    }
}

/** Bottom navigation tab definition */
data class BottomNavTab(
    val route: Route,
    val label: String,
    val contentDescription: String,
)

val mensaBottomNavTabs = listOf(
    BottomNavTab(Route.Today, "Today", "Oggi"),
    BottomNavTab(Route.Discover, "Discover", "Scopri"),
    BottomNavTab(Route.Search, "Search", "Cerca"),
    BottomNavTab(Route.Card, "Card", "Tessera"),
    BottomNavTab(Route.Profile, "Profile", "Profilo"),
)
