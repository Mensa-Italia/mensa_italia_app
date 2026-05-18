package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.CalendarLinkModel
import kotlinx.serialization.Serializable

class CalendarLinksApi(private val pb: PocketBaseClient) {

    @Serializable
    data class UpdateStateBody(val state: List<String>)

    suspend fun list(): List<CalendarLinkModel> =
        pb.fullList("calendar_link")

    suspend fun get(id: String): CalendarLinkModel =
        pb.getOne("calendar_link", id)

    /** Returns the single calendar_link record for the current user, if any. */
    suspend fun current(): CalendarLinkModel? =
        pb.list<CalendarLinkModel>("calendar_link", perPage = 1).items.firstOrNull()

    /** PATCH /api/collections/calendar_link/records/{id} with `{ "state": [...] }` */
    suspend fun updateState(id: String, state: List<String>): CalendarLinkModel =
        pb.update("calendar_link", id, UpdateStateBody(state))
}
