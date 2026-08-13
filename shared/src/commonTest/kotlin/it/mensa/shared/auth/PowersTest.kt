package it.mensa.shared.auth

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * I casi che erano rotti in produzione: le schermate si scrivevano ognuna la
 * propria lista di permessi e le liste non combaciavano ne' fra loro ne' col
 * server.
 */
class PowersTest {

    @Test
    fun referenteConvenzioniPuoGestireLeConvenzioni() {
        // Su iOS l'elenco era ["super", "admin", "deals_admin"]: chi aveva il
        // power vero non vedeva il bottone, su Android si'.
        assertTrue(Powers.canManageDeals(listOf("deals")))
        assertTrue(Powers.canManageDeals(listOf("super")))
        assertFalse(Powers.canManageDeals(listOf("events")))
    }

    @Test
    fun chiOrganizzaEventiPuoCrearli() {
        // Su iOS il controllo era `super || canAddEvent`, e `canAddEvent` non
        // esiste fra i powers del server: passavano solo i super.
        assertTrue(Powers.canManageEvents(listOf("events")))
        assertTrue(Powers.canManageEvents(listOf("events_helper")))
        assertTrue(Powers.canManageEvents(listOf("super")))
        assertFalse(Powers.canManageEvents(listOf("deals")))
    }

    @Test
    fun eventsHelperValeQuantoEvents() {
        // E' il motivo per cui esiste: `events` lo assegna e lo revoca il
        // server in automatico, `events_helper` no.
        assertEquals(
            Powers.canManageEvents(listOf("events")),
            Powers.canManageEvents(listOf("events_helper")),
        )
    }

    @Test
    fun superPassaSempre() {
        Powers.all.forEach { power ->
            assertTrue(Powers.has(listOf("super"), power), "super deve passare $power")
        }
    }

    @Test
    fun nessunPermessoNessunAccesso() {
        assertFalse(Powers.canManageEvents(emptyList()))
        assertFalse(Powers.canManageDeals(null))
        assertFalse(Powers.canManageSigs(listOf("addons")))
        assertFalse(Powers.canManageTests(listOf("events")))
    }

    @Test
    fun iNomiInventatiNonConcedonoNiente() {
        // "admin", "deals_admin" e "canAddEvent" comparivano nel codice ma il
        // backend non li emette: non devono valere come permessi.
        listOf("admin", "deals_admin", "canAddEvent").forEach { finto ->
            assertFalse(Powers.canManageDeals(listOf(finto)), "$finto non e' un permesso")
            assertFalse(Powers.canManageEvents(listOf(finto)), "$finto non e' un permesso")
        }
    }

    @Test
    fun elencoAllineatoAlServer() {
        assertEquals(
            listOf("super", "sigs", "events", "events_helper", "addons", "deals", "testmakers"),
            Powers.all,
        )
    }
}
