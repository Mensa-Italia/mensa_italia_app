package it.mensa.shared.repository

import it.mensa.shared.db.Calendar
import it.mensa.shared.model.CalendarLinkModel
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal fun Calendar.toModel(json: Json): CalendarLinkModel {
    val state: List<String> = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), stateJson)
    }.getOrDefault(emptyList())
    return CalendarLinkModel(
        id = id,
        user = user,
        hash = hash,
        state = state,
    )
}
