package it.mensa.shared.net

/**
 * Normalizzazione dei link esterni che arrivano dal backend.
 *
 * I campi `link` di PocketBase sono di tipo `url`, ma quella validazione accetta
 * anche un indirizzo senza schema: in produzione ci sono davvero convenzioni
 * salvate come `www.bonavitaly.com`. Una stringa cosi' non e' un URL assoluto e
 * le due piattaforme la trattano in modo diverso, entrambe sbagliato:
 *
 *  - Android: `Uri.parse("www.bonavitaly.com")` non lancia, ma l'Intent
 *    `ACTION_VIEW` che ne esce non ha schema, nessuna app lo sa gestire e
 *    `startActivity` lancia `ActivityNotFoundException` — l'app si chiudeva.
 *  - iOS: `URL(string:)` restituisce un URL *relativo*, e
 *    `UIApplication.open` su un URL relativo non fa assolutamente niente —
 *    il bottone sembrava morto.
 *
 * Qui si decide una volta sola cosa vuol dire "link apribile", cosi' le due
 * app si comportano allo stesso modo e i call site non devono ricordarsi di
 * rattoppare ognuno per conto suo (prima lo faceva solo il dettaglio evento,
 * con uno `startsWith("http")` che accetta anche `httpqualcosa`).
 *
 * `normalize` restituisce `null` quando dalla stringa non si ricava niente di
 * apribile: la UI deve nascondere il bottone invece di aprire il vuoto.
 */
object ExternalUrl {

    /** `schema:` come da RFC 3986: lettera, poi lettere/cifre/`+`/`-`/`.`. */
    private val schemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.\\-]*:")

    /** Un indirizzo mail scritto nudo in un campo link: `info@mensa.it`. */
    private val emailRegex = Regex("^[^\\s@/:]+@[^\\s@/:]+\\.[A-Za-z]{2,}$")

    /**
     * Schemi che non sono seguiti da `//` e che quindi non si riconoscono dalla
     * forma: senza questo elenco `mailto:info@mensa.it` sarebbe indistinguibile
     * da un `host:porta`.
     */
    private val schemesWithoutSlashes = setOf(
        "mailto", "tel", "sms", "smsto", "callto", "geo", "maps", "market",
        "whatsapp", "tg", "skype", "facetime", "facetime-audio", "sip", "im",
        "bitcoin", "magnet", "webcal",
    )

    /**
     * Schemi che non passiamo mai al sistema operativo.
     *
     * Il contenuto di questi campi lo scrivono gli amministratori delle sedi
     * locali dal pannello, non e' codice nostro: `javascript:` e `data:`
     * eseguono, `file:`/`content:` leggono il disco e `intent:` su Android sa
     * far partire componenti arbitrari. Un link di una convenzione non ha
     * nessun motivo di essere una di queste cose.
     */
    private val blockedSchemes = setOf(
        "javascript", "data", "vbscript", "blob",
        "file", "content", "intent", "android-app", "jar",
    )

    /**
     * Porta [raw] a un URL assoluto che il sistema sa aprire, o restituisce
     * `null` se non e' possibile.
     *
     *  - `www.bonavitaly.com` → `https://www.bonavitaly.com`
     *  - `//cdn.mensa.it/x`   → `https://cdn.mensa.it/x`
     *  - `HTTP://Mensa.it`    → `http://Mensa.it` (schema minuscolo: i filtri
     *    intent di Android confrontano lo schema cosi' com'e')
     *  - `info@mensa.it`      → `mailto:info@mensa.it`
     *  - `Chiedere in sede`   → `null`
     */
    fun normalize(raw: String?): String? {
        val trimmed = raw?.trim { it.isWhitespace() || it == '\u00A0' || it == '\u200B' }
        if (trimmed.isNullOrEmpty()) return null

        val scheme = schemeOf(trimmed)
        if (scheme != null) {
            if (scheme in blockedSchemes) return null
            val rebuilt = scheme + trimmed.substring(trimmed.indexOf(':'))
            return if (scheme == "http" || scheme == "https") sanitizeWeb(rebuilt) else rebuilt
        }

        // Senza schema restano tre casi: protocol-relative, mail nuda, dominio.
        return when {
            trimmed.startsWith("//") -> sanitizeWeb("https:$trimmed")
            emailRegex.matches(trimmed) -> "mailto:$trimmed"
            else -> sanitizeWeb("https://$trimmed")
        }
    }

    /**
     * Lo schema di [value] in minuscolo, o `null` se non ne ha uno.
     *
     * Il solo `^schema:` non basta a decidere: `www.mensa.it:8080/x` lo
     * soddisfa, con "www.mensa.it" al posto dello schema, e verrebbe passato al
     * sistema cosi' com'e' — cioe' non aprirebbe niente. Le tre regole qui
     * sotto separano uno schema da un `host:porta`.
     */
    private fun schemeOf(value: String): String? {
        if (!schemeRegex.containsMatchIn(value)) return null
        val separator = value.indexOf(':')
        val candidate = value.substring(0, separator).lowercase()
        val rest = value.substring(separator + 1)

        // Un punto nello schema, in pratica, vuol dire che e' un nome a dominio.
        if (candidate.contains('.')) return null
        // `schema://host`: non c'e' altro modo di leggerlo.
        if (rest.startsWith("//")) return candidate
        // Gli schemi senza `//` sono noti: vanno riconosciuti prima della
        // regola sulla porta, o `tel:0212345` diventerebbe un `host:porta`.
        if (candidate in schemesWithoutSlashes) return candidate
        // `host:8080` e `host:8080/path`: la porta e' solo cifre.
        val digits = rest.takeWhile { it.isDigit() }
        if (digits.isNotEmpty() && digits.length == rest.length) return null
        if (digits.isNotEmpty() && rest.getOrNull(digits.length) == '/') return null

        return candidate
    }

    /**
     * `true` se [url] e' un indirizzo web, cioe' se ha senso aprirlo in un
     * browser in-app (Custom Tabs su Android, `SFSafariViewController` su iOS).
     * Per `mailto:`/`tel:` la risposta e' no: vanno passati al sistema.
     *
     * Si aspetta un valore gia' passato da [normalize].
     */
    fun isWebUrl(url: String): Boolean {
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        return scheme == "http" || scheme == "https"
    }

    /**
     * Controlla che un `http(s)://` abbia davvero un host, e codifica gli spazi
     * che restano nel percorso.
     *
     * Serve perche' il passo precedente e' ottimista: appiccicare `https://` a
     * una stringa qualsiasi produce sempre qualcosa che *sembra* un URL. Senza
     * questo controllo un campo compilato a mano con "Chiedere in sede"
     * diventerebbe `https://Chiedere in sede` e il bottone aprirebbe una
     * ricerca a caso invece di non comparire.
     */
    private fun sanitizeWeb(url: String): String? {
        val afterScheme = url.substringAfter("://", missingDelimiterValue = "")
        if (afterScheme.isEmpty()) return null

        val authorityEnd = afterScheme.indexOfFirst { it == '/' || it == '?' || it == '#' }
        val authority = if (authorityEnd < 0) afterScheme else afterScheme.substring(0, authorityEnd)
        val rest = if (authorityEnd < 0) "" else afterScheme.substring(authorityEnd)

        // Uno spazio prima della barra vuol dire che non era un indirizzo:
        // `Nome <info@mensa.it>` altrimenti passerebbe, perche' guardando solo
        // la parte dopo la chiocciola si legge un host che sembra buono.
        if (authority.any { it.isWhitespace() || it == ' ' }) return null

        // Via le credenziali `utente:password@` prima di guardare l'host.
        val hostPort = authority.substringAfterLast('@')
        val host = if (hostPort.startsWith("[")) {
            hostPort.substringBefore(']').removePrefix("[") // IPv6
        } else {
            hostPort.substringBefore(':')
        }

        if (host.isEmpty()) return null
        if (host.any { it.isWhitespace() || it == '\u00A0' }) return null
        // Un host senza punto non esiste sulla rete pubblica: e' testo libero.
        if (!host.contains('.') && !host.equals("localhost", ignoreCase = true) && !hostPort.startsWith("[")) {
            return null
        }

        val scheme = url.substringBefore("://")
        return scheme + "://" + authority + rest.replace(" ", "%20")
    }
}
