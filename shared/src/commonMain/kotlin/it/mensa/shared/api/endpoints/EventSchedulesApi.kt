package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.EventScheduleModel

class EventSchedulesApi(private val pb: PocketBaseClient) {

    @Throws(Exception::class)
    suspend fun listForEvent(eventId: String): List<EventScheduleModel> {
        val safe = eventId.replace("'", "")
        return pb.fullList(
            collection = "events_schedule",
            filter = "event = '$safe'",
            sort = "when_start",
        )
    }

    @Throws(Exception::class)
    suspend fun get(id: String): EventScheduleModel =
        pb.getOne("events_schedule", id)

    @Throws(Exception::class)
    suspend fun create(body: EventScheduleModel): EventScheduleModel =
        pb.create("events_schedule", body)

    @Throws(Exception::class)
    suspend fun update(id: String, body: EventScheduleModel): EventScheduleModel =
        pb.update("events_schedule", id, body)

    @Throws(Exception::class)
    suspend fun delete(id: String) =
        pb.delete("events_schedule", id)
}
