package it.mensa.app.features.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.LocaleManager
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class ProfileUiState(
    val user: UserModel? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val loggingOut: Boolean = false,
    val showLogoutDialog: Boolean = false,
    val errorMessage: String? = null,
    val localeName: String = "",
) {
    val fullName: String get() = user?.let {
        it.name.ifEmpty { it.username }.ifEmpty { "Socio Mensa" }
    } ?: "Socio Mensa"

    val email: String get() = user?.email.orEmpty()

    val initials: String get() = user?.let {
        val parts = it.name.ifEmpty { it.username }.split(" ").filter { p -> p.isNotEmpty() }
        parts.take(2).joinToString("") { p -> p.first().uppercase() }
    } ?: "?"

    val membershipCode: String? get() = user?.username?.ifEmpty { null }
}

class ProfileViewModel(
    private val localeManager: LocaleManager,
) : ViewModel() {

    private val auth = koinAccess().auth

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        observeUser()
        observeLocale()
    }

    private fun observeUser() {
        viewModelScope.launch {
            auth.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    private fun observeLocale() {
        viewModelScope.launch {
            localeManager.currentLocale.collect { tag ->
                val name = if (tag != null) {
                    runCatching {
                        Locale.forLanguageTag(tag)
                            .getDisplayName(Locale.getDefault())
                            .replaceFirstChar { it.uppercase() }
                    }.getOrDefault(tag)
                } else {
                    "Sistema"
                }
                _uiState.update { it.copy(localeName = name) }
            }
        }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
    }

    fun onNotificationsToggle(enabled: Boolean) {
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun onLogoutRequest() {
        _uiState.update { it.copy(showLogoutDialog = true) }
    }

    fun onLogoutDismiss() {
        _uiState.update { it.copy(showLogoutDialog = false) }
    }

    fun onLogoutConfirm() {
        viewModelScope.launch {
            _uiState.update { it.copy(showLogoutDialog = false, loggingOut = true) }
            runCatching { auth.logout() }.onFailure { e ->
                _uiState.update {
                    it.copy(loggingOut = false, errorMessage = e.message)
                }
                Logger.w("ProfileVM", "logout", "failed: ${e.message}")
                return@launch
            }
            _uiState.update { it.copy(loggingOut = false) }
        }
    }

    fun onErrorDismiss() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
