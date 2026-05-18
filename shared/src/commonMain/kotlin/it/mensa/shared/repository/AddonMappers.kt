package it.mensa.shared.repository

import it.mensa.shared.db.Addon
import it.mensa.shared.model.AddonModel

internal fun Addon.toModel(): AddonModel = AddonModel(
    id = id,
    name = name,
    description = description,
    icon = icon,
    version = version,
    url = url,
    requiredPower = requiredPower.toInt(),
)
