package it.mensa.shared.auth

import it.mensa.shared.model.EventModel
import it.mensa.shared.model.UserModel

/**
 * Chi puo' modificare (ed eliminare) un evento. E' la `updateRule` della
 * collection `events`, identica alla `deleteRule`:
 *
 *     (@request.auth.id = owner)
 *     || (@request.auth.powers:each ?= "super")
 *     || (@request.auth.id ?= local_office.local_offices_admins_via_local_office.user.id)
 *
 * `events` ed `events_helper` non ci sono: servono a creare eventi senza il
 * limite dei due attivi, non a modificare quelli degli altri. Prima il client
 * mostrava "Modifica" a chiunque avesse uno dei due, e il server rifiutava il
 * salvataggio.
 *
 * I segretari (che hanno `events`) modificano gli eventi del proprio gruppo
 * locale in quanto admin di quel gruppo, non per il power.
 */
object EventPermissions {

    /**
     * [officeAdminIds] sono gli utenti admin del gruppo locale dell'evento
     * (`local_offices_admins.user`, che e' lo stesso id di `users`). Vuota
     * se l'evento non ha gruppo locale o non e' ancora stata caricata.
     */
    fun canEdit(user: UserModel?, event: EventModel, officeAdminIds: List<String>): Boolean {
        val userId = user?.id?.takeIf { it.isNotEmpty() } ?: return false
        if (user.powers.contains(Powers.SUPER)) return true
        if (userId == event.ownerId) return true
        return event.localOfficeId.isNotEmpty() && officeAdminIds.contains(userId)
    }
}
