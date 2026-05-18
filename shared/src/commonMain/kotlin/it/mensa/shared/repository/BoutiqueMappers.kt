package it.mensa.shared.repository

import it.mensa.shared.db.Boutique
import it.mensa.shared.model.BoutiqueModel
import kotlinx.datetime.Instant
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

internal fun Boutique.toModel(json: Json): BoutiqueModel {
    val images: List<String> = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), imagesJson)
    }.getOrDefault(emptyList())
    return BoutiqueModel(
        id = id,
        uid = uid,
        name = name,
        description = description,
        image = images,
        amount = amount.toInt(),
        alternativeOf = alternativeOf,
        created = Instant.fromEpochMilliseconds(createdAt),
        updated = Instant.fromEpochMilliseconds(updatedAt),
    )
}
