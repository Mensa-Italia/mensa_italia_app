package it.mensa.shared.search

/**
 * Pure URL/string parsing helpers shared between platforms.
 *
 * Behaviour mirrors the original Swift implementations in
 * `iosApp/.../SearchViewModel.swift`. All functions are stateless and
 * have no platform dependencies — safe to call from any thread.
 */
object SearchParsers {

    /**
     * Extracts the last path segment ("slug") from a deep link
     * `mensa://<expectedHost>/<slug>`.
     *
     * Returns null if the scheme is not `mensa`, the host does not match,
     * or the slug is empty/missing.
     *
     * Examples:
     *  - `mensa://local-office/lombardia` + host=`local-office` → `"lombardia"`
     *  - `mensa://other/foo`              + host=`local-office` → `null`
     */
    fun parseDeepLinkSlug(deepLink: String, expectedHost: String): String? {
        val (host, segments) = parseMensaUrl(deepLink) ?: return null
        if (host != expectedHost) return null
        val last = segments.lastOrNull().orEmpty()
        return if (last.isEmpty()) null else last
    }

    /**
     * Extracts the trailing integer from a deep link `mensa://<expectedHost>/<id>`.
     *
     * Returns null if the scheme/host don't match or the last segment isn't a
     * valid Long. Swift's source returns `Int64?` — we mirror with `Long?`.
     *
     * Examples:
     *  - `mensa://quid/113`        + host=`quid`         → 113
     *  - `mensa://quid-article/42` + host=`quid-article` → 42
     */
    fun parseDeepLinkLastInt(deepLink: String, expectedHost: String): Long? {
        val (host, segments) = parseMensaUrl(deepLink) ?: return null
        if (host != expectedHost) return null
        val last = segments.lastOrNull().orEmpty()
        return last.toLongOrNull()
    }

    /**
     * Extracts the PocketBase user id from a canonical members_registry file URL,
     * e.g. `https://svc.mensa.it/api/files/members_registry/<userId>/<filename>`.
     *
     * Returns null for unrecognised shapes.
     */
    fun parseMemberIdFromImageURL(raw: String): String? {
        if (raw.isEmpty()) return null
        val regex = Regex("/members_registry/([^/]+)/")
        val match = regex.find(raw) ?: return null
        val userId = match.groupValues.getOrNull(1).orEmpty()
        return if (userId.isEmpty()) null else userId
    }

    /**
     * Finds the first run of digits in a string and returns its integer value.
     * Used to surface counts like "22 articoli" → 22, "1 articolo" → 1.
     * Returns 0 when no digits are present — matches Swift's graceful fallback.
     */
    fun parseArticleCount(subtitle: String): Int {
        val match = Regex("\\d+").find(subtitle) ?: return 0
        return match.value.toIntOrNull() ?: 0
    }

    // --- private helpers -------------------------------------------------

    /**
     * Parses a `mensa://<host>/<...path>` style URL into `(host, pathSegments)`.
     * Returns null when the scheme is not `mensa` or the URL is malformed.
     *
     * Implemented manually (rather than via `java.net.URI`) so it stays
     * commonMain-friendly and reproduces Swift's `URL`/`URLComponents` parsing
     * for the limited shapes we care about.
     */
    private fun parseMensaUrl(deepLink: String): Pair<String, List<String>>? {
        if (deepLink.isEmpty()) return null
        val schemeSep = "://"
        val idx = deepLink.indexOf(schemeSep)
        if (idx <= 0) return null
        val scheme = deepLink.substring(0, idx)
        if (scheme != "mensa") return null
        val rest = deepLink.substring(idx + schemeSep.length)
        if (rest.isEmpty()) return null

        // Strip query/fragment — `URL.path` in Swift excludes them.
        val pathEnd = rest.indexOfAny(charArrayOf('?', '#')).let {
            if (it == -1) rest.length else it
        }
        val authorityAndPath = rest.substring(0, pathEnd)

        val slash = authorityAndPath.indexOf('/')
        val host: String
        val path: String
        if (slash == -1) {
            host = authorityAndPath
            path = ""
        } else {
            host = authorityAndPath.substring(0, slash)
            path = authorityAndPath.substring(slash)
        }
        if (host.isEmpty()) return null

        val segments = path.split('/').filter { it.isNotEmpty() }
        return host to segments
    }
}
