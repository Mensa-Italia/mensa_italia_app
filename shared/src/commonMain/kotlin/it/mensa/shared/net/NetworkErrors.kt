package it.mensa.shared.net

import kotlinx.io.IOException

/**
 * Riconosce un errore di rete "non c'e' connessione": niente rete, server
 * irraggiungibile, timeout.
 *
 * Serve alle due UI per non trattarlo come un guasto. Offline, l'app mostra
 * i dati che ha in database; prima ogni schermata che provava ad aggiornarsi
 * apriva un alert con l'eccezione intera ("Exception in http request: Error
 * Domain=NSURLErrorDomain Code=-1009…").
 *
 * Basta [IOException]: e' la base di tutti gli errori di trasporto di Ktor su
 * entrambe le piattaforme (su Android `UnknownHostException`,
 * `ConnectException`, `SocketTimeoutException`; su iOS
 * `DarwinHttpRequestException`; i timeout del plugin). Gli errori del server
 * (4xx/5xx) e quelli di decodifica non lo sono, e restano errori veri.
 */
object NetworkErrors {
    fun isUnavailable(t: Throwable): Boolean {
        var current: Throwable? = t
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            if (current is IOException) return true
            current = current.cause
            depth++
        }
        return false
    }

    private const val MAX_CAUSE_DEPTH = 8
}
