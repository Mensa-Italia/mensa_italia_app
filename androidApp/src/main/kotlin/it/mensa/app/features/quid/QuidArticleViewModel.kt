package it.mensa.app.features.quid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.QuidArticle
import it.mensa.shared.model.QuidArticleAudio
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QuidArticleState(
    val article: QuidArticle? = null,
    val audio: QuidArticleAudio? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

/**
 * QuidArticleViewModel — loads a single article + optional audio narration.
 *
 * Mirrors iOS QuidArticleView.load():
 * - getArticle() blocking
 * - getAudioForArticle() best-effort (failure silently hides banner)
 */
class QuidArticleViewModel(private val articleId: Long) : ViewModel() {

    private val repo = koinAccess().quid

    private val _state = MutableStateFlow(QuidArticleState())
    val state: StateFlow<QuidArticleState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            try {
                // Fetch article + audio concurrently
                val articleDeferred = async { repo.getArticle(articleId) }
                val audioDeferred = async {
                    try { repo.getAudioForArticle(articleId) } catch (_: Exception) { null }
                }

                val article = articleDeferred.await()
                val audio = audioDeferred.await()

                _state.update {
                    it.copy(
                        article = article,
                        audio = audio,
                        loading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(loading = false, error = e.message ?: "Errore sconosciuto") }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
