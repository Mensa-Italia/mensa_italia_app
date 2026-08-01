package it.mensa.app.features.profile.sub

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.features.auth.PasskeyAuthenticator
import it.mensa.app.features.auth.PasskeyPromptResult
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import it.mensa.shared.auth.passkey.PasskeyModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PasskeysUiState(
    val passkeys: List<PasskeyModel> = emptyList(),
    val loading: Boolean = true,
    val adding: Boolean = false,
    val supported: Boolean = PasskeyAuthenticator.isSupported,
    val message: String? = null,
)

/**
 * Gestione delle passkey dell'utente: elenco, attivazione su questo
 * dispositivo, revoca.
 *
 * E' anche l'unica strada per chi ha rifiutato la proposta automatica dopo il
 * login, e per chi ha cambiato telefono.
 */
class PasskeysViewModel : ViewModel() {

    private val passkeys = koinAccess().passkeys

    private val _uiState = MutableStateFlow(PasskeysUiState())
    val uiState: StateFlow<PasskeysUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            val list = passkeys.list()
            _uiState.update { it.copy(passkeys = list, loading = false) }
        }
    }

    /**
     * Aggiunge una passkey su questo dispositivo.
     *
     * Il duplicato non lo controlliamo noi: le creation options arrivano con
     * `excludeCredentials` popolato da Zitadel, quindi se una passkey per questo
     * account esiste gia' qui e' l'authenticator a rifiutare, e arriva come
     * [PasskeyPromptResult.AlreadyRegistered].
     */
    fun add(activityContext: Context) {
        if (_uiState.value.adding) return

        viewModelScope.launch {
            _uiState.update { it.copy(adding = true, message = null) }

            val challenge = passkeys.beginRegistration()
            if (challenge == null) {
                settle("Non riesco ad avviare l'attivazione. Riprova piu' tardi.")
                return@launch
            }

            when (val prompt = PasskeyAuthenticator(activityContext).create(challenge.creationOptionsJson)) {
                is PasskeyPromptResult.Success -> {
                    val registered = passkeys.finishRegistration(
                        passkeyId = challenge.passkeyId,
                        credentialJson = prompt.credentialJson,
                        name = PasskeyAuthenticator.deviceLabel(),
                    )
                    if (registered) {
                        _uiState.update { it.copy(adding = false) }
                        refresh()
                    } else {
                        Logger.e("PasskeysVM", "finish", "registrazione rifiutata dal server")
                        settle("Attivazione non completata. Riprova.")
                    }
                }

                // Chiusura del prompt: scelta dell'utente, nessun messaggio.
                PasskeyPromptResult.Cancelled -> _uiState.update { it.copy(adding = false) }

                PasskeyPromptResult.AlreadyRegistered ->
                    settle("Su questo dispositivo la passkey e' gia' attiva.")

                PasskeyPromptResult.Unsupported ->
                    settle("Questo dispositivo non supporta le passkey.")

                PasskeyPromptResult.NoCredential ->
                    settle("Attivazione non completata. Riprova.")

                is PasskeyPromptResult.Failed -> {
                    Logger.e("PasskeysVM", "prompt", "${prompt.message}")
                    settle("Attivazione non completata. Riprova.")
                }
            }
        }
    }

    /**
     * Revoca una passkey. Nessuna cautela particolare sull'ultima rimasta: la
     * password resta sempre valida, quindi non si puo' restare chiusi fuori.
     */
    fun delete(passkeyId: String) {
        viewModelScope.launch {
            if (!passkeys.remove(passkeyId)) {
                settle("Non riesco a rimuovere la passkey. Riprova.")
                return@launch
            }
            refresh()
        }
    }

    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    private fun settle(message: String) {
        _uiState.update { it.copy(adding = false, message = message) }
    }
}
