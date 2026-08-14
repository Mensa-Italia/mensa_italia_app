package it.mensa.app.features.sigs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.SigModel
import it.mensa.shared.model.UserModel
import it.mensa.shared.repository.SigDraft
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import it.mensa.shared.auth.Powers

data class SigListUiState(
    val sigs: List<SigModel> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val filterKey: String = "all", // "all" | "sig" | "chat" | "local"
    val canControl: Boolean = false,
)

class SigListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SigListUiState(loading = true))
    val uiState: StateFlow<SigListUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().sigs
    private val auth get() = koinAccess().auth
    private var observeJob: Job? = null

    init {
        startObserving()
        observeUser()
        refresh(showSpinner = false)
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(sigs = list, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
            .launchIn(viewModelScope)
    }

    private fun observeUser() {
        auth.currentUser
            .onEach { user ->
                val can = hasPower("sigs", user)
                _uiState.update { it.copy(canControl = can) }
            }
            .launchIn(viewModelScope)
    }

    private fun hasPower(power: String, user: UserModel?): Boolean =
        Powers.has(user?.powers, power)

    fun refresh(showSpinner: Boolean = true) {
        viewModelScope.launch {
            if (showSpinner) _uiState.update { it.copy(refreshing = true) }
            try {
                repo.refresh(filter = null, sort = "name")
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(refreshing = false) }
            }
        }
    }

    fun setQuery(q: String) = _uiState.update { it.copy(query = q) }
    fun setFilter(key: String) = _uiState.update { it.copy(filterKey = key) }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun create(draft: SigDraft) {
        viewModelScope.launch {
            try {
                repo.create(draft)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun update(id: String, draft: SigDraft) {
        viewModelScope.launch {
            try {
                repo.update(id, draft)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            try {
                repo.delete(id)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Derived

    fun filtered(state: SigListUiState): List<SigModel> {
        val base = if (state.filterKey == ALL) {
            state.sigs
        } else {
            // Confronto esatto, non `contains`: con il contains il filtro
            // "chat" tirava dentro anche chat_whatsapp e chat_telegram.
            state.sigs.filter { canonicalKey(it.groupType) == state.filterKey }
        }
        val q = state.query.trim().lowercase()
        if (q.isEmpty()) return base
        return base.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    fun availableFilterKeys(state: SigListUiState): List<String> {
        val seen = mutableSetOf<String>()
        for (s in state.sigs) {
            canonicalKey(s.groupType)?.let(seen::add)
        }
        // I tipi che il server conosce vengono prima, nel loro ordine; quelli
        // che non conosciamo ancora seguono in fondo invece di sparire.
        val ordered = SigGroupType.entries.map { it.rawValue }.filter { it in seen } +
            seen.filter { key -> SigGroupType.entries.none { it.rawValue == key } }.sorted()
        return listOf(ALL) + ordered
    }

    /**
     * Il `group_type` cosi' com'e', senza accorpamenti.
     *
     * Prima questa funzione schiacciava i sei tipi del server in tre secchi
     * per sottostringa, e `chat` veniva controllato per primo: i gruppi
     * WhatsApp finivano nel secchio "chat" insieme ai Telegram, sotto un chip
     * che diceva "Gruppi Telegram". Erano nell'elenco, ma nessuno poteva
     * trovarli, e la loro card diceva la cosa sbagliata.
     */
    fun canonicalKey(raw: String): String? = raw.lowercase().ifEmpty { null }

    companion object {
        const val ALL = "all"
    }
}
