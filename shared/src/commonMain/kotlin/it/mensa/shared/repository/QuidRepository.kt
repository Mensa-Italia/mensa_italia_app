package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.QuidApi
import it.mensa.shared.config.FeatureFlags
import it.mensa.shared.model.QuidArticle
import it.mensa.shared.model.QuidArticleAudio
import it.mensa.shared.model.QuidIssue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class QuidRepository(
    private val api: QuidApi,
    private val flags: FeatureFlags,
) {
    private val _issues = MutableStateFlow<List<QuidIssue>>(emptyList())
    private val _issueArticles = MutableStateFlow<Map<Long, List<QuidArticle>>>(emptyMap())

    fun observeIssues(): Flow<List<QuidIssue>> = _issues.asStateFlow()

    /// Throws on network failure — Swift bridges this as a throwing async call.
    /// kotlin.Result intentionally NOT used here because it doesn't survive K/N → Swift
    /// interop (Swift would receive an opaque box that won't cast back to T).
    @Throws(Exception::class)
    suspend fun refreshIssues() {
        _issues.value = api.listIssues()
    }

    fun observeIssueArticles(issueId: Long): Flow<List<QuidArticle>> =
        _issueArticles.map { it[issueId] ?: emptyList() }

    @Throws(Exception::class)
    suspend fun refreshIssueArticles(issueId: Long) {
        val articles = api.listPostsInIssue(issueId)
        _issueArticles.value = _issueArticles.value + (issueId to articles)
    }

    @Throws(Exception::class)
    suspend fun getArticle(id: Long): QuidArticle = api.getPost(id)

    /**
     * Null anche con `quid_audio_enabled` spento, senza chiedere niente al
     * server: l'audio si accende da configurazione, come l'organigramma. La
     * lista di un numero lo chiede per ogni articolo, e con la lettura chiusa
     * sul server erano altrettanti 403.
     */
    @Throws(Exception::class)
    suspend fun getAudioForArticle(wpId: Long): QuidArticleAudio? =
        if (flags.quidAudioEnabled) api.getAudioForArticle(wpId) else null
}
