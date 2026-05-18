package it.mensa.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TestelabModel(
    val id: String = "",
    val fullname: String = "",
    @SerialName("type_of_test")
    val typeOfTest: String = "",
    val modality: String = "",
    val status: String = "",
    val state: String = "",
)
