package it.mensa.app.features.discover

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalOffer
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.Store
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * DiscoverCategory — Mensa Discover grid items.
 */
enum class DiscoverCategory(
    val labelKey: String,
    val labelFallback: String,
    val icon: ImageVector,
) {
    // ── Sezione 1: Vita Mensa ────────────────────────────────────────────────
    Events(
        labelKey = "discover.category.events",
        labelFallback = "Eventi",
        icon = Icons.Outlined.CalendarMonth,
    ),
    Deals(
        labelKey = "discover.category.deals",
        labelFallback = "Offerte",
        icon = Icons.Outlined.LocalOffer,
    ),
    Sigs(
        labelKey = "discover.category.sigs",
        labelFallback = "SIG e gruppi",
        icon = Icons.Outlined.Groups,
    ),
    Members(
        labelKey = "discover.category.members",
        labelFallback = "Soci",
        icon = Icons.Outlined.People,
    ),
    LocalOffices(
        labelKey = "discover.category.local_offices",
        labelFallback = "Sedi locali",
        icon = Icons.Outlined.LocationCity,
    ),

    // ── Sezione 2: Conoscere ─────────────────────────────────────────────────
    Documents(
        labelKey = "discover.category.documents",
        labelFallback = "Documenti",
        icon = Icons.Outlined.Description,
    ),
    Quid(
        labelKey = "discover.category.quid",
        labelFallback = "Quid magazine",
        icon = Icons.Outlined.MenuBook,
    ),
    Podcasts(
        labelKey = "discover.category.podcasts",
        labelFallback = "Podcast",
        icon = Icons.Outlined.Podcasts,
    ),
    OrgChart(
        labelKey = "discover.category.orgchart",
        labelFallback = "Organigramma",
        icon = Icons.Outlined.AccountTree,
    ),

    // ── Sezione 3: Strumenti ─────────────────────────────────────────────────
    Tableport(
        labelKey = "discover.category.tableport",
        labelFallback = "Tableport",
        icon = Icons.Outlined.QrCodeScanner,
    ),
    Boutique(
        labelKey = "discover.category.boutique",
        labelFallback = "Boutique",
        icon = Icons.Outlined.Store,
    ),
    Contacts(
        labelKey = "discover.category.contacts",
        labelFallback = "Contatti",
        icon = Icons.Outlined.Contacts,
    ),
    AddonsHub(
        labelKey = "discover.category.addons_hub",
        labelFallback = "Tutti gli addon",
        icon = Icons.Outlined.SpaceDashboard,
    ),
    TestAssistant(
        labelKey = "discover.category.test_assistant",
        labelFallback = "Test Assistant",
        icon = Icons.Outlined.Quiz,
    ),

    // ── Extras (navigation targets, non mostrati nella griglia Discover) ─────
    Notifications(
        labelKey = "notifications.title",
        labelFallback = "Notifiche",
        icon = Icons.Outlined.Notifications,
    ),
    RegSoci(
        labelKey = "reg_soci.title",
        labelFallback = "Reg. Soci",
        icon = Icons.Outlined.Person,
    ),
    Card(
        labelKey = "app.tab.card",
        labelFallback = "Tessera",
        icon = Icons.Outlined.Badge,
    ),
    External(
        labelKey = "app.discover.external",
        labelFallback = "Esterno",
        icon = Icons.Outlined.OpenInBrowser,
    ),
}

/** Sezione risolta nel ViewModel con kicker per M3 Expressive */
data class DiscoverSection(
    val titleKey: String,
    val titleFallback: String,
    val kickerKey: String,
    val kickerFallback: String,
    val categories: List<DiscoverCategory>,
)

/** Template statico delle sezioni — il ViewModel inietta TestAssistant dinamicamente */
val discoverSections: List<DiscoverSection> = listOf(
    DiscoverSection(
        titleKey = "discover.section.community",
        titleFallback = "Comunità",
        kickerKey = "discover.kicker.community",
        kickerFallback = "COMUNITÀ",
        categories = listOf(
            DiscoverCategory.LocalOffices,
            DiscoverCategory.Events,
            DiscoverCategory.Documents,
            DiscoverCategory.Members,
            DiscoverCategory.Sigs,
            DiscoverCategory.Deals,
        ),
    ),
    DiscoverSection(
        titleKey = "discover.section.addons",
        titleFallback = "Addon",
        kickerKey = "discover.kicker.addons",
        kickerFallback = "ADDON",
        categories = listOf(
            DiscoverCategory.Tableport,
            DiscoverCategory.Quid,
            DiscoverCategory.Podcasts,
            DiscoverCategory.Boutique,
            DiscoverCategory.AddonsHub,
            // TestAssistant iniettato dinamicamente dal ViewModel se user ha power "testmakers"
        ),
    ),
)
