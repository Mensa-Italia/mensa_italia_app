package it.mensa.shared.repository

import it.mensa.shared.db.Location
import it.mensa.shared.model.LocationModel

internal fun Location.toModel(): LocationModel = LocationModel(
    id = id,
    name = name,
    lat = lat,
    lon = lon,
    address = address,
    state = state,
)
