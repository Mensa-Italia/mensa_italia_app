package it.mensa.shared.quid

/**
 * A piece of an article body as identified by the pure HTML splitter.
 *
 * The iOS layer turns each block into a UIKit/SwiftUI view; this shared code
 * only does the regex-based block IDENTIFICATION. Subclasses are top-level
 * (flat naming) for clean Kotlin/Native bridging — Swift sees them as
 * `QuidHtmlBlockText`, `QuidHtmlBlockImage`, `QuidHtmlBlockBlockquote`,
 * `QuidHtmlBlockHeading` without nested-type ceremony.
 */
sealed class QuidHtmlBlock

/** Inline HTML run with no `<img>`, `<blockquote>` or `<hN>` boundary inside. */
data class QuidHtmlBlockText(val html: String) : QuidHtmlBlock()

/** Resolved image reference extracted from an `<img>` tag. */
data class QuidHtmlBlockImage(val url: String, val alt: String?) : QuidHtmlBlock()

/** Inner HTML of a `<blockquote>...</blockquote>`. */
data class QuidHtmlBlockBlockquote(val html: String) : QuidHtmlBlock()

/** Inner HTML of an `<h2>`, `<h3>` or `<h4>`, with the original level (2/3/4). */
data class QuidHtmlBlockHeading(val level: Int, val html: String) : QuidHtmlBlock()

/**
 * Splits a WordPress HTML body into a stream of blocks for native rendering.
 *
 * The block-identification logic is pure: regex on `<img>`, then within each
 * text run regex on `<blockquote>` and `<h2..h4>`. Image src resolution
 * prefers WP lazy-load attributes (`data-src` → largest `srcset` candidate →
 * `src`) so we don't render the 1×1 placeholder.
 */
object QuidHtmlParser {

    // `[^>]` already excludes `>` (newline-safe). Avoid `RegexOption.DOT_MATCHES_ALL`
    // because it's JVM-only; not available on Kotlin/JS where the host platform
    // regex engine is JS RegExp.
    private val imgRegex = Regex("<img[^>]*?>", RegexOption.IGNORE_CASE)
    private val blockRegex = Regex(
        "<(blockquote|h2|h3|h4)[^>]*>([\\s\\S]*?)</\\1>",
        RegexOption.IGNORE_CASE
    )

    fun parse(html: String): List<QuidHtmlBlock> {
        val blocks = mutableListOf<QuidHtmlBlock>()
        var cursor = 0
        for (m in imgRegex.findAll(html)) {
            if (m.range.first > cursor) {
                val chunk = html.substring(cursor, m.range.first)
                blocks += splitTextChunk(chunk)
            }
            parseImage(m.value)?.let { blocks += it }
            cursor = m.range.last + 1
        }
        if (cursor < html.length) {
            blocks += splitTextChunk(html.substring(cursor))
        }
        return blocks
    }

    /**
     * Split a piece of HTML (no `<img>` inside) on `<blockquote>` and
     * `<h2|h3|h4>` boundaries so the iOS layer can render those natively.
     */
    private fun splitTextChunk(html: String): List<QuidHtmlBlock> {
        val matches = blockRegex.findAll(html).toList()
        if (matches.isEmpty()) {
            return if (html.isNotBlank()) listOf(QuidHtmlBlockText(html)) else emptyList()
        }
        val out = mutableListOf<QuidHtmlBlock>()
        var cursor = 0
        for (m in matches) {
            if (m.range.first > cursor) {
                val chunk = html.substring(cursor, m.range.first)
                if (chunk.isNotBlank()) out += QuidHtmlBlockText(chunk)
            }
            val tagName = m.groupValues[1].lowercase()
            val inner = m.groupValues[2]
            out += when (tagName) {
                "blockquote" -> QuidHtmlBlockBlockquote(inner)
                "h2" -> QuidHtmlBlockHeading(2, inner)
                "h3" -> QuidHtmlBlockHeading(3, inner)
                "h4" -> QuidHtmlBlockHeading(4, inner)
                else -> QuidHtmlBlockText(inner)
            }
            cursor = m.range.last + 1
        }
        if (cursor < html.length) {
            val tail = html.substring(cursor)
            if (tail.isNotBlank()) out += QuidHtmlBlockText(tail)
        }
        return out
    }

    private fun parseImage(tag: String): QuidHtmlBlockImage? {
        val resolved = attribute("data-src", tag)
            ?: largestFromSrcset(attribute("srcset", tag))
            ?: attribute("src", tag)
            ?: return null
        if (resolved.isBlank()) return null
        return QuidHtmlBlockImage(url = resolved, alt = attribute("alt", tag))
    }

    /** Pick the widest variant URL out of a WordPress `srcset` string. */
    private fun largestFromSrcset(srcset: String?): String? {
        if (srcset.isNullOrEmpty()) return null
        val candidates = srcset.split(",").mapNotNull { part ->
            val trimmed = part.trim()
            if (trimmed.isEmpty()) return@mapNotNull null
            val comps = trimmed.split(Regex("\\s+"), limit = 2)
            val urlPart = comps.firstOrNull() ?: return@mapNotNull null
            val widthPart = comps.getOrNull(1)?.trim()?.let {
                if (it.endsWith("w", ignoreCase = true)) it.dropLast(1) else it
            }
            val width = widthPart?.toIntOrNull() ?: 0
            urlPart to width
        }
        return candidates.maxByOrNull { it.second }?.first
    }

    /** Read an HTML attribute value (double- or single-quoted) out of a tag. */
    private fun attribute(name: String, tag: String): String? {
        val pattern = Regex(
            "$name\\s*=\\s*\"([^\"]*)\"|$name\\s*=\\s*'([^']*)'",
            RegexOption.IGNORE_CASE
        )
        val m = pattern.find(tag) ?: return null
        // Return whichever capture group matched (double- or single-quoted).
        for (i in 1..m.groupValues.lastIndex) {
            val g = m.groups[i]?.value
            if (g != null) return g
        }
        return null
    }
}
