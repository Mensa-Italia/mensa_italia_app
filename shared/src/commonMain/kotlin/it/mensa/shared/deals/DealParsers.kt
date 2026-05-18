package it.mensa.shared.deals

/**
 * Stateless parsers for deal-related text heuristics shared across platforms.
 *
 * Mirrors the Swift implementation in `DealSearchResultRow.swift`'s
 * `discountBadge` computed property: scan candidate strings (e.g. `details`,
 * `who`) for a "NN%" pattern and return the numeric percent.
 */
object DealParsers {

    private val regex = Regex("""(\d{1,3})\s?%""")

    /**
     * Returns the first 1-3 digit percentage found in [text], or `null`.
     *
     * Matches `\d{1,3}\s?%` — same pattern as the iOS regex.
     */
    fun extractDiscountPercent(text: String): Int? {
        val match = regex.find(text) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    /**
     * Scans multiple candidate strings in order and returns the first percent
     * found. `null` candidates are ignored, preserving Swift's
     * `compactMap { $0 }` semantics.
     */
    fun extractDiscountPercentFromCandidates(candidates: List<String?>): Int? {
        for (text in candidates) {
            if (text == null) continue
            val percent = extractDiscountPercent(text)
            if (percent != null) return percent
        }
        return null
    }
}
