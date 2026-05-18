package it.mensa.app.features.testassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class TestAssistantUiState(
    val currentUser: UserModel? = null,
    val loading: Boolean = true,
)

class TestAssistantViewModel : ViewModel() {

    private val auth = koinAccess().auth

    private val _uiState = MutableStateFlow(TestAssistantUiState())
    val uiState: StateFlow<TestAssistantUiState> = _uiState.asStateFlow()

    /** Platform URL for the testelab admin panel — mirrors iOS. */
    val platformUrl = "https://www.cloud32.it/Associazioni/utenti/testelab"

    init {
        auth.currentUser
            .onEach { user ->
                _uiState.update { it.copy(currentUser = user, loading = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * Returns true when the current user has "testmakers" power.
     * Mirrors iOS `hasPower("testmakers", user: user)`.
     */
    fun hasTestmakersPower(): Boolean {
        return _uiState.value.currentUser?.powers?.contains("testmakers") == true
    }
}
