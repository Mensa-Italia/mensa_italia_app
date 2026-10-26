package it.mensa.app.support

import it.mensa.shared.net.NetworkErrors

/**
 * Cosa dire all'utente di un errore. Mirrors iOS `Error.userMessage`.
 *
 * Le schermate mostravano `e.message`, che offline e' "Unable to resolve host
 * "svc.mensa.it": No address associated with hostname" — e lo mostravano anche
 * quando avevano gia' i dati in database da far vedere.
 */
val Throwable.isOffline: Boolean get() = NetworkErrors.isUnavailable(this)

/** Leggibile se si e' offline, il messaggio dell'errore altrimenti. */
val Throwable.userMessage: String
    get() = if (isOffline) {
        koinAccess().i18n.t("app.error.offline", "Sei offline. Riprova quando torna la connessione.")
    } else {
        message ?: koinAccess().i18n.t("app.error.generic", "Qualcosa e' andato storto. Riprova.")
    }

/**
 * Per l'aggiornamento di dati gia' a schermo: offline non c'e' niente da
 * segnalare, restano quelli salvati.
 */
val Throwable.refreshMessage: String? get() = if (isOffline) null else userMessage
