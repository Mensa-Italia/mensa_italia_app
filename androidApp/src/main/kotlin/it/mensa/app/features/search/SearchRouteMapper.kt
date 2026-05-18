package it.mensa.app.features.search

import it.mensa.app.features.boutique.BoutiqueRoutes
import it.mensa.app.features.deals.DealsRoute
import it.mensa.app.features.documents.DocumentsRoutes
import it.mensa.app.features.events.EventRoutes
import it.mensa.app.features.localoffices.LocalOfficesRoutes
import it.mensa.app.features.members.MembersRoutes
import it.mensa.app.features.sigs.SigsRoutes

/**
 * SearchRouteMapper — maps a [HydratedHit] to the appropriate detail route.
 *
 * Returns null when no detail screen exists for the given payload type
 * (e.g. lean hits, org hits without a dedicated screen, addons).
 */
fun HydratedHit.toDetailRoute(): String? = when (val p = payload) {
    is HydratedHit.Payload.User     -> MembersRoutes.detail(id)
    is HydratedHit.Payload.Event    -> EventRoutes.detail(p.event.id)
    is HydratedHit.Payload.Deal     -> DealsRoute.detail(p.deal.id)
    is HydratedHit.Payload.Sig      -> SigsRoutes.detail(p.sig.id)
    is HydratedHit.Payload.Document -> DocumentsRoutes.detail(p.document.id)
    is HydratedHit.Payload.Boutique -> BoutiqueRoutes.product(p.product.id)
    is HydratedHit.Payload.OrgGroup -> null  // no dedicated screen
    is HydratedHit.Payload.OrgRole  -> {
        // Navigate to the member detail using userId
        val userId = p.member.userId.takeIf { it.isNotEmpty() } ?: return null
        MembersRoutes.detail(userId)
    }
    is HydratedHit.Payload.Addon    -> null  // addon detail varies per addon kind
    is HydratedHit.Payload.Lean     -> {
        // Try best-effort route based on id prefix convention
        when {
            id.startsWith("local_offices/") -> {
                val officeId = id.removePrefix("local_offices/")
                LocalOfficesRoutes.detail(officeId)
            }
            else -> null
        }
    }
}
