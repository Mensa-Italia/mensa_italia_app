package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.EventModel
import kotlinx.datetime.Clock

class EventsApi(internal val pb: PocketBaseClient) {

    /**
     * List all events. Flutter default: sort=when_end, filter=when_end>=now, expand=position,owner
     * For simplicity we allow callers to override filter/sort; the default sort follows the Flutter app.
     */
    suspend fun list(
        filter: String? = null,
        sort: String = "when_end"
    ): List<EventModel> =
        pb.fullList("events", filter = filter, sort = sort, expand = "position,owner")

    suspend fun get(id: String): EventModel =
        pb.getOne("events", id, expand = "position,owner")

    suspend fun create(body: EventModel): EventModel =
        pb.create("events", body)

    suspend fun update(id: String, body: EventModel): EventModel =
        pb.update("events", id, body)

    suspend fun delete(id: String) =
        pb.delete("events", id)

    suspend fun listPublic(): List<EventModel> {
        val nowIso = Clock.System.now().toString()
        return pb.fullListUnauthenticated(
            "events",
            filter = "is_public=true && when_end>=\"$nowIso\"",
            sort = "when_end",
            expand = "position,owner"
        )
    }
}
