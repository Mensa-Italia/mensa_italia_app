package it.mensa.shared.deals

import it.mensa.shared.medals.RegionKey
import it.mensa.shared.model.DealModel

/** Which region a deal belongs to, and how to filter a list of deals by region. */
object DealRegions {
    /** The region key of the place of [deal] (e.g. "Piemonte"; Valle d'Aosta counts as Piemonte, Basilicata as Puglia), or null when the deal has no place in Italy: it is valid everywhere. */
    fun keyOf(deal: DealModel): String? = RegionKey.of(deal.position?.state)

    /** The region keys that have at least one deal, alphabetical. */
    fun regions(deals: List<DealModel>): List<String> = deals.mapNotNull(::keyOf).distinct().sorted()

    /** With [regionKey] null: [deals] unchanged. Otherwise: first the deals of that region, then the deals valid everywhere (keyOf == null), each group in its original order. Deals of other regions are dropped. */
    fun filter(deals: List<DealModel>, regionKey: String?): List<DealModel> {
        if (regionKey == null) return deals
        val keys = deals.map(::keyOf)
        val local = deals.filterIndexed { i, _ -> keys[i] == regionKey }
        val everywhere = deals.filterIndexed { i, _ -> keys[i] == null }
        return local + everywhere
    }
}
