package it.mensa.app.features.events

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navArgument

/**
 * EventsNavGraph — NavGraphBuilder extension for the Events feature.
 *
 * Registers all Events destinations into the host NavHost.
 * Called from the master NavHost builder.
 *
 * Routes:
 *   - events/list          → EventListScreen
 *   - events/detail/{eventId} → EventDetailScreen
 *   - events/add           → AddEventScreen (create)
 *   - events/edit/{eventId} → AddEventScreen (edit)
 *   - events/calendar      → EventCalendarScreen
 *   - events/map           → EventMapScreen
 *
 * Sheets (EventFiltersSheet, ScheduleEditorSheet, etc.) are opened
 * inline as composable state — NOT as navigation destinations.
 */
fun NavGraphBuilder.eventsNavGraph(navController: NavController) {

    composable(route = EventRoutes.LIST) {
        EventListScreen(
            onEventClick = { eventId -> navController.navigate(EventRoutes.detail(eventId)) },
            onCalendarClick = { navController.navigate(EventRoutes.CALENDAR) },
            onMapClick = { navController.navigate(EventRoutes.MAP) },
            onAddClick = { navController.navigate(EventRoutes.ADD) },
            onBack = { navController.popBackStack() },
        )
    }

    composable(
        route = EventRoutes.DETAIL,
        arguments = listOf(navArgument(EventRoutes.ARG_EVENT_ID) { type = NavType.StringType }),
    ) { backStackEntry ->
        val eventId = backStackEntry.arguments?.getString(EventRoutes.ARG_EVENT_ID) ?: return@composable
        EventDetailScreen(
            eventId = eventId,
            onBack = { navController.popBackStack() },
            onEditClick = { id -> navController.navigate(EventRoutes.edit(id)) },
        )
    }

    composable(route = EventRoutes.ADD) {
        AddEventScreen(
            eventId = null,
            onDismiss = { navController.popBackStack() },
        )
    }

    composable(
        route = EventRoutes.EDIT,
        arguments = listOf(navArgument(EventRoutes.ARG_EVENT_ID) { type = NavType.StringType }),
    ) { backStackEntry ->
        val eventId = backStackEntry.arguments?.getString(EventRoutes.ARG_EVENT_ID) ?: return@composable
        AddEventScreen(
            eventId = eventId,
            onDismiss = { navController.popBackStack() },
        )
    }

    composable(route = EventRoutes.CALENDAR) {
        EventCalendarScreen(
            onBack = { navController.popBackStack() },
            onEventClick = { eventId -> navController.navigate(EventRoutes.detail(eventId)) },
        )
    }

    composable(route = EventRoutes.MAP) {
        EventMapScreen(
            onBack = { navController.popBackStack() },
            onEventClick = { eventId -> navController.navigate(EventRoutes.detail(eventId)) },
        )
    }
}

/**
 * EventRoutes — route constants for the Events feature.
 * Companion to EventsNavGraph.
 */
object EventRoutes {
    const val LIST = "events/list"
    const val DETAIL = "events/detail/{eventId}"
    const val ADD = "events/add"
    const val EDIT = "events/edit/{eventId}"
    const val CALENDAR = "events/calendar"
    const val MAP = "events/map"

    const val ARG_EVENT_ID = "eventId"

    fun detail(eventId: String) = "events/detail/$eventId"
    fun edit(eventId: String) = "events/edit/$eventId"
}
