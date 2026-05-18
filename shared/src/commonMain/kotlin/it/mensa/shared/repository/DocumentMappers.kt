package it.mensa.shared.repository

import it.mensa.shared.db.Document
import it.mensa.shared.db.DocumentElaborated
import it.mensa.shared.model.DocumentElaboratedModel
import it.mensa.shared.model.DocumentModel
import kotlinx.datetime.Instant

internal fun Document.toModel(): DocumentModel = DocumentModel(
    id = id,
    name = name,
    description = description,
    file = file_,
    uploadedBy = uploadedBy,
    category = category,
    elaborated = elaborated,
    created = Instant.fromEpochMilliseconds(created),
)

internal fun DocumentElaborated.toModel(): DocumentElaboratedModel = DocumentElaboratedModel(
    id = id,
    document = document,
    iaResume = iaResume,
)
