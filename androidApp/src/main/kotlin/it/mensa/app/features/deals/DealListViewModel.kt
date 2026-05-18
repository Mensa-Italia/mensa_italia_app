package it.mensa.app.features.deals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── UI state ────────────────────────────────────────────────────────────────

data class DealListUiState(
    val allDeals: List<DealModel> = emptyList(),
    val refreshing: Boolean = false,
    val error: String? = null,
    val currentUser: UserModel? = null,
    val selectedSector: String? = null,
    val searchText: String = "",
) {
    val sectors: List<String>
        get() = allDeals
            .map { it.commercialSector }
            .filter { it.isNotBlank() }
            .toSet()
            .sorted()

    val filteredDeals: List<DealModel>
        get() {
            val query = searchText.trim().lowercase()
            return allDeals.filter { d ->
                if (selectedSector != null && d.commercialSector != selectedSector) return@filter false
                if (query.isEmpty()) return@filter true
                if (d.name.lowercase().contains(query)) return@filter true
                if (d.details?.lowercase()?.contains(query) == true) return@filter true
                if (d.commercialSector.lowercase().contains(query)) return@filter true
                false
            }
        }

    val isFilterActive: Boolean
        get() = selectedSector != null || searchText.isNotBlank()

    val canAddDeal: Boolean
        get() {
            val user = currentUser ?: return false
            val allowed = setOf("super", "admin", "deals", "deals_admin")
            return user.powers.any { it in allowed }
        }
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

class DealListViewModel : ViewModel() {

    private val koin = koinAccess()

    private val _state = MutableStateFlow(DealListUiState())
    val state: StateFlow<DealListUiState> = _state.asStateFlow()

    init {
        // Observe deals cache flow
        viewModelScope.launch {
            koin.deals.observeAll().collect { deals ->
                _state.update { it.copy(allDeals = deals) }
            }
        }
        // Observe current user flow
        viewModelScope.launch {
            koin.auth.currentUser.collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
        // Initial background refresh
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, error = null) }
            try {
                koin.deals.refresh()
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Errore sconosciuto") }
            } finally {
                _state.update { it.copy(refreshing = false) }
            }
        }
    }

    fun setSearchText(text: String) {
        _state.update { it.copy(searchText = text) }
    }

    fun setSelectedSector(sector: String?) {
        _state.update { it.copy(selectedSector = sector) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
