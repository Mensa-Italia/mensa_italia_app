package it.mensa.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import it.mensa.app.features.addonshub.addonsHubNavGraph
import it.mensa.app.features.contacts.ContactsRoutes
import it.mensa.app.features.contacts.contactsNavGraph
import it.mensa.app.features.documents.DocumentsRoutes
import it.mensa.app.features.documents.documentsNavGraph
import it.mensa.app.features.events.eventsNavGraph
import it.mensa.app.features.external.ExternalRoutes
import it.mensa.app.features.external.externalNavGraph
import it.mensa.app.features.leaderboard.leaderboardNavGraph
import it.mensa.app.features.localoffices.localOfficesNavGraph
import it.mensa.app.features.locations.locationsNavGraph
import it.mensa.app.features.members.membersNavGraph
import it.mensa.app.features.notifications.notificationsNavGraph
import it.mensa.app.features.podcasts.PodcastsRoutes
import it.mensa.app.features.podcasts.podcastsNavGraph
import it.mensa.app.features.quid.QuidRoute
import it.mensa.app.features.quid.quidNavGraph
import it.mensa.app.features.receipts.receiptsNavGraph
import it.mensa.app.features.sigs.sigsNavGraph
import it.mensa.app.features.tableport.TableportRoutes
import it.mensa.app.features.tableport.tableportNavGraph
import it.mensa.app.features.testassistant.TestAssistantRoutes
import it.mensa.app.features.testassistant.testAssistantNavGraph
import it.mensa.app.features.tickets.ticketsNavGraph
import it.mensa.app.features.deals.dealsNavGraph

/**
 * Registers every drill destination (the feature nav graphs) into the host
 * `NavHost` of [navController].
 *
 * Two hosts use it: the shell's master `NavHost`, and the detail pane of the
 * wide Discover / Profile / Search layouts (`IndexDetailPane`), which runs its
 * own `NavController` so the screens opened beside an index are exactly the
 * ones a phone pushes, with a back stack of their own.
 */
fun NavGraphBuilder.drillNavGraphs(navController: NavController) {
    eventsNavGraph(navController)
    locationsNavGraph(navController)
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
    leaderboardNavGraph(navController)
    testAssistantNavGraph(navController)
    addonsHubNavGraph(
        navController = navController,
        onAddonClick = { addonId -> navController.navigateAddon(addonId) },
    )
}

/**
 * Maps AddonsHub [addonId] strings to the correct drill destination.
 * Convention: addonId equals the feature slug returned by the backend AddonModel.id.
 */
private fun NavController.navigateAddon(addonId: String) {
    val route = when (addonId.lowercase()) {
        "tableport", "stamp"        -> TableportRoutes.PASSPORT
        "podcasts", "podcast"       -> PodcastsRoutes.LIST
        "contacts"                  -> ContactsRoutes.LIST
        "quid"                      -> QuidRoute.ISSUES
        "documents", "docs"         -> DocumentsRoutes.LIST
        "testassistant", "test"     -> TestAssistantRoutes.DASHBOARD
        else -> ExternalRoutes.addon(addonId)  // fallback: open as external addon
    }
    navigate(route)
}
