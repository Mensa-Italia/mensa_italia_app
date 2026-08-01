package it.mensa.app.features.profile.sub

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.AppFormat
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

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

    /** [locale] is passed in from the composable so the row re-renders on a language switch. */
    fun expiryString(user: UserModel?, locale: java.util.Locale = AppFormat.locale()): String {
        val d = expiryDate(user) ?: return "—"
        return AppFormat.format(d, AppFormat.Skeleton.DAY_MONTH_LONG_YEAR, locale)
    }

    /**
     * Countdown to expiry, in the in-app language. Mirrors the iOS
     * `RenewMembershipView.countdownString` key-for-key — this used to build
     * the sentence from Italian literals, so it stayed Italian in all 11
     * languages.
     */
    fun countdownString(user: UserModel?): String {
        val i18n = koinAccess().i18n
        val d = expiryDate(user) ?: return "—"
        val interval = d.time - System.currentTimeMillis()
        if (interval <= 0) return i18n.t("app.renew.expired_already", "Scaduta")
        val days = (interval / 86400000).toInt()
        return when {
            days > 30 -> i18n.t("app.renew.days_left", "{days} giorni", mapOf("days" to "$days"))
            days > 0 -> i18n.t(
                "app.renew.days_warning",
                "{days} giorni: rinnova presto",
                mapOf("days" to "$days"),
            )
            else -> {
                val hours = (interval / 3600000).toInt()
                i18n.t("app.renew.hours_left", "{hours} ore", mapOf("hours" to "$hours"))
            }
        }
    }
}
