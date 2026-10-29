package it.mensa.shared.auth

import it.mensa.shared.model.EventModel
import it.mensa.shared.model.UserModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * La `updateRule` di `events`: owner, `super`, admin del gruppo locale.
 * Prima il client mostrava "Modifica" a chiunque avesse `events` o
 * `events_helper`, su qualsiasi evento, e il server rifiutava il salvataggio.
 */
class EventPermissionsTest {

    private val event = EventModel(id = "e1", ownerId = "owner", localOfficeId = "lo1")

    private fun user(id: String, vararg powers: String) = UserModel(id = id, powers = powers.toList())

    @Test
    fun eventsHelperModificaSoloISuoi() {
        assertFalse(EventPermissions.canEdit(user("helper", "events_helper"), event, emptyList()))
        assertTrue(
            EventPermissions.canEdit(user("helper", "events_helper"), event.copy(ownerId = "helper"), emptyList()),
        )
    }

    @Test
    fun eventsDaSoloNonBastaPerGliEventiAltrui() {
        assertFalse(EventPermissions.canEdit(user("segretario", "events"), event, emptyList()))
    }

    @Test
    fun adminDelGruppoLocaleModificaGliEventiDelGruppo() {
        assertTrue(EventPermissions.canEdit(user("segretario", "events"), event, listOf("segretario")))
        assertTrue(EventPermissions.canEdit(user("admin"), event, listOf("admin")))
    }

    @Test
    fun adminDiUnAltroGruppoNo() {
        // Gli admin passati sono quelli del gruppo dell'evento: senza gruppo
        // locale non c'e' nessun admin che valga.
        assertFalse(EventPermissions.canEdit(user("admin"), event.copy(localOfficeId = ""), listOf("admin")))
        assertFalse(EventPermissions.canEdit(user("altro"), event, listOf("admin")))
    }

    @Test
    fun ownerEdSuperPassano() {
        assertTrue(EventPermissions.canEdit(user("owner"), event, emptyList()))
        assertTrue(EventPermissions.canEdit(user("x", "super"), event, emptyList()))
    }

    @Test
    fun senzaUtenteONelloOwnerVuotoNiente() {
        assertFalse(EventPermissions.canEdit(null, event, emptyList()))
        // Utente senza id ed evento senza owner non devono combaciare.
        assertFalse(EventPermissions.canEdit(user(""), event.copy(ownerId = ""), emptyList()))
    }
}
