package it.mensa.shared.repository

import it.mensa.shared.db.Ticket
import it.mensa.shared.model.TicketModel
import kotlinx.datetime.Instant

internal fun Ticket.toModel(): TicketModel = TicketModel(
    id = id,
    name = name,
    description = description,
    userId = userId,
    link = link,
    qr = qr,
    internalRefId = internalRefId,
    customerData = customerData,
    deadline = deadline?.let { Instant.fromEpochMilliseconds(it) },
    created = Instant.fromEpochMilliseconds(createdAt),
    updated = Instant.fromEpochMilliseconds(updatedAt),
)
