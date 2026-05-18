package it.mensa.shared.model

data class QuidIssue(
    val id: Long,             // WP category id (negative for PDF-only issues)
    val slug: String,         // e.g. "quid-16-la-fine" or "quid-1-pdf"
    val name: String,         // e.g. "Quid 16 - La Fine"
    val description: String,  // WP category description (often empty — ok)
    val articleCount: Int,    // category.count (0 for PDF-only issues)
    val coverImageUrl: String?, // derived from the latest article's featured media
    val pdfUrl: String? = null, // non-null ⇒ this issue is a PDF, not a set of web articles
)

data class QuidArticle(
    val id: Long,
    val slug: String,
    val link: String,
    val date: String,
    val modified: String,
    val titleHtml: String,
    val excerptHtml: String,
    val contentHtml: String,
    val coverImageUrl: String?,
    val categoryNames: List<String>,
) {
    val titlePlain: String get() = stripHtml(titleHtml)
    val excerptPlain: String get() = stripHtml(excerptHtml)
}

private val htmlTagRegex = Regex("<[^>]+>")
private val htmlEntities = mapOf(
    "&#8211;" to "–",
    "&#8212;" to "—",
    "&#8216;" to "'",
    "&#8217;" to "'",
    "&#8220;" to "\"",
    "&#8221;" to "\"",
    "&#8230;" to "…",
    "&amp;" to "&",
    "&lt;" to "<",
    "&gt;" to ">",
    "&quot;" to "\"",
    "&#8222;" to "„",
    "&nbsp;" to " ",
    "&#160;" to " ",
)

private fun stripHtml(html: String): String {
    var text = htmlTagRegex.replace(html, "")
    htmlEntities.forEach { (entity, replacement) ->
        text = text.replace(entity, replacement)
    }
    return text.trim()
}

data class QuidArticleAudio(
    val id: String,             // PB record id
    val audioUrl: String,       // full absolute https URL, ready for AVPlayer
    val voice: String,          // narrator name
    val contentHash: String,
    val durationSeconds: Int,
)
