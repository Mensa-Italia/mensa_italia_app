package it.mensa.app.features.profile.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.OrgChartGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrgChartUiState(
    val groups: List<OrgChartGroup> = emptyList(),
    val loading: Boolean = true,
    val searchQuery: String = "",
    val errorMessage: String? = null,
)

class OrgChartViewModel : ViewModel() {

    private val orgChart = koinAccess().orgChart

    private val _uiState = MutableStateFlow(OrgChartUiState())
    val uiState: StateFlow<OrgChartUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { orgChart.fetch() }.fold(
                onSuccess = { model ->
                    _uiState.update { it.copy(groups = model.groups, loading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, errorMessage = e.message) }
                },
            )
        }
    }

    fun onSearchChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun filteredGroups(): List<OrgChartGroup> {
        val query = _uiState.value.searchQuery.trim()
        val all = _uiState.value.groups.filter { it.members.isNotEmpty() }
        if (query.isEmpty()) return all
        val needle = query.lowercase()
        return all.filter { group ->
            localizedTitle(group.title).lowercase().contains(needle)
        }
    }

    fun localizedTitle(raw: String): String =
        raw.replace("_", " ").replace("-", " ")
            .replaceFirstChar { it.uppercase() }

    fun dismissError() = _uiState.update { it.copy(errorMessage = null) }
}
