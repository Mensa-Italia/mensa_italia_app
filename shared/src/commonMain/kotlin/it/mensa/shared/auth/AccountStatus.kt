package it.mensa.shared.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Se il server ha sospeso l'account (livello `blocked`).
 *
 * Lo scrive [it.mensa.shared.integrity.IntegrityGuard] appena una risposta
 * qualunque a `svc.mensa.it` e' un 403 `account_blocked`, e lo spegne alla prima
 * risposta di successo a una richiesta autenticata. Va in un oggetto globale, come
 * [it.mensa.shared.net.ServerReachability], perche' chi lo scopre e' il client
 * HTTP, che non conosce [AuthRepository] (il repository dipende dal client, non il
 * contrario).
 *
 * E' uno stato a parte e non un [AuthState]: l'utente e' autenticato per il
 * server, che si limita a rifiutarlo. Trattarlo come `Anonymous` farebbe
 * scattare un logout e, con le credenziali salvate, un nuovo login che torna
 * subito sospeso: un ciclo.
 */
object AccountStatus {
    private val _suspended = MutableStateFlow(false)
    val suspended: StateFlow<Boolean> = _suspended.asStateFlow()

    internal fun markSuspended() {
        _suspended.value = true
    }

    internal fun clear() {
        if (_suspended.value) _suspended.value = false
    }
}
