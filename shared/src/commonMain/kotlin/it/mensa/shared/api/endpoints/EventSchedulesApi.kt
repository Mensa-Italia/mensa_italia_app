package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.EventScheduleModel

class EventSchedulesApi(private val pb: PocketBaseClient) {

    suspend fun listForEvent(eventId: String): List<EventScheduleModel> {
        val safe = eventId.replace("'", "")
        return pb.fullList(
            collection = "events_schedule",
            filter = "event = '$safe'",
            sort = "when_start",
        )
    }

    suspend fun get(id: String): EventScheduleModel =
        pb.getOne("events_schedule", id)

    suspend fun create(body: EventScheduleModel): EventScheduleModel =
        pb.create("events_schedule", body)

    suspend fun update(id: String, body: EventScheduleModel): EventScheduleModel =
        pb.update("events_schedule", id, body)

    suspend fun delete(id: String) =
        pb.delete("events_schedule", id)
}
