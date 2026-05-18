package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import it.mensa.shared.api.ApiConfig
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.QuidArticle
import it.mensa.shared.model.QuidArticleAudio
import it.mensa.shared.model.QuidIssue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private const val BASE_URL = "https://quid.mensa.it/wp-json/wp/v2"

// ── PocketBase DTOs ───────────────────────────────────────────────────────────

@Serializable
private data class PbQuidIssue(
    val id: String = "",
    @SerialName("category_id") val categoryId: String = "",
    val number: Int = 0,
    val name: String = "",
    val slug: String = "",
    @SerialName("articles_count") val articlesCount: Int = 0,
    val image: String = "",
    @SerialName("pdf_url") val pdfUrl: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    val created: String = "",
    val updated: String = "",
)

@Serializable
private data class PbQuidArticle(
    val id: String = "",
    @SerialName("wp_id") val wpId: String = "",
    val title: String = "",
    val excerpt: String = "",
    val link: String = "",
    val image: String = "",
    @SerialName("category_id") val categoryId: String = "",
    @SerialName("category_name") val categoryName: String = "",
    @SerialName("published_at") val publishedAt: String = "",
    val created: String = "",
    val updated: String = "",
)

@Serializable
private data class PbQuidArticleAudio(
    val id: String = "",
    val article: String = "",
    val audio: String = "",
    val voice: String = "",
    @SerialName("content_hash") val contentHash: String = "",
    @SerialName("duration_seconds") val durationSeconds: Int = 0,
)

private fun PbQuidIssue.toIssue(): QuidIssue {
    // Use WP category_id as the issue id when it's a valid positive number.
    // Fall back to -number for PDF-only issues (category_id empty or "0").
    val id: Long = categoryId
        .toLongOrNull()
        ?.takeIf { it > 0L }
        ?: -number.toLong()

    return QuidIssue(
        id = id,
        slug = slug,
        name = name,
        description = "",
        articleCount = articlesCount,
        coverImageUrl = image.takeIf { it.isNotBlank() },
        pdfUrl = pdfUrl.takeIf { it.isNotBlank() },
    )
}

private fun PbQuidArticle.toArticle(): QuidArticle? {
    val wpIdLong = wpId.toLongOrNull() ?: 0L
    if (wpIdLong == 0L) return null   // defensive: skip rows with unparseable wp_id

    return QuidArticle(
        id = wpIdLong,
        slug = link.substringAfterLast("/").trimEnd('/').ifBlank { id },
        link = link,
        date = publishedAt,
        modified = updated,
        titleHtml = title,
        excerptHtml = excerpt,
        contentHtml = "",   // deliberately empty; fetched lazily by getPost
        coverImageUrl = image.takeIf { it.isNotBlank() },
        categoryNames = listOf(categoryName),
    )
}

// ── WordPress DTOs (kept for getPost) ────────────────────────────────────────

@Serializable
private data class WpRendered(
    val rendered: String = "",
)

@Serializable
private data class WpPost(
    val id: Long = 0,
    val slug: String = "",
    val link: String = "",
    val date: String = "",
    val modified: String = "",
    val title: WpRendered = WpRendered(),
    val excerpt: WpRendered = WpRendered(),
    val content: WpRendered = WpRendered(),
    @SerialName("featured_media")
    val featuredMedia: Long = 0,
    @SerialName("_embedded")
    val embedded: JsonObject? = null,
)

private fun WpPost.toArticle(): QuidArticle {
    val coverImageUrl = embedded
        ?.get("wp:featuredmedia")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonObject
        ?.get("source_url")
        ?.jsonPrimitive
        ?.contentOrNull

    val categoryNames: List<String> = embedded
        ?.get("wp:term")
        ?.jsonArray
        ?.firstOrNull()
        ?.jsonArray
        ?.mapNotNull { term ->
            term.jsonObject["name"]?.jsonPrimitive?.contentOrNull
        }
        ?: emptyList()

    return QuidArticle(
        id = id,
        slug = slug,
        link = link,
        date = date,
        modified = modified,
        titleHtml = title.rendered,
        excerptHtml = excerpt.rendered,
        contentHtml = content.rendered,
        coverImageUrl = coverImageUrl,
        categoryNames = categoryNames,
    )
}

// ── API ───────────────────────────────────────────────────────────────────────

class QuidApi(
    private val pb: PocketBaseClient,
    private val client: HttpClient,
) {

    suspend fun listIssues(): List<QuidIssue> =
        pb.fullList<PbQuidIssue>("quid_issues", sort = "-number")
            .map { it.toIssue() }

    suspend fun listPostsInIssue(issueId: Long, page: Int = 1, perPage: Int = 50): List<QuidArticle> {
        // PDF issues (negative id) have no articles in PocketBase.
        if (issueId <= 0L) return emptyList()

        val filter = "(category_id=\"$issueId\")"
        return pb.list<PbQuidArticle>(
            collection = "quid_articles",
            page = page,
            perPage = perPage,
            filter = filter,
            sort = "-published_at",
        ).items.mapNotNull { it.toArticle() }
    }

    suspend fun getPost(id: Long): QuidArticle {
        val post: WpPost = client.get("$BASE_URL/posts/$id") {
            parameter("_embed", "wp:featuredmedia,wp:term")
        }.body()
        return post.toArticle()
    }

    suspend fun getAudioForArticle(wpId: Long): QuidArticleAudio? {
        val result = pb.list<PbQuidArticleAudio>(
            collection = "quid_articles_audio",
            page = 1,
            perPage = 1,
            filter = "(article.wp_id=\"$wpId\")",
        )
        val item = result.items.firstOrNull() ?: return null
        val audioUrl = "${ApiConfig.BASE_URL}/api/files/quid_articles_audio/${item.id}/${item.audio}"
        return QuidArticleAudio(
            id = item.id,
            audioUrl = audioUrl,
            voice = item.voice,
            contentHash = item.contentHash,
            durationSeconds = item.durationSeconds,
        )
    }
}
