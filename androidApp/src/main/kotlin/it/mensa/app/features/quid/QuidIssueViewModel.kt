package it.mensa.app.features.quid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.QuidArticle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuidIssueState(
    val articles: List<QuidArticle> = emptyList(),
    val issueName: String = "",
    val query: String = "",
    val refreshing: Boolean = false,
    val error: String? = null,
) {
    /** Filtered articles — case/diacritic-insensitive, same as iOS. */
    val filtered: List<QuidArticle>
        get() {
            val q = query.trim()
            if (q.isEmpty()) return articles
            val needle = q.lowercase()
            return articles.filter { a ->
                val haystack = buildString {
                    append(a.titlePlain)
                    append(" ")
                    append(a.excerptPlain)
                    append(" ")
                    append(a.categoryNames.joinToString(" "))
                }.lowercase()
                haystack.contains(needle)
            }
        }

    val hasArticles: Boolean get() = articles.isNotEmpty()
    val hasResults: Boolean get() = filtered.isNotEmpty()
    val isFilterActive: Boolean get() = query.trim().isNotEmpty()
}

/**
 * QuidIssueViewModel — detail for a single issue with its articles.
 *
 * Mirrors iOS QuidIssueView data layer.
 */
class QuidIssueViewModel(private val issueId: Long, initialName: String = "") : ViewModel() {

    private val repo = koinAccess().quid

    private val _state = MutableStateFlow(QuidIssueState(issueName = initialName))
    val state: StateFlow<QuidIssueState> = _state.asStateFlow()

    init {
        // Subscribe to articles flow
        repo.observeIssueArticles(issueId)
            .onEach { articles -> _state.update { it.copy(articles = articles) } }
            .launchIn(viewModelScope)

        // If name not provided, resolve from issues flow
        if (initialName.isEmpty()) {
            repo.observeIssues()
                .onEach { issues ->
                    val match = issues.firstOrNull { it.id == issueId }
                    if (match != null) {
                        _state.update { it.copy(issueName = match.name) }
                    }
                }
                .launchIn(viewModelScope)

            viewModelScope.launch {
                try { repo.refreshIssues() } catch (_: Exception) { }
            }
        }

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            try {
                repo.refreshIssueArticles(issueId)
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message) }
            } finally {
                _state.update { it.copy(refreshing = false) }
            }
        }
    }

    fun setQuery(q: String) = _state.update { it.copy(query = q) }

    fun clearError() = _state.update { it.copy(error = null) }
}
