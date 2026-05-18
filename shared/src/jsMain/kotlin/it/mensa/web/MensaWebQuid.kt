@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.model.QuidArticle
import it.mensa.shared.model.QuidArticleAudio
import it.mensa.shared.model.QuidIssue
import it.mensa.shared.repository.QuidRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebQuid internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: QuidRepository get() = KoinPlatform.getKoin().get()

    fun subscribeIssues(callback: (issues: Array<MensaWebQuidIssue>) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.observeIssues().collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return { job.cancel() }
    }

    fun refreshIssues(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.refreshIssues()
    }

    /**
     * One-shot fetch (refresh + cache read) for the articles of an issue.
     * `issueId` is `Double` because Kotlin `Long` is not allowed in `@JsExport`;
     * we cast to `Long` internally. WP category IDs fit well inside 2^53 so the
     * conversion is lossless.
     */
    fun articlesForIssue(issueId: Double): Promise<Array<MensaWebQuidArticle>> = scope.promise {
        sdk.awaitReady()
        val id = issueId.toLong()
        runCatching { repo.refreshIssueArticles(id) }
        val list = runCatching { repo.observeIssueArticles(id).first() }.getOrNull().orEmpty()
        list.map { it.toJs(issueId = id) }.toTypedArray()
    }

    /** Fetches the full article body (HTML) from the WordPress backend. */
    fun articleById(id: Double): Promise<MensaWebQuidArticle?> = scope.promise {
        sdk.awaitReady()
        val wpId = id.toLong()
        val article = runCatching { repo.getArticle(wpId) }.getOrNull() ?: return@promise null
        val audio = runCatching { repo.getAudioForArticle(wpId) }.getOrNull()
        article.toJs(audio = audio)
    }
}

/**
 * JS-facing Quid issue. `id` is `Double` because Kotlin's `Long` is not legal
 * in `@JsExport` declarations. The number is the WordPress category id; a
 * negative value indicates a PDF-only issue (matches `QuidIssue.id`).
 */
@JsExport
data class MensaWebQuidIssue(
    val id: Double,
    val slug: String,
    val title: String,
    val number: Int,
    val description: String,
    val articleCount: Int,
    val coverUrl: String,
    val isPdf: Boolean,
    val pdfUrl: String,
)

@JsExport
data class MensaWebQuidArticle(
    val id: Double,
    val issueId: Double,
    val title: String,
    val byline: String,
    val leadHtml: String,
    val bodyHtml: String,
    val heroImageUrl: String,
    val audioUrl: String,
    val durationSec: Int,
    val wpUrl: String,
)

// JS regex doesn't support inline `(?i)`; use Kotlin's RegexOption.IGNORE_CASE
// which compiles to the standard `i` flag on the Kotlin/JS RegExp emission.
private val numberRegex = Regex("^quid[- _]?(\\d+)", RegexOption.IGNORE_CASE)

internal fun QuidIssue.toJs(): MensaWebQuidIssue {
    val number = numberRegex.find(slug)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
    return MensaWebQuidIssue(
        id = id.toDouble(),
        slug = slug,
        title = name,
        number = number,
        description = description,
        articleCount = articleCount,
        coverUrl = coverImageUrl ?: "",
        isPdf = pdfUrl != null,
        pdfUrl = pdfUrl ?: "",
    )
}

internal fun QuidArticle.toJs(
    issueId: Long = 0L,
    audio: QuidArticleAudio? = null,
): MensaWebQuidArticle = MensaWebQuidArticle(
    id = id.toDouble(),
    issueId = issueId.toDouble(),
    title = titlePlain,
    byline = "",
    leadHtml = excerptHtml,
    bodyHtml = contentHtml,
    heroImageUrl = coverImageUrl ?: "",
    audioUrl = audio?.audioUrl ?: "",
    durationSec = audio?.durationSeconds ?: 0,
    wpUrl = link,
)
