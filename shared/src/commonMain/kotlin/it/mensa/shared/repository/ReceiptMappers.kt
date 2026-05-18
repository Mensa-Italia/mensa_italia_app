package it.mensa.shared.repository

import it.mensa.shared.db.Receipt
import it.mensa.shared.model.ReceiptModel
import kotlinx.datetime.Instant

internal fun Receipt.toModel(): ReceiptModel = ReceiptModel(
    id = id,
    description = description,
    user = user,
    stripeCode = stripeCode,
    status = status,
    amount = amount.toInt(),
    created = Instant.fromEpochMilliseconds(createdAt),
    updated = Instant.fromEpochMilliseconds(updatedAt),
)
