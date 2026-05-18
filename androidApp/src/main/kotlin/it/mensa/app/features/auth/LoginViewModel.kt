package it.mensa.app.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
)

class LoginViewModel : ViewModel() {

    private val auth = koinAccess().auth

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return
        if (state.loading) return

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null) }
            val result = runCatching {
                auth.login(state.email.trim(), state.password)
            }
            result.fold(
                onSuccess = { loginResult ->
                    loginResult.fold(
                        onSuccess = {
                            Logger.i("LoginVM", "login", "successful")
                            // Auth state flow in RootViewModel will trigger phase → Main/Onboarding
                        },
                        onFailure = { err ->
                            Logger.e("LoginVM", "login", "failed: ${err.message}")
                            _uiState.update {
                                it.copy(
                                    loading = false,
                                    error = err.message ?: "Errore di accesso",
                                )
                            }
                        },
                    )
                },
                onFailure = { err ->
                    Logger.e("LoginVM", "login", "exception: ${err.message}")
                    _uiState.update {
                        it.copy(
                            loading = false,
                            error = err.message ?: "Errore di rete",
                        )
                    }
                },
            )
        }
    }
}
