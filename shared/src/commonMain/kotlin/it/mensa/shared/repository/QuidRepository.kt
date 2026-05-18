package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.QuidApi
import it.mensa.shared.model.QuidArticle
import it.mensa.shared.model.QuidArticleAudio
import it.mensa.shared.model.QuidIssue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class QuidRepository(
    private val api: QuidApi,
) {
    private val _issues = MutableStateFlow<List<QuidIssue>>(emptyList())
    private val _issueArticles = MutableStateFlow<Map<Long, List<QuidArticle>>>(emptyMap())

    fun observeIssues(): Flow<List<QuidIssue>> = _issues.asStateFlow()

    /// Throws on network failure — Swift bridges this as a throwing async call.
    /// kotlin.Result intentionally NOT used here because it doesn't survive K/N → Swift
    /// interop (Swift would receive an opaque box that won't cast back to T).
    suspend fun refreshIssues() {
        _issues.value = api.listIssues()
    }

    fun observeIssueArticles(issueId: Long): Flow<List<QuidArticle>> =
        _issueArticles.map { it[issueId] ?: emptyList() }

    suspend fun refreshIssueArticles(issueId: Long) {
        val articles = api.listPostsInIssue(issueId)
        _issueArticles.value = _issueArticles.value + (issueId to articles)
    }

    suspend fun getArticle(id: Long): QuidArticle = api.getPost(id)

    suspend fun getAudioForArticle(wpId: Long): QuidArticleAudio? = api.getAudioForArticle(wpId)
}
