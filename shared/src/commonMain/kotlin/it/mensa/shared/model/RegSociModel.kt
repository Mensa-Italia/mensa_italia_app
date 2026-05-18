@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.UseSerializers

@Serializable
data class RegSociModel(
    val id: String = "",
    val image: String = "",
    val name: String = "",
    val city: String = "",
    val birthdate: Instant? = null,
    val state: String = "",
    @SerialName("full_data")
    val fullData: JsonObject = JsonObject(emptyMap()),
    @SerialName("full_profile_link")
    val fullProfileLink: String? = null,
    /**
     * Hash dei dati visualizzabili. Quando il valore cambia, l'iOS Spotlight
     * indexer deve ricostruire l'attributeSet (titolo/sottotitolo/keywords).
     * Nullable: i record legacy non lo hanno → forza re-index.
     */
    @SerialName("data_hash")
    val dataHash: String? = null,
    /**
     * Hash dell'immagine. Quando cambia, l'iOS Spotlight indexer deve
     * ri-scaricare la foto profilo; altrimenti riusa la thumbnail su disco.
     */
    @SerialName("image_hash")
    val imageHash: String? = null,
    @SerialName("alias_mail")
    val aliasMail: String? = null,
)

/**
 * Hash dell'immagine "Uomo-1" di default usata storicamente in PocketBase per
 * i membri senza foto reale. Quando il backend restituisce questo hash (o
 * `null`) significa che non c'è una foto profilo: forziamo `image=""` così
 * che tutte le piattaforme (iOS / Android / web) cadano sul fallback iniziali
 * (lettera nome + lettera cognome) senza dover replicare la regola altrove.
 */
private const val DEFAULT_AVATAR_HASH =
    "cc1ca075592fac4fd4bea0ce4f9dfd83ca0734db1d55e12dae7f531b33c2734a"

fun RegSociModel.withDefaultAvatarStripped(): RegSociModel =
    if (imageHash == null || imageHash == DEFAULT_AVATAR_HASH)
        copy(image = "", imageHash = null)
    else this
