package it.mensa.shared.repository

import it.mensa.shared.db.EventSchedule
import it.mensa.shared.model.EventScheduleModel
import kotlinx.datetime.Instant

internal fun EventSchedule.toModel(): EventScheduleModel = EventScheduleModel(
    id = id,
    title = title,
    event = eventId,
    description = description,
    image = image,
    whenStart = Instant.fromEpochMilliseconds(whenStart),
    whenEnd = Instant.fromEpochMilliseconds(whenEnd),
    maxExternalGuests = maxExternalGuests.toInt(),
    price = price,
    infoLink = infoLink,
    isSubscriptable = isSubscriptable != 0L,
)
