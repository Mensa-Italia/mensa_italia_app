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

    private fun hasPower(power: String, user: UserModel?): Boolean {
        if (user == null) return false
        val powers = user.powers.toSet()
        return powers.contains("super") || powers.contains(power) || powers.contains("${power}_helper")
    }

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
        val base = if (state.filterKey == "all") {
            state.sigs
        } else {
            state.sigs.filter { it.groupType.lowercase().contains(state.filterKey) }
        }
        val q = state.query.trim().lowercase()
        if (q.isEmpty()) return base
        return base.filter {
            it.name.lowercase().contains(q) || it.description.lowercase().contains(q)
        }
    }

    fun availableFilterKeys(state: SigListUiState): List<String> {
        val seen = mutableSetOf<String>()
        val keys = mutableListOf<String>()
        for (s in state.sigs) {
            val key = canonicalKey(s.groupType) ?: continue
            if (seen.add(key)) keys.add(key)
        }
        val preferred = listOf("sig", "chat", "local")
        val ordered = preferred.filter { seen.contains(it) } +
            keys.filter { it !in preferred }.sorted()
        return listOf("all") + ordered
    }

    fun canonicalKey(raw: String): String? {
        val lower = raw.lowercase()
        return when {
            lower.contains("chat") -> "chat"
            lower.contains("local") -> "local"
            lower.contains("sig") -> "sig"
            lower.isNotEmpty() -> lower
            else -> null
        }
    }

    fun filterLabel(key: String): String = when (key) {
        "all" -> "Tutti"
        "sig" -> "SIG"
        "chat" -> "Gruppi Telegram"
        "local" -> "Gruppi ufficiali"
        else -> key.replace("_", " ").replaceFirstChar { it.uppercase() }
    }

    fun shortLabel(groupType: String): String {
        val lower = groupType.lowercase()
        return when {
            lower.contains("chat") -> "Gruppi Telegram"
            lower.contains("local") -> "Gruppi ufficiali"
            lower.contains("sig") -> "SIG"
            else -> groupType.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }
}
