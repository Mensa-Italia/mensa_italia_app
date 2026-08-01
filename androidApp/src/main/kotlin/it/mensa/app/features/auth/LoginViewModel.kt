package it.mensa.app.features.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import it.mensa.shared.auth.passkey.PasskeyLoginStatus
import it.mensa.shared.auth.passkey.PasskeyStartStatus
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
    /** Accesso con passkey in corso: bottone e spinner separati da quelli password. */
    val passkeyLoading: Boolean = false,
    /**
     * Nota **neutra**, non un errore: il percorso passkey non deve mai colorare
     * di rosso la schermata. Chi non ha una passkey su questo dispositivo non ha
     * sbagliato niente, gli si dice solo di usare la password.
     */
    val passkeyNotice: String? = null,
    /** False sotto API 28: il bottone passkey non va nemmeno mostrato. */
    val passkeySupported: Boolean = PasskeyAuthenticator.isSupported,
)

class LoginViewModel : ViewModel() {

    private val auth = koinAccess().auth
    private val passkeys = koinAccess().passkeys

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value, error = null, passkeyNotice = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onLoginClick() {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) return
        if (state.loading || state.passkeyLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(loading = true, error = null, passkeyNotice = null) }
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

    /**
     * Accesso con passkey. Serve solo l'email, perche' la Session API di Zitadel
     * pretende il check utente prima di poter creare la challenge WebAuthn:
     * il login usernameless non e' disponibile.
     *
     * [activityContext] deve essere il Context dell'Activity: Credential Manager
     * ci mostra sopra il prompt di sistema.
     *
     * Ogni esito negativo riporta alla password senza errori rossi.
     */
    fun onPasskeyClick(activityContext: Context) {
        val state = _uiState.value
        val email = state.email.trim()
        if (email.isBlank() || state.loading || state.passkeyLoading) return

        viewModelScope.launch {
            _uiState.update { it.copy(passkeyLoading = true, error = null, passkeyNotice = null) }
            val authenticator = PasskeyAuthenticator(activityContext)

            // Un solo ritentativo, e solo per challenge scaduta: vuol dire che
            // l'utente ha esitato davanti al prompt, non che qualcosa e' rotto.
            repeat(MAX_PASSKEY_ATTEMPTS) { attempt ->
                val start = passkeys.beginLogin(email)
                val challenge = start.challenge
                if (start.status != PasskeyStartStatus.READY || challenge == null) {
                    settle(noticeForStart(start.status))
                    return@launch
                }

                when (val prompt = authenticator.assert(challenge.requestOptionsJson)) {
                    is PasskeyPromptResult.Success -> {
                        val result = passkeys.finishLogin(challenge.loginId, prompt.credentialJson)
                        when (result.status) {
                            PasskeyLoginStatus.SUCCESS -> {
                                Logger.i("LoginVM", "passkey", "successful")
                                // Come per la password: RootViewModel reagisce
                                // al cambio di authState e cambia fase.
                                _uiState.update { it.copy(passkeyLoading = false) }
                                return@launch
                            }

                            PasskeyLoginStatus.CHALLENGE_EXPIRED -> {
                                if (attempt == 0) return@repeat
                                settle(NOTICE_FAILED)
                                return@launch
                            }

                            else -> {
                                Logger.e("LoginVM", "passkey", "finish: ${result.status}")
                                settle(noticeForFinish(result.status))
                                return@launch
                            }
                        }
                    }

                    // L'utente ha chiuso il prompt: nessun messaggio, e' una scelta.
                    PasskeyPromptResult.Cancelled -> {
                        _uiState.update { it.copy(passkeyLoading = false) }
                        return@launch
                    }

                    PasskeyPromptResult.NoCredential -> {
                        settle("Nessuna passkey su questo dispositivo. Accedi con la password.")
                        return@launch
                    }

                    PasskeyPromptResult.Unsupported -> {
                        settle("Questo dispositivo non supporta le passkey. Accedi con la password.")
                        return@launch
                    }

                    // Non dovrebbe capitare in login (excludeCredentials vale solo
                    // in registrazione), ma se capita non va trattato come errore.
                    PasskeyPromptResult.AlreadyRegistered -> {
                        settle(NOTICE_FAILED)
                        return@launch
                    }

                    is PasskeyPromptResult.Failed -> {
                        Logger.e("LoginVM", "passkey", "prompt: ${prompt.message}")
                        settle(NOTICE_FAILED)
                        return@launch
                    }
                }
            }
        }
    }

    fun dismissPasskeyNotice() {
        _uiState.update { it.copy(passkeyNotice = null) }
    }

    private fun settle(notice: String) {
        _uiState.update { it.copy(passkeyLoading = false, passkeyNotice = notice) }
    }

    private fun noticeForStart(status: PasskeyStartStatus): String = when (status) {
        PasskeyStartStatus.NETWORK_ERROR -> NOTICE_OFFLINE
        // UNAVAILABLE copre "email sconosciuta" e "nessuna passkey": il server li
        // rende indistinguibili di proposito, per non dire a chi chiede quali
        // email sono socie.
        else -> "Nessuna passkey attiva per questa email. Accedi con la password."
    }

    private fun noticeForFinish(status: PasskeyLoginStatus): String = when (status) {
        PasskeyLoginStatus.NETWORK_ERROR -> NOTICE_OFFLINE
        else -> NOTICE_FAILED
    }

    private companion object {
        const val MAX_PASSKEY_ATTEMPTS = 2
        const val NOTICE_OFFLINE = "Non riesco a contattare il server. Accedi con la password."
        const val NOTICE_FAILED = "Non riesco a usare la passkey. Accedi con la password."
    }
}
