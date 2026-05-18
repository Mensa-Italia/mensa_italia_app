package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DocumentElaboratedModel(
    val id: String = "",
    val document: String = "",
    @SerialName("ia_resume")
    val iaResume: String = "",
)
