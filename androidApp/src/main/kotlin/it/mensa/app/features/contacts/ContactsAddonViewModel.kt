package it.mensa.app.features.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.OrgChartGroup
import it.mensa.shared.model.OrgChartMember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ContactsAddonUiState(
    val groups: List<OrgChartGroup> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
)

class ContactsAddonViewModel : ViewModel() {

    private val orgChart = koinAccess().orgChart

    private val _uiState = MutableStateFlow(ContactsAddonUiState())
    val uiState: StateFlow<ContactsAddonUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            runCatching { orgChart.fetch() }.fold(
                onSuccess = { model ->
                    _uiState.update { it.copy(groups = model.groups, loading = false) }
                },
                onFailure = { e ->
                    _uiState.update { it.copy(loading = false, error = e.message) }
                },
            )
        }
    }

    fun onSearchChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun filteredGroups(): List<OrgChartGroup> {
        val state = _uiState.value
        val query = state.searchQuery.trim().lowercase()
        val all = state.groups.filter { it.members.isNotEmpty() }
        if (query.isEmpty()) return all
        return all.mapNotNull { group ->
            val filteredMembers = group.members.filter { member ->
                member.name.lowercase().contains(query) ||
                    member.role.lowercase().contains(query)
            }
            if (filteredMembers.isNotEmpty()) group.copy(members = filteredMembers) else null
        }
    }

    fun localizedGroupTitle(raw: String): String =
        raw.replace("_", " ").replace("-", " ")
            .replaceFirstChar { it.uppercase() }
}
