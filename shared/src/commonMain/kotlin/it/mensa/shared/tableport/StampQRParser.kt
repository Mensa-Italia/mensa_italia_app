package it.mensa.shared.tableport

/**
 * Parses stamp QR payloads of the form `stampId:::verificationCode`.
 *
 * Stateless, no DI required.
 */
data class StampQRPayload(
    val stampId: String,
    val verificationCode: String,
)

object StampQRParser {
    private const val SEPARATOR = ":::"

    /**
     * Returns the parsed payload, or `null` if the raw string does not contain at least
     * two `:::`-separated, non-empty (after whitespace trimming) components.
     */
    fun parse(raw: String): StampQRPayload? {
        val parts = raw.split(SEPARATOR)
        if (parts.size < 2) return null
        val stampId = parts[0].trim()
        val verificationCode = parts[1].trim()
        if (stampId.isEmpty() || verificationCode.isEmpty()) return null
        return StampQRPayload(stampId = stampId, verificationCode = verificationCode)
    }
}
