package it.mensa.shared.repository

import it.mensa.shared.db.RegSoci
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.model.withDefaultAvatarStripped
import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

internal fun RegSoci.toModel(json: Json): RegSociModel {
    val data: JsonObject = runCatching {
        json.parseToJsonElement(fullDataJson) as? JsonObject ?: JsonObject(emptyMap())
    }.getOrDefault(JsonObject(emptyMap()))
    return RegSociModel(
        id = id,
        image = image,
        name = name,
        city = city,
        birthdate = birthdate?.let { Instant.fromEpochMilliseconds(it) },
        state = state,
        fullData = data,
        fullProfileLink = fullProfileLink,
        dataHash = dataHash,
        imageHash = imageHash,
    ).withDefaultAvatarStripped()
}
