package it.mensa.shared.members

import it.mensa.shared.geo.ItalianProvinces
import it.mensa.shared.geo.ItalianRegions
import it.mensa.shared.model.RegSociModel

/** Una regione o una provincia dei filtri del registro, con quanti soci ci risiedono. */
data class MemberPlace(val name: String, val count: Int)

/**
 * La ricerca e i filtri per regione e provincia del registro soci, gli stessi
 * su Android e iOS.
 *
 * La regione e' quella che il server scrive in `members_registry.area`, la
 * provincia quella del registro in `members_registry.state`: qui si confronta
 * soltanto, non si deduce l'una dall'altra. Chi risiede all'estero (`NaN`) non
 * sta in nessuna regione, quindi esce solo senza filtro.
 */
object MemberDirectory {

    /** La regione del socio fra quelle di [ItalianRegions.all], null se non ne ha una. */
    fun regionOf(member: RegSociModel): String? = ItalianRegions.canonical(member.area)

    /** La provincia del socio fra quelle di [ItalianProvinces], null se non ne ha una. */
    fun provinceOf(member: RegSociModel): String? = ItalianProvinces.canonical(member.state)

    /**
     * Le opzioni del filtro: tutte le regioni di [ItalianRegions.all], sempre,
     * cosi' il filtro c'e' prima che il registro finisca di caricare. Il
     * numero e' quello dei [members] gia' in mano, 0 finche' non arrivano.
     */
    fun regions(members: List<RegSociModel>): List<MemberPlace> {
        val counts = members.mapNotNull(::regionOf).groupingBy { it }.eachCount()
        return ItalianRegions.all.map { name -> MemberPlace(name, counts[name] ?: 0) }
    }

    /**
     * Le opzioni del sottofiltro: tutte le province di [region] da
     * [ItalianProvinces], sempre, contate come in [regions] sui soci di quella
     * regione. Vuoto senza regione.
     */
    fun provinces(members: List<RegSociModel>, region: String?): List<MemberPlace> {
        val names = ItalianProvinces.of(region)
        if (names.isEmpty()) return emptyList()
        val counts = filter(members, query = "", region = region, province = null)
            .mapNotNull(::provinceOf).groupingBy { it }.eachCount()
        return names.map { name -> MemberPlace(name, counts[name] ?: 0) }
    }

    /**
     * I soci di [region] e [province] (null = nessun filtro) che corrispondono
     * a [query] per nome, citta' o numero di tessera. Le tre condizioni valgono
     * insieme.
     */
    fun filter(
        members: List<RegSociModel>,
        query: String,
        region: String?,
        province: String?,
    ): List<RegSociModel> {
        val q = query.trim().lowercase()
        if (q.isEmpty() && region == null && province == null) return members
        val wantedRegion = region?.let { ItalianRegions.canonical(it) }
        val wantedProvince = province?.let { ItalianProvinces.canonical(it) }
        return members.filter { m ->
            (region == null || (wantedRegion != null && regionOf(m) == wantedRegion)) &&
                (province == null || (wantedProvince != null && provinceOf(m) == wantedProvince)) &&
                (q.isEmpty() ||
                    m.name.lowercase().contains(q) ||
                    m.city.lowercase().contains(q) ||
                    m.id.lowercase().contains(q))
        }
    }
}
