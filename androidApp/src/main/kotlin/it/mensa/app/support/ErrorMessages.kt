package it.mensa.app.support

import it.mensa.shared.api.endpoints.ExportRequestGoneException
import it.mensa.shared.integrity.AccountBlockedException
import it.mensa.shared.integrity.IntegrityException
import it.mensa.shared.integrity.IntegrityFailedException
import it.mensa.shared.integrity.IntegrityUnsupportedException
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
    get() = when {
        isOffline ->
            koinAccess().i18n.t("app.error.offline", "Sei offline. Riprova quando torna la connessione.")
        this is ExportRequestGoneException ->
            koinAccess().i18n.t("ex_app.export.error.gone", "La richiesta è scaduta o è già stata gestita. Chiedi di nuovo all'app esterna.")
        this is AccountBlockedException ->
            koinAccess().i18n.t("app.account.suspended.title", "Account sospeso")
        this is IntegrityUnsupportedException || this is IntegrityFailedException ->
            koinAccess().i18n.t(
                "app.error.integrity",
                "Questo dispositivo non ha superato la verifica di sicurezza dell'app. " +
                    "Usa l'app ufficiale su un dispositivo non modificato.",
            )
        this is IntegrityException ->
            koinAccess().i18n.t("app.error.integrity_retry", "Verifica di sicurezza non riuscita. Riprova fra poco.")
        else -> message ?: koinAccess().i18n.t("app.error.generic", "Qualcosa e' andato storto. Riprova.")
    }

/**
 * Per l'aggiornamento di dati gia' a schermo: offline non c'e' niente da
 * segnalare, restano quelli salvati.
 */
val Throwable.refreshMessage: String? get() = if (isOffline) null else userMessage
