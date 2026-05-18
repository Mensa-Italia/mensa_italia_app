package it.mensa.app.features.deals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.DealsContactModel
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ─── UI state ────────────────────────────────────────────────────────────────

data class DealDetailUiState(
    val deal: DealModel? = null,
    val contacts: List<DealsContactModel> = emptyList(),
    val loading: Boolean = false,
    val loadingContacts: Boolean = false,
    val error: String? = null,
    val currentUser: UserModel? = null,
) {
    val canEdit: Boolean
        get() {
            val user = currentUser ?: return false
            // Mirrors Flutter/web: the `deals` power is what the backend
            // grants to convention managers. `super`/`admin`/`deals_admin`
            // are kept as aliases for legacy/server-admin accounts.
            val allowed = setOf("super", "admin", "deals", "deals_admin")
            return user.powers.any { it in allowed }
        }
}

// ─── ViewModel ───────────────────────────────────────────────────────────────

class DealDetailViewModel(private val dealId: String) : ViewModel() {

    private val koin = koinAccess()

    private val _state = MutableStateFlow(DealDetailUiState())
    val state: StateFlow<DealDetailUiState> = _state.asStateFlow()

    init {
        // Observe all deals from cache, pick the matching deal reactively
        viewModelScope.launch {
            koin.deals.observeAll().collect { deals ->
                val match = deals.firstOrNull { it.id == dealId }
                if (match != null) {
                    _state.update { it.copy(deal = match) }
                }
            }
        }
        // Observe current user for edit permission
        viewModelScope.launch {
            koin.auth.currentUser.collect { user ->
                _state.update { it.copy(currentUser = user) }
            }
        }
        // Load deal if cache empty + load contacts
        viewModelScope.launch { loadIfNeeded() }
        viewModelScope.launch { loadContacts() }
    }

    private suspend fun loadIfNeeded() {
        if (_state.value.deal != null) return
        _state.update { it.copy(loading = true) }
        try {
            val deal = koin.deals.getById(dealId)
            _state.update { it.copy(deal = deal, loading = false) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message, loading = false) }
        }
    }

    private suspend fun loadContacts() {
        _state.update { it.copy(loadingContacts = true) }
        try {
            val contacts = koin.deals.contacts(dealId)
            _state.update { it.copy(contacts = contacts, loadingContacts = false) }
        } catch (e: Exception) {
            _state.update { it.copy(loadingContacts = false) }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
