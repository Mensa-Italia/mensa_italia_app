package it.mensa.shared.geo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Il filtro "Regione" della lista eventi confronta `positions.state`, che il
 * backend scrive con `GET /api/position/state?lat=&lon=`. Quell'endpoint
 * risponde con i nomi canonici ("Emilia-Romagna", "Valle d'Aosta") e con
 * "NaN" fuori dall'Italia: qui si fissa che li leggiamo come si deve, comprese
 * le grafie vecchie rimaste nei record.
 */
class ItalianRegionsTest {

    @Test
    fun canonicalAcceptsTheBackendSpelling() {
        assertEquals("Emilia-Romagna", ItalianRegions.canonical("Emilia-Romagna"))
        assertEquals("Valle d'Aosta", ItalianRegions.canonical("Valle d'Aosta"))
        assertEquals("Trentino-Alto Adige", ItalianRegions.canonical("Trentino-Alto Adige"))
        assertEquals("Friuli-Venezia Giulia", ItalianRegions.canonical("Friuli-Venezia Giulia"))
    }

    @Test
    fun canonicalIgnoresPunctuationAndCase() {
        assertEquals("Emilia-Romagna", ItalianRegions.canonical("Emilia Romagna"))
        assertEquals("Valle d'Aosta", ItalianRegions.canonical("valle d aosta"))
        assertEquals("Valle d'Aosta", ItalianRegions.canonical("Valle d’Aosta"))
        assertEquals("Trentino-Alto Adige", ItalianRegions.canonical("Trentino Alto Adige"))
        assertEquals("Sicilia", ItalianRegions.canonical("SICILIA"))
    }

    @Test
    fun nonRegionsAreNotRegions() {
        // "NaN" e' cio' che /api/position/state restituisce fuori dall'Italia.
        assertNull(ItalianRegions.canonical("NaN"))
        assertNull(ItalianRegions.canonical(""))
        assertNull(ItalianRegions.canonical(null))
        assertNull(ItalianRegions.canonical("Canton Ticino"))
        // Il bug di partenza: una via intitolata a una regione non e' la regione.
        assertNull(ItalianRegions.canonical("Via Sicilia, 5, 10023 Chieri TO"))
    }

    @Test
    fun matchesIsEmptyFilterFriendly() {
        assertTrue(ItalianRegions.matches("Piemonte", emptySet()))
        assertTrue(ItalianRegions.matches(null, emptySet()))
        assertTrue(ItalianRegions.matches("NaN", emptySet()))
    }

    @Test
    fun matchesComparesRegionsNotText() {
        val selected = setOf("Sicilia", "Toscana", "Lombardia")
        assertTrue(ItalianRegions.matches("Sicilia", selected))
        assertFalse(ItalianRegions.matches("Piemonte", selected))
        // Evento senza regione risolta: fuori dal filtro, non in tutte.
        assertFalse(ItalianRegions.matches(null, selected))
        assertFalse(ItalianRegions.matches("", selected))
        assertFalse(ItalianRegions.matches("NaN", selected))
    }

    @Test
    fun canonicalListIsTheOneTheChipsShow() {
        assertEquals(20, ItalianRegions.all.size)
        assertEquals(ItalianRegions.all.size, ItalianRegions.all.toSet().size)
        ItalianRegions.all.forEach { region ->
            assertEquals(region, ItalianRegions.canonical(region))
        }
    }
}
