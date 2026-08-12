package it.mensa.shared.repository

import it.mensa.shared.db.Event
import it.mensa.shared.model.EventExpand
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocationModel
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

internal fun Event.toModel(json: Json): EventModel {
    val location = positionJson?.let {
        runCatching { json.decodeFromString(LocationModel.serializer(), it) }.getOrNull()
    }
    return EventModel(
        id = id,
        name = name,
        description = description,
        image = image,
        infoLink = infoLink,
        contact = contact,
        whenStart = Instant.fromEpochMilliseconds(whenStart),
        whenEnd = Instant.fromEpochMilliseconds(whenEnd),
        ownerId = owner,
        isNational = isNational != 0L,
        isSpot = isSpot != 0L,
        isPublic = isPublic != 0L,
        bookingLink = bookingLink ?: "",
        positionId = location?.id,
        expand = location?.let { EventExpand(position = it) },
    )
}

/**
 * Copia dell'evento con la regione scritta sulla posizione. La usa
 * [EventsRepository] dopo aver richiesto `/api/position/state` per i record che
 * il backend ha lasciato senza regione leggibile.
 */
internal fun EventModel.withPositionState(state: String): EventModel {
    val position = position ?: return this
    if (position.state == state) return this
    return copy(expand = (expand ?: EventExpand()).copy(position = position.copy(state = state)))
}
