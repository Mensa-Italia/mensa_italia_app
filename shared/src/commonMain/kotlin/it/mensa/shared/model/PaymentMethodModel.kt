package it.mensa.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodModel(
    val id: String = "",
    val brand: String = "",
    val display: String = "",
)
