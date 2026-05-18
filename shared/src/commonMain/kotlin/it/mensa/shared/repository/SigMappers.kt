package it.mensa.shared.repository

import it.mensa.shared.db.Sig
import it.mensa.shared.model.SigModel

internal fun Sig.toModel(): SigModel = SigModel(
    id = id,
    name = name,
    description = description,
    image = image,
    link = link,
    groupType = groupType,
)
