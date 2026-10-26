package it.mensa.shared.notifications

import it.mensa.shared.tableport.StampQRParser
import it.mensa.shared.tableport.StampQRPayload

/** Un link `https://svc.mensa.it/links/...` che l'app sa aprire. */
sealed class AppLink {
    data class Event(val eventId: String) : AppLink()
    data class Stamp(val payload: StampQRPayload) : AppLink()
}

/**
 * Legge i link universali (iOS) / App Links (Android) di `svc.mensa.it`.
 *
 * Sono quelli che il backend stampa sui QR dei francobolli e mette nei
 * messaggi di condivisione degli eventi:
 *
 *  - `/links/event/<eventId>`
 *  - `/links/stamp/<stampId>:::<codice>`
 *
 * Il server li dichiara gia' (`apple-app-site-association` su tutto `/links`,
 * `assetlinks.json`); la vecchia app Flutter li apriva, quella nativa no, e
 * inquadrare il QR con la fotocamera finiva sulla pagina web che dice "apri
 * l'app".
 */
object AppLinkParser {
    private val HOSTS = setOf("svc.mensa.it")

    fun parse(url: String): AppLink? {
        val trimmed = url.trim()
        val afterScheme = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed.substring(8)
            trimmed.startsWith("http://", ignoreCase = true) -> trimmed.substring(7)
            else -> return null
        }
        val host = afterScheme.substringBefore('/').substringBefore(':').lowercase()
        if (host !in HOSTS) return null

        val path = afterScheme.substringAfter('/', "")
            .substringBefore('?')
            .substringBefore('#')
        // Alcuni lettori di QR riscrivono `:::` come `%3A%3A%3A`.
        val segments = path.split('/').filter { it.isNotEmpty() }.map(::percentDecode)
        if (segments.size < 3 || segments[0] != "links") return null

        return when (segments[1]) {
            "event" -> segments[2].takeIf { it.isNotBlank() }?.let { AppLink.Event(it) }
            "stamp" -> StampQRParser.parse(segments.drop(2).joinToString("/"))?.let { AppLink.Stamp(it) }
            else -> null
        }
    }

    private fun percentDecode(s: String): String {
        if ('%' !in s) return s
        val bytes = ArrayList<Byte>(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            val hex = if (c == '%' && i + 2 < s.length) s.substring(i + 1, i + 3).toIntOrNull(16) else null
            if (hex != null) {
                bytes.add(hex.toByte())
                i += 3
            } else {
                c.toString().encodeToByteArray().forEach { bytes.add(it) }
                i++
            }
        }
        return bytes.toByteArray().decodeToString()
    }
}
