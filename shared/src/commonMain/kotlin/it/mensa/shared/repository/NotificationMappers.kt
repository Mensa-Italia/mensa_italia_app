package it.mensa.shared.repository

import it.mensa.shared.db.Notification
import it.mensa.shared.model.NotificationModel
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal fun Notification.toModel(json: Json): NotificationModel = NotificationModel(
    id = id,
    tr = tr,
    trNamedParams = trNamedParamsJson.let {
        runCatching {
            @Suppress("UNCHECKED_CAST")
            json.decodeFromString<Map<String, String>>(it)
        }.getOrDefault(emptyMap())
    },
    data = dataJson?.let {
        runCatching { json.decodeFromString(JsonObject.serializer(), it) }.getOrNull()
    },
    seen = seen?.let { Instant.fromEpochMilliseconds(it) },
    created = Instant.fromEpochMilliseconds(createdAt),
    updated = Instant.fromEpochMilliseconds(updatedAt),
)
