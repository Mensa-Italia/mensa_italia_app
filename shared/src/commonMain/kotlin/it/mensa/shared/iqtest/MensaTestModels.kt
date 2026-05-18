package it.mensa.shared.iqtest

import kotlinx.serialization.Serializable

@Serializable
data class MensaTestQuestion(
    val id: Long = 0L,
    val imageUrl: String = "",
    val options: List<String> = emptyList(),
)

@Serializable
data class MensaTestPayload(
    val token: Long = 0L,
    val totalQuestions: Int = 35,
    val durationSeconds: Int = 1500,
    val questions: List<MensaTestQuestion> = emptyList(),
)

@Serializable
data class MensaTestResult(
    val success: Boolean = false,
    val iq: Int? = null,
    val percentile: Int? = null,
    val orMore: Boolean? = null,
    val gaussImage1: String? = null,
    val errorMessage: String? = null,
)

enum class MensaAgeGroup(val rawValue: Int) {
    Y1617(1617),
    Y1850(1850),
    Y5160(5160),
    Y6199(6199),
}

sealed class MensaTestException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidResponse(cause: Throwable? = null) : MensaTestException(
        "Risposta non valida dal server.${cause?.message?.let { " ($it)" }.orEmpty()}",
        cause,
    )
    class ParseFailure(reason: String) : MensaTestException("Errore di parsing: $reason")
    class HttpError(code: Int) : MensaTestException("Errore HTTP $code")
}
