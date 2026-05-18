package it.mensa.shared.repository

import it.mensa.shared.db.Stamp
import it.mensa.shared.model.StampModel
import kotlinx.datetime.Instant

internal fun Stamp.toModel(): StampModel = StampModel(
    id = id,
    description = description,
    image = image,
    created = Instant.fromEpochMilliseconds(createdAt),
    updated = Instant.fromEpochMilliseconds(updatedAt),
)
