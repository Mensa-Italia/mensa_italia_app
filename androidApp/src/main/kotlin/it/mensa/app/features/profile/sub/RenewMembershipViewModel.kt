package it.mensa.app.features.profile.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RenewMembershipUiState(
    val user: UserModel? = null,
)

class RenewMembershipViewModel : ViewModel() {

    private val auth = koinAccess().auth

    private val _uiState = MutableStateFlow(RenewMembershipUiState())
    val uiState: StateFlow<RenewMembershipUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            auth.currentUser.collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    fun expiryDate(user: UserModel?): java.util.Date? {
        if (user == null || user.expireMembership.epochSeconds <= 0) return null
        return java.util.Date(user.expireMembership.toEpochMilliseconds())
    }

    fun isExpired(user: UserModel?): Boolean {
        val d = expiryDate(user) ?: return true
        return d.before(java.util.Date())
    }

    fun expiryString(user: UserModel?): String {
        val d = expiryDate(user) ?: return "—"
        val fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN)
            .withZone(ZoneId.systemDefault())
        return fmt.format(d.toInstant())
    }

    fun countdownString(user: UserModel?): String {
        val d = expiryDate(user) ?: return "—"
        val interval = d.time - System.currentTimeMillis()
        if (interval <= 0) return "Scaduta"
        val days = (interval / 86400000).toInt()
        return when {
            days > 30 -> "$days giorni"
            days > 0 -> "$days giorni — rinnova presto"
            else -> {
                val hours = (interval / 3600000).toInt()
                "$hours ore"
            }
        }
    }
}
