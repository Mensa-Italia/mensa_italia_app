package it.mensa.shared.geo

/**
 * Normalizzazione dei nomi di regione, per confrontarli senza farsi fermare da
 * trattini, apostrofi, accenti e maiuscole.
 *
 * kotlin-stdlib non ha `java.text.Normalizer` in commonMain, quindi le lettere
 * accentate le pieghiamo a mano: e' un insieme chiuso e copre le grafie che
 * arrivano dal backend e dai geocoder.
 */
internal object TextNormalizer {

    private val folded: Map<Char, String> = mapOf(
        'à' to "a", 'á' to "a", 'â' to "a", 'ã' to "a", 'ä' to "a", 'å' to "a", 'ā' to "a",
        'è' to "e", 'é' to "e", 'ê' to "e", 'ë' to "e", 'ē' to "e",
        'ì' to "i", 'í' to "i", 'î' to "i", 'ï' to "i", 'ī' to "i",
        'ò' to "o", 'ó' to "o", 'ô' to "o", 'õ' to "o", 'ö' to "o", 'ø' to "o", 'ō' to "o",
        'ù' to "u", 'ú' to "u", 'û' to "u", 'ü' to "u", 'ū' to "u",
        'ç' to "c", 'ñ' to "n", 'ý' to "y", 'ÿ' to "y",
        'æ' to "ae", 'œ' to "oe", 'ß' to "ss",
    )

    /**
     * Minuscolo, accenti piegati, ogni carattere non alfanumerico diventa uno
     * spazio, spazi collassati: "Valle d'Aosta" e "Valle d’Aosta" e
     * "VALLE D AOSTA" diventano tutti "valle d aosta".
     */
    fun normalize(raw: String): String {
        val sb = StringBuilder(raw.length)
        for (ch in raw.lowercase()) {
            val replacement = folded[ch]
            when {
                replacement != null -> sb.append(replacement)
                ch.isLetterOrDigit() -> sb.append(ch)
                else -> sb.append(' ')
            }
        }
        return sb.toString().split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }
}
