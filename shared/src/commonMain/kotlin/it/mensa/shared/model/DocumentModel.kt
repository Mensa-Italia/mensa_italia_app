@file:UseSerializers(PbInstantSerializer::class)

package it.mensa.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

@Serializable
data class DocumentModel(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val file: String = "",
    @SerialName("uploaded_by")
    val uploadedBy: String = "",
    val category: String = "",
    val elaborated: String = "",
    val created: Instant = Instant.fromEpochMilliseconds(0),
)
