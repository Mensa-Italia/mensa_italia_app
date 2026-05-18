package it.mensa.app.navigation

import it.mensa.app.features.addonshub.AddonsHubRoutes
import it.mensa.app.features.boutique.BoutiqueRoutes
import it.mensa.app.features.contacts.ContactsRoutes
import it.mensa.app.features.deals.DealsRoute
import it.mensa.app.features.discover.DiscoverCategory
import it.mensa.app.features.documents.DocumentsRoutes
import it.mensa.app.features.events.EventRoutes
import it.mensa.app.features.localoffices.LocalOfficesRoutes
import it.mensa.app.features.members.MembersRoutes
import it.mensa.app.features.notifications.NotificationsRoutes
import it.mensa.app.features.podcasts.PodcastsRoutes
import it.mensa.app.features.quid.QuidRoute
import it.mensa.app.features.sigs.SigsRoutes
import it.mensa.app.features.tableport.TableportRoutes
import it.mensa.app.features.testassistant.TestAssistantRoutes

/**
 * DiscoverCategoryRoutes — maps each [DiscoverCategory] to its navigation route.
 *
 * Returns null for categories that navigate to tabs rather than drill destinations
 * (e.g. Card → navigate to Card tab, handled upstream).
 */
fun DiscoverCategory.toRoute(): String? = when (this) {
    DiscoverCategory.Events        -> EventRoutes.LIST
    DiscoverCategory.Deals         -> DealsRoute.LIST
    DiscoverCategory.Sigs          -> SigsRoutes.LIST
    DiscoverCategory.Members       -> MembersRoutes.DIRECTORY
    DiscoverCategory.LocalOffices  -> LocalOfficesRoutes.LIST
    DiscoverCategory.Documents     -> DocumentsRoutes.LIST
    DiscoverCategory.Notifications -> NotificationsRoutes.LIST
    DiscoverCategory.Tableport     -> TableportRoutes.PASSPORT
    DiscoverCategory.Quid          -> QuidRoute.ISSUES
    DiscoverCategory.Podcasts      -> PodcastsRoutes.LIST
    DiscoverCategory.Boutique      -> BoutiqueRoutes.LIST
    DiscoverCategory.TestAssistant -> TestAssistantRoutes.DASHBOARD
    DiscoverCategory.AddonsHub     -> AddonsHubRoutes.HUB
    DiscoverCategory.Contacts      -> ContactsRoutes.LIST
    // Categories navigated via tab switch (handled at call site)
    DiscoverCategory.Card          -> null  // → navigate to Card tab
    DiscoverCategory.OrgChart      -> null  // → ProfileNavGraph sub-screen (no master route)
    DiscoverCategory.RegSoci       -> MembersRoutes.DIRECTORY
    DiscoverCategory.External      -> null  // requires addonId — resolved at call site
}
