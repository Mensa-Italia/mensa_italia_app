package it.mensa.shared.repository

import it.mensa.shared.db.Device
import it.mensa.shared.model.DeviceModel
import kotlinx.datetime.Instant

internal fun Device.toModel(): DeviceModel = DeviceModel(
    id = id,
    user = user,
    deviceName = deviceName,
    firebaseId = firebaseId,
    created = Instant.fromEpochMilliseconds(createdAt),
    updated = Instant.fromEpochMilliseconds(updatedAt),
)
