package it.mensa.shared.net

import io.ktor.client.plugins.ResponseException

/**
 * Il testo per una risposta d'errore: la chiave i18n, il testo da usare se
 * manca, e lo stato. Chi lo mostra ci aggiunge " (HTTP <stato>)": in uno
 * screenshot dice gia' cosa e' successo.
 */
data class HttpErrorText(val status: Int, val key: String, val fallback: String)

/**
 * Cosa dire di una risposta non 2xx del server. Una sola tabella per Android e
 * iOS: il messaggio di una `ResponseException` e' "Client request(GET
 * https://svc.mensa.it/…) invalid: 404 Not Found. Text: …", da log e non da
 * schermata.
 */
object HttpErrors {

    /**
     * Lo stato HTTP, se [t] e' una risposta d'errore che nessuno ha tradotto.
     * Solo [t] e non le sue cause: un'eccezione che ne avvolge una HTTP
     * (`StampClaimRejectedException`, `SessionLostException`) ha gia' il suo
     * messaggio, e deve restare quello.
     */
    fun status(t: Throwable): Int? = (t as? ResponseException)?.response?.status?.value

    /** Il testo per [t], o null se non e' una risposta d'errore. */
    fun text(t: Throwable): HttpErrorText? = status(t)?.let(::textFor)

    internal fun textFor(status: Int): HttpErrorText = when {
        status == 401 -> HttpErrorText(status, "app.error.http.session", "La sessione non è più valida. Esci e accedi di nuovo.")
        status == 403 -> HttpErrorText(status, "app.error.http.forbidden", "Non hai accesso a questo contenuto.")
        status == 404 -> HttpErrorText(status, "app.error.http.not_found", "Non trovato: potrebbe essere stato rimosso.")
        status == 429 -> HttpErrorText(status, "app.error.http.too_many", "Troppe richieste. Riprova fra qualche minuto.")
        status >= 500 -> HttpErrorText(status, "app.error.http.server", "Il servizio non risponde in questo momento. Riprova fra poco.")
        else -> HttpErrorText(status, "app.error.http.rejected", "Il server non ha accettato la richiesta. Riprova.")
    }
}
