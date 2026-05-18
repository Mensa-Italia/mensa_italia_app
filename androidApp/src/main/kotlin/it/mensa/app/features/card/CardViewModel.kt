package it.mensa.app.features.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.services.wallet.WalletService
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.UserModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

enum class CardStatus { Active, ExpiringSoon, Expired }

data class CardUiState(
    val user: UserModel? = null,
    val membershipCode: String? = null,
    val expirationDate: String? = null,
    val cardStatus: CardStatus = CardStatus.Active,
    val qrPayload: String? = null,
    val loading: Boolean = true,
    val walletLoading: Boolean = false,
    val snackbarMessage: String? = null,
)

class CardViewModel : ViewModel() {

    private val auth = koinAccess().auth
    private val walletService: WalletService by lazy {
        org.koin.mp.KoinPlatform.getKoin().get()
    }

    private val _uiState = MutableStateFlow(CardUiState())
    val uiState: StateFlow<CardUiState> = _uiState.asStateFlow()

    init {
        auth.currentUser
            .onEach { user -> _uiState.update { buildState(it, user) } }
            .launchIn(viewModelScope)
    }

    private fun buildState(prev: CardUiState, user: UserModel?): CardUiState {
        if (user == null) return CardUiState(loading = false)

        val expiry = formatExpiry(user)
        val status = computeStatus(user)
        val qr = buildQrPayload(user, expiry)
        return prev.copy(
            user = user,
            membershipCode = user.id,
            expirationDate = expiry,
            cardStatus = status,
            qrPayload = qr,
            loading = false,
        )
    }

    private fun formatExpiry(user: UserModel): String {
        return try {
            val local = user.expireMembership.toLocalDateTime(TimeZone.currentSystemDefault())
            val day = local.dayOfMonth.toString().padStart(2, '0')
            val month = local.monthNumber.toString().padStart(2, '0')
            "${day}/${month}/${local.year}"
        } catch (_: Exception) {
            "—"
        }
    }

    private fun computeStatus(user: UserModel): CardStatus {
        val now = Clock.System.now()
        val exp = user.expireMembership
        val thirtyDaysMs = 30L * 24 * 60 * 60 * 1000
        return when {
            exp.toEpochMilliseconds() < now.toEpochMilliseconds() -> CardStatus.Expired
            exp.toEpochMilliseconds() - now.toEpochMilliseconds() < thirtyDaysMs -> CardStatus.ExpiringSoon
            else -> CardStatus.Active
        }
    }

    private fun buildQrPayload(user: UserModel, expiry: String): String {
        val name = if (user.name.isNotBlank()) user.name else user.username
        return "MENSA-IT|id:${user.id}|user:${user.username}|exp:$expiry"
    }

    fun refresh() {
        // currentUser StateFlow auto-updates; trigger a re-emit by marking loading
        _uiState.update { it.copy(loading = true) }
        // The flow emission from auth.currentUser will reset loading once it emits
        _uiState.update { buildState(it, it.user) }
    }

    fun onAddToWalletClick() {
        val user = _uiState.value.user ?: return
        _uiState.update { it.copy(walletLoading = true) }
        walletService.checkAvailability { available ->
            if (!available) {
                _uiState.update {
                    it.copy(
                        walletLoading = false,
                        snackbarMessage = "Google Wallet non disponibile su questo dispositivo",
                    )
                }
                return@checkAvailability
            }
            // In a real implementation we'd get a signed JWT from backend
            walletService.addMembershipCard(
                signedJwt = "",
                onSuccess = {
                    _uiState.update { it.copy(walletLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            walletLoading = false,
                            snackbarMessage = "Setup Google Wallet richiesto",
                        )
                    }
                },
            )
        }
    }

    fun onRenewClick() {
        // TODO: wire to RenewMembershipScreen navigation
        // For now, show snackbar as placeholder
        _uiState.update { it.copy(snackbarMessage = "Rinnovo tessera — prossimamente") }
    }

    fun onSnackbarDismissed() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
