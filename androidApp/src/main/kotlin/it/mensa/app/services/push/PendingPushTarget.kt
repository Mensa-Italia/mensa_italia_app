package it.mensa.app.services.push

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiene da parte il bersaglio di una push toccata, finche' non c'e' un
 * NavController a cui darlo.
 *
 * E' il pezzo che mancava ad Android. Il tap su una notifica apre
 * [it.mensa.app.MainActivity] con il payload FCM negli extra dell'Intent, ma in
 * quel momento il NavController non esiste ancora: al cold-launch la UI e'
 * ferma sullo splash mentre auth e onboarding decidono la fase, e navigare
 * prima che il NavHost sia composto non porta da nessuna parte. Senza uno slot
 * dove parcheggiare il bersaglio, il tap finiva per aprire l'app e basta.
 *
 * Stessa semantica di `PendingDeepLink` su iOS: MainActivity riempie, il guscio
 * [it.mensa.app.ui.shell.MainAppShell] drena appena e' montato.
 *
 * Il valore e' uno solo: se arrivano due push prima che l'app sia pronta,
 * l'ultima toccata vince. E' anche il comportamento del sistema, che apre una
 * Activity sola.
 */
object PendingPushTarget {

    private val _target = MutableStateFlow<PushDeepLinkRouter.NotificationTarget?>(null)

    /** Osservabile dal guscio, cosi' una push che arriva ad app aperta si vede subito. */
    val target: StateFlow<PushDeepLinkRouter.NotificationTarget?> = _target.asStateFlow()

    fun set(target: PushDeepLinkRouter.NotificationTarget) {
        _target.value = target
    }

    /** Restituisce il bersaglio e svuota lo slot, cosi' non si naviga due volte. */
    fun consume(): PushDeepLinkRouter.NotificationTarget? {
        val current = _target.value
        _target.value = null
        return current
    }
}
