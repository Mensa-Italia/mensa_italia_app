package it.mensa.app.features.members

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.RegSociModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MembersDirectoryUiState(
    val members: List<RegSociModel> = emptyList(),
    val query: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

class MembersDirectoryViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MembersDirectoryUiState(loading = true))
    val uiState: StateFlow<MembersDirectoryUiState> = _uiState.asStateFlow()

    private val repo get() = koinAccess().regSoci
    private var observeJob: Job? = null
    private var searchJob: Job? = null

    init {
        startObserving()
        refresh()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(members = list, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(error = e.message, loading = false) }
            }
            .launchIn(viewModelScope)
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                repo.refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun onQueryChange(newValue: String) {
        _uiState.update { it.copy(query = newValue) }
        searchJob?.cancel()
        val trimmed = newValue.trim()
        if (trimmed.length < 2) return
        searchJob = viewModelScope.launch {
            delay(300)
            try {
                repo.searchByName(trimmed)
            } catch (_: Exception) {
            }
        }
    }

    /** Client-side filtered list */
    fun filtered(state: MembersDirectoryUiState): List<RegSociModel> {
        val q = state.query.trim().lowercase()
        if (q.isEmpty()) return state.members
        return state.members.filter {
            it.name.lowercase().contains(q) ||
                it.city.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
        }
    }

    /**
     * Groups members by first letter (A-Z), non-alpha → "#".
     * Returns list of (letter, sortedMembers) pairs.
     */
    fun sectioned(members: List<RegSociModel>): List<Pair<String, List<RegSociModel>>> {
        val grouped = members.groupBy { m ->
            val first = m.name.trim().firstOrNull() ?: return@groupBy "#"
            val folded = first.uppercaseChar().toString()
            if (folded.matches(Regex("[A-Z]"))) folded else "#"
        }
        return grouped.entries
            .map { (letter, list) ->
                letter to list.sortedBy { it.name.lowercase() }
            }
            .sortedWith(Comparator { a, b ->
                when {
                    a.first == "#" -> 1
                    b.first == "#" -> -1
                    else -> a.first.compareTo(b.first)
                }
            })
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
}
