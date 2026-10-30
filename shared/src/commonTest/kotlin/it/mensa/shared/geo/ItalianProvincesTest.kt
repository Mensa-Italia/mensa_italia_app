package it.mensa.shared.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ItalianProvincesTest {

    @Test
    fun everyRegionHasItsProvincesAndThereAre107() {
        assertEquals(ItalianRegions.all.toSet(), ItalianProvinces.byRegion.keys)
        val all = ItalianProvinces.byRegion.values.flatten()
        assertEquals(107, all.size)
        assertEquals(all.size, all.map { TextNormalizer.normalize(it) }.toSet().size)
        all.forEach { assertEquals(it, ItalianProvinces.canonical(it)) }
    }

    @Test
    fun registrySpellingsAreProvinces() {
        // Come le scrive `members_registry.state`.
        assertEquals("Reggio nell'Emilia", ItalianProvinces.canonical("Reggio nell'Emilia"))
        assertEquals("Forlì-Cesena", ItalianProvinces.canonical("Forli Cesena"))
        assertEquals("Monza e della Brianza", ItalianProvinces.canonical("Monza e della Brianza"))
        assertEquals("Sud Sardegna", ItalianProvinces.canonical("Sud Sardegna"))
        assertEquals("Bolzano", ItalianProvinces.canonical("Bolzano"))
    }

    @Test
    fun abroadAndEmptyAreNotProvinces() {
        assertNull(ItalianProvinces.canonical("-- STATO ESTERO/FOREIGN STATE--"))
        assertNull(ItalianProvinces.canonical(""))
        assertNull(ItalianProvinces.canonical(null))
    }

    @Test
    fun ofTakesTheRegionAsTheBackendWritesIt() {
        assertEquals(listOf("Bolzano", "Trento"), ItalianProvinces.of("Trentino-Alto Adige/Südtirol"))
        assertTrue(ItalianProvinces.of("NaN").isEmpty())
        assertTrue(ItalianProvinces.of(null).isEmpty())
    }
}
