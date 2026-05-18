package it.mensa.shared.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import it.mensa.shared.api.endpoints.EventSchedulesApi
import it.mensa.shared.db.MensaDatabase
import it.mensa.shared.model.EventScheduleModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock

class EventSchedulesRepository(
    private val api: EventSchedulesApi,
    private val db: MensaDatabase,
) {
    fun observeForEvent(eventId: String): Flow<List<EventScheduleModel>> =
        db.eventScheduleQueries.selectByEventId(eventId)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { rows -> rows.map { it.toModel() } }

    suspend fun refresh(eventId: String) {
        val items = api.listForEvent(eventId)
        val now = Clock.System.now().toEpochMilliseconds()
        db.transaction {
            db.eventScheduleQueries.deleteByEventId(eventId)
            items.forEach { s ->
                db.eventScheduleQueries.insertOrReplace(
                    id = s.id.orEmpty().ifEmpty { "tmp-${s.title}-${s.whenStart.toEpochMilliseconds()}" },
                    eventId = s.event ?: eventId,
                    title = s.title,
                    description = s.description,
                    image = s.image,
                    whenStart = s.whenStart.toEpochMilliseconds(),
                    whenEnd = s.whenEnd.toEpochMilliseconds(),
                    maxExternalGuests = s.maxExternalGuests.toLong(),
                    price = s.price,
                    infoLink = s.infoLink,
                    isSubscriptable = if (s.isSubscriptable) 1L else 0L,
                    updatedAt = now,
                )
            }
        }
    }

    suspend fun firstSnapshot(eventId: String): List<EventScheduleModel> =
        observeForEvent(eventId).first()
}
