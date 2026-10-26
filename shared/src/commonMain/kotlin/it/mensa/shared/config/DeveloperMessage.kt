package it.mensa.shared.config

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Un messaggio degli sviluppatori ai soci, in markdown, dalla chiave
 * `developer_message` della collection `configs`.
 *
 * Si mostra all'apertura finche' non e' stato letto tutto, e ricompare ogni
 * volta che il testo cambia rispetto all'ultimo letto. Conta solo l'ultimo,
 * non lo storico: "problema X", poi "problema Y", poi di nuovo "problema X"
 * compaiono tutti e tre, perche' rimettere un testo vecchio e' un messaggio
 * nuovo per chi legge. Per spegnerlo basta svuotare la chiave, e anche il
 * vuoto conta come un messaggio arrivato: X, vuoto, di nuovo X fa comparire X
 * due volte.
 *
 * L'ultimo valore arrivato dal server resta salvato: chi apre l'app offline
 * vede comunque un messaggio che non ha ancora letto.
 */
class DeveloperMessage internal constructor(
    private val settings: SettingsApi,
    private val db: MensaDatabase,
) {
    /**
     * Il messaggio da mostrare adesso, o null se non c'e' o e' gia' stato letto.
     *
     * Sempre dal server, non da `SettingsApi.configs`: quello si scarica una
     * volta per sessione, e un messaggio cambiato con l'app gia' aperta non
     * si sarebbe visto finche' non la si chiudeva del tutto. Le UI lo chiamano
     * all'apertura e a ogni ritorno in primo piano.
     */
    @Throws(Exception::class)
    suspend fun pending(): String? {
        val message = runCatching { settings.settings()[KEY] }
            .onSuccess { fresh ->
                store(KEY_LATEST, fresh?.trim().orEmpty())
                // Svuotato dal server: non si mostra niente, ma e' come se
                // fosse arrivato e letto, cosi' un X rimesso dopo ricompare.
                // Solo con la risposta in mano: offline non si sa.
                if (fresh.isNullOrBlank()) store(KEY_SEEN, EMPTY_SEEN)
            }
            .getOrElse { read(KEY_LATEST) }
        return pendingOf(message, lastSeen())
    }

    /**
     * Toccata una notifica `developer_message`: la scheda va mostrata, anche
     * se il messaggio e' gia' stato letto. E' uno stato e non un evento
     * perche' la notifica puo' aprire l'app da chiusa, prima che la scheda
     * esista: la richiesta aspetta finche' qualcuno non la prende con
     * [consumeShowRequest].
     */
    private val _showRequested = MutableStateFlow(false)
    val showRequested: StateFlow<Boolean> = _showRequested.asStateFlow()

    fun requestShow() {
        _showRequested.value = true
    }

    fun consumeShowRequest() {
        _showRequested.value = false
    }

    /**
     * Il messaggio di adesso, letto o no: quello da rimostrare al tocco della
     * notifica. Null se la chiave e' vuota. Dal server, o dall'ultimo
     * arrivato se offline.
     */
    @Throws(Exception::class)
    suspend fun current(): String? {
        val message = runCatching { settings.settings()[KEY] }.getOrElse { read(KEY_LATEST) }
        return message?.trim()?.takeIf { it.isNotEmpty() }
    }

    /** Letto fino in fondo: non ricompare finche' il testo non cambia. */
    @Throws(Exception::class)
    suspend fun markSeen(message: String) {
        store(KEY_SEEN, fingerprint(message))
    }

    /**
     * L'impronta dell'ultimo messaggio letto. Le versioni di prova salvavano
     * un elenco separato da virgole, ultimo in fondo: di quello conta l'ultimo.
     */
    private suspend fun lastSeen(): String? =
        read(KEY_SEEN)?.split(',')?.lastOrNull { it.isNotBlank() }

    private suspend fun read(key: String): String? =
        runCatching { db.keyValueQueries.selectById(key).awaitAsOneOrNull()?.value_ }.getOrNull()

    private suspend fun store(key: String, value: String) {
        runCatching { db.keyValueQueries.insertOrReplace(key = key, value_ = value) }
    }

    companion object {
        const val KEY = "developer_message"
        private const val KEY_LATEST = "developer_message.latest"
        private const val KEY_SEEN = "developer_message.seen"

        /** L'ultimo "letto" quando il server ha svuotato la chiave. */
        val EMPTY_SEEN: String = fingerprint("")

        /** La regola, separata per i test: un testo, diverso dall'ultimo letto. */
        fun pendingOf(message: String?, lastSeen: String?): String? {
            val text = message?.trim().orEmpty()
            if (text.isEmpty()) return null
            return text.takeIf { fingerprint(it) != lastSeen }
        }

        /**
         * Impronta del testo (FNV-1a a 64 bit sui byte UTF-8), uguale su iOS e
         * Android: salvare l'impronta e non il testo tiene piccola la riga.
         * Spazi in testa e in coda non contano.
         */
        fun fingerprint(message: String): String {
            var hash = -0x340d631b7bdddcdbL // 0xcbf29ce484222325
            for (b in message.trim().encodeToByteArray()) {
                hash = hash xor (b.toLong() and 0xff)
                hash *= 0x100000001b3L
            }
            return hash.toULong().toString(16)
        }
    }
}
