package it.mensa.app.support

import it.mensa.shared.api.endpoints.ExportRequestGoneException
import it.mensa.shared.integrity.AccountBlockedException
import it.mensa.shared.integrity.IntegrityException
import it.mensa.shared.integrity.IntegrityFailedException
import it.mensa.shared.integrity.IntegrityUnavailableException
import it.mensa.shared.integrity.IntegrityUnsupportedException
import it.mensa.shared.net.HttpErrors
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
    get() = HttpErrors.text(this).let { http ->
        when {
            isOffline ->
                koinAccess().i18n.t("app.error.offline", "Sei offline. Riprova quando torna la connessione.")
            this is ExportRequestGoneException ->
                koinAccess().i18n.t("ex_app.export.error.gone", "La richiesta è scaduta o è già stata gestita. Chiedi di nuovo all'app esterna.")
            this is AccountBlockedException ->
                koinAccess().i18n.t("app.account.suspended.title", "Account sospeso")
            // Il tag (`attest_key/2`) porta con se' la causa: uno screenshot di un socio
            // la mostra anche se il report al server non fosse mai arrivato.
            this is IntegrityUnsupportedException || this is IntegrityFailedException ->
                koinAccess().i18n.t(
                    "app.error.integrity",
                    "Questo dispositivo non ha superato la verifica di sicurezza dell'app. " +
                        "Usa l'app ufficiale su un dispositivo non modificato.",
                ) + integrityTagSuffix()
            this is IntegrityException ->
                koinAccess().i18n.t("app.error.integrity_retry", "Verifica di sicurezza non riuscita. Riprova fra poco.") +
                    integrityTagSuffix()
            // Una risposta non 2xx: il messaggio di Ktor e' la richiesta intera.
            http != null -> koinAccess().i18n.t(http.key, http.fallback) + " (HTTP ${http.status})"
            else -> message ?: koinAccess().i18n.t("app.error.generic", "Qualcosa e' andato storto. Riprova.")
        }
    }

/**
 * " (attest_key/2)" se l'errore di integrita' conosce la sua causa, altrimenti
 * niente. Non scrive nel log: una schermata rilegge `userMessage` a ogni
 * ricomposizione. La riga di log la scrive una volta sola `IntegrityGuard`, dove
 * il fallimento nasce.
 */
private fun Throwable.integrityTagSuffix(): String {
    val failure = (this as? IntegrityUnavailableException)?.failure
        ?: (this as? IntegrityUnsupportedException)?.failure
    val tag = failure?.tag ?: return ""
    return " ($tag)"
}

/**
 * Per l'aggiornamento di dati gia' a schermo: offline non c'e' niente da
 * segnalare, restano quelli salvati.
 */
val Throwable.refreshMessage: String? get() = if (isOffline) null else userMessage
