package it.mensa.app.features.addonshub

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.AddonModel
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddonsHubUiState(
    val addons: List<AddonModel> = emptyList(),
    val currentUser: UserModel? = null,
    val loading: Boolean = true,
    val error: String? = null,
)

class AddonsHubViewModel : ViewModel() {

    private val repo = koinAccess().addons
    private val auth = koinAccess().auth

    private val _uiState = MutableStateFlow(AddonsHubUiState())
    val uiState: StateFlow<AddonsHubUiState> = _uiState.asStateFlow()

    init {
        repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(addons = list, loading = false, error = null) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        auth.currentUser
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
            .launchIn(viewModelScope)

        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            runCatching { repo.refresh() }
                .onFailure { e -> _uiState.update { it.copy(loading = false, error = e.message) } }
        }
    }

    /**
     * Visible addons filtered by user power/addons list.
     * Mirrors iOS AddonsHubView.userCanSeeAddon logic:
     * - No requiredPower (0): visible to all authenticated users
     * - requiredPower > 0: visible only if user.powers contains the addon's requiredPower or user.addons contains addon.id
     */
    fun visibleAddons(): List<AddonModel> {
        val user = _uiState.value.currentUser ?: return emptyList()
        return _uiState.value.addons.filter { addon ->
            addon.requiredPower == 0 ||
                user.addons.contains(addon.id) ||
                user.powers.any { it == addon.requiredPower.toString() }
        }
    }

    /** CTA handler for addon tiles — navigates to the addon's destination. */
    fun onAddonClick(addonId: String) {
        Log.d("AddonsHub", "Addon clicked: $addonId")
        // Navigation is handled by the screen via the callback lambda.
    }
}
