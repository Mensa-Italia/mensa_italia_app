package it.mensa.shared.repository

import it.mensa.shared.db.Deal
import it.mensa.shared.model.DealExpand
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.LocationModel
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json

internal fun Deal.toModel(json: Json): DealModel {
    val location = positionJson?.let {
        runCatching { json.decodeFromString(LocationModel.serializer(), it) }.getOrNull()
    }
    return DealModel(
        id = id,
        name = name,
        commercialSector = commercialSector,
        positionId = location?.id,
        expand = location?.let { DealExpand(position = it) },
        isLocal = isLocal != 0L,
    details = details,
    who = who,
    starting = starting?.let { Instant.fromEpochMilliseconds(it) },
    ending = ending?.let { Instant.fromEpochMilliseconds(it) },
    howToGet = howToGet,
    link = link,
    owner = owner,
    attachment = attachment,
    isActive = isActive != 0L,
    vatNumber = vatNumber,
        created = Instant.fromEpochMilliseconds(updatedAt),
        updated = Instant.fromEpochMilliseconds(updatedAt),
    )
}
