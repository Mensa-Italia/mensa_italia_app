package it.mensa.shared.members

import it.mensa.shared.geo.ItalianProvinces
import it.mensa.shared.geo.ItalianRegions
import it.mensa.shared.model.RegSociModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemberDirectoryTest {
    private val members = listOf(
        RegSociModel(id = "1", name = "Anna Rossi", city = "Torino", state = "Torino", area = "Piemonte"),
        RegSociModel(id = "2", name = "Bruno Neri", city = "Milano", state = "Milano", area = "Lombardia"),
        RegSociModel(id = "3", name = "Carla Bianchi", city = "Bolzano", state = "Bolzano", area = "Trentino-Alto Adige/Südtirol"),
        RegSociModel(id = "4", name = "Dario Rossi", city = "Lugano", state = "", area = "NaN"),
        RegSociModel(id = "5", name = "Elena Rossi", city = "Novara", state = "Novara", area = "Piemonte"),
        // Riga in cache da prima della migrazione: niente regione finche' non si aggiorna.
        RegSociModel(id = "6", name = "Franco Verdi", city = "Asti", state = "Asti", area = ""),
        RegSociModel(id = "7", name = "Gino Rossi", city = "Ivrea", state = "Torino", area = "Piemonte"),
    )

    private fun ids(list: List<RegSociModel>) = list.map { it.id }

    @Test
    fun noQueryAndNoRegionKeepsEveryone() {
        assertEquals(members, MemberDirectory.filter(members, "  ", null, null))
    }

    @Test
    fun regionAloneKeepsOnlyItsMembers() {
        assertEquals(listOf("1", "5", "7"), ids(MemberDirectory.filter(members, "", "Piemonte", null)))
    }

    @Test
    fun queryAndRegionApplyTogether() {
        assertEquals(listOf("1", "5", "7"), ids(MemberDirectory.filter(members, "rossi", "Piemonte", null)))
        assertEquals(listOf("5"), ids(MemberDirectory.filter(members, "novara", "Piemonte", null)))
        assertEquals(emptyList(), ids(MemberDirectory.filter(members, "bruno", "Piemonte", null)))
        // Senza regione la stessa ricerca trova anche chi sta altrove o all'estero.
        assertEquals(listOf("1", "4", "5", "7"), ids(MemberDirectory.filter(members, "rossi", null, null)))
    }

    @Test
    fun officialBilingualNameMatchesTheCanonicalRegion() {
        assertEquals("Trentino-Alto Adige", MemberDirectory.regionOf(members[2]))
        assertEquals(listOf("3"), ids(MemberDirectory.filter(members, "", "Trentino-Alto Adige", null)))
    }

    @Test
    fun abroadAndUnknownBelongToNoRegion() {
        ItalianRegions.all.forEach { region ->
            val found = ids(MemberDirectory.filter(members, "", region, null))
            assertTrue("4" !in found && "6" !in found, "$region: $found")
        }
    }

    @Test
    fun regionsAreAlwaysAllTwentyWithTheirCounts() {
        val regions = MemberDirectory.regions(members)
        assertEquals(ItalianRegions.all, regions.map { it.name })
        assertEquals(3, regions.first { it.name == "Piemonte" }.count)
        assertEquals(1, regions.first { it.name == "Trentino-Alto Adige" }.count)
        assertEquals(0, regions.first { it.name == "Sicilia" }.count)
    }

    @Test
    fun regionsAreThereBeforeTheRegistryLoads() {
        val regions = MemberDirectory.regions(emptyList())
        assertEquals(ItalianRegions.all, regions.map { it.name })
        assertTrue(regions.all { it.count == 0 })
    }

    @Test
    fun provinceNarrowsTheRegionAndWorksWithTheSearch() {
        assertEquals(listOf("1", "7"), ids(MemberDirectory.filter(members, "", "Piemonte", "Torino")))
        assertEquals(listOf("7"), ids(MemberDirectory.filter(members, "ivrea", "Piemonte", "Torino")))
        assertEquals(listOf("5"), ids(MemberDirectory.filter(members, "rossi", "Piemonte", "Novara")))
        assertEquals(emptyList(), ids(MemberDirectory.filter(members, "", "Piemonte", "Cuneo")))
    }

    @Test
    fun provincesAreTheRegionsWholeListWithTheirCounts() {
        val piemonte = MemberDirectory.provinces(members, "Piemonte")
        assertEquals(ItalianProvinces.byRegion.getValue("Piemonte"), piemonte.map { it.name })
        assertEquals(2, piemonte.first { it.name == "Torino" }.count)
        assertEquals(1, piemonte.first { it.name == "Novara" }.count)
        // Asti c'e' anche se l'unico socio di Asti non ha ancora la regione.
        assertEquals(0, piemonte.first { it.name == "Asti" }.count)
        // Prima che il registro carichi le province ci sono gia'.
        assertEquals(piemonte.map { it.name }, MemberDirectory.provinces(emptyList(), "Piemonte").map { it.name })
        assertEquals(emptyList(), MemberDirectory.provinces(members, null))
        // Anche col nome bilingue della regione.
        assertEquals(listOf("Bolzano", "Trento"), MemberDirectory.provinces(members, "Trentino-Alto Adige/Südtirol").map { it.name })
    }
}
