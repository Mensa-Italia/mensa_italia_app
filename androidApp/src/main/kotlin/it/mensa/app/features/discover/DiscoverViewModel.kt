package it.mensa.app.features.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Stato UI per la schermata Discover.
 *
 * @param isLoading  true finché l'elenco iniziale non è ancora disponibile
 * @param sections   sezioni risolte (comunità + risorse + strumenti), già filtrate
 *                   per item power-gated (es. TestAssistant visibile solo a "testmakers")
 * @param currentUser utente autenticato, usato per personalizzazione futura
 */
data class DiscoverUiState(
    val isLoading: Boolean = true,
    val sections: List<ResolvedDiscoverSection> = emptyList(),
    val currentUser: UserModel? = null,
)

data class ResolvedDiscoverSection(
    val titleKey: String,
    val titleFallback: String,
    val kickerKey: String,
    val kickerFallback: String,
    val categories: List<DiscoverCategory>,
)

/**
 * DiscoverViewModel — thin state holder per la tab Discover.
 *
 * - Si sottoscrive a [auth.currentUser] per rilevare flag di potere
 *   (testmakers → TestAssistant visibile)
 * - Inietta o rimuove [DiscoverCategory.TestAssistant] dalla sezione Strumenti
 * - Non fa fetch di rete — il contenuto è tile statiche di categoria
 */
class DiscoverViewModel : ViewModel() {

    private val auth = koinAccess().auth

    private val _error = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DiscoverUiState> = auth.currentUser
        .combine(_error) { user, _ -> user }
        .catch { e ->
            Logger.e("DiscoverViewModel", "Errore osservando auth state", cause = e)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )
        .let { userFlow ->
            combine(userFlow, _error) { user, _ ->
                DiscoverUiState(
                    isLoading = false,
                    currentUser = user,
                    sections = buildSections(user),
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = DiscoverUiState(isLoading = true),
            )
        }

    // ── Costruttore sezioni ──────────────────────────────────────────────────

    private fun buildSections(user: UserModel?): List<ResolvedDiscoverSection> {
        val canSeeTestAssistant = user?.powers?.contains("testmakers") == true

        return discoverSections.map { section ->
            val cats = if (section.titleKey == "discover.section.addons") {
                val mutable = section.categories.toMutableList()
                mutable.remove(DiscoverCategory.TestAssistant)
                if (canSeeTestAssistant) {
                    val hubIdx = mutable.indexOf(DiscoverCategory.AddonsHub)
                    if (hubIdx >= 0) {
                        mutable.add(hubIdx, DiscoverCategory.TestAssistant)
                    } else {
                        mutable.add(DiscoverCategory.TestAssistant)
                    }
                }
                mutable
            } else {
                section.categories
            }

            ResolvedDiscoverSection(
                titleKey = section.titleKey,
                titleFallback = section.titleFallback,
                kickerKey = section.kickerKey,
                kickerFallback = section.kickerFallback,
                categories = cats,
            )
        }
    }

    // ── Refresh (pull-to-refresh per fasi future) ────────────────────────────

    fun refresh() {
        viewModelScope.launch {
            runCatching { koinAccess().addons.refresh() }
                .onFailure { e -> Logger.e("DiscoverViewModel", "Refresh addon fallito", cause = e) }
        }
    }
}
