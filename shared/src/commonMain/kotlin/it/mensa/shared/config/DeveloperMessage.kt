package it.mensa.shared.config

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import it.mensa.shared.api.endpoints.SettingsApi
import it.mensa.shared.db.MensaDatabase

/**
 * Un messaggio degli sviluppatori ai soci, in markdown, dalla chiave
 * `developer_message` della collection `configs`.
 *
 * Si mostra all'apertura finche' non e' stato letto tutto, e ricompare ogni
 * volta che il testo cambia rispetto all'ultimo letto. Conta solo l'ultimo,
 * non lo storico: "problema X", poi "problema Y", poi di nuovo "problema X"
 * compaiono tutti e tre, perche' rimettere un testo vecchio e' un messaggio
 * nuovo per chi legge. Per spegnerlo basta svuotare la chiave.
 *
 * L'ultimo valore arrivato dal server resta salvato: chi apre l'app offline
 * vede comunque un messaggio che non ha ancora letto.
 */
class DeveloperMessage internal constructor(
    private val settings: SettingsApi,
    private val db: MensaDatabase,
) {
    /** Il messaggio da mostrare adesso, o null se non c'e' o e' gia' stato letto. */
    @Throws(Exception::class)
    suspend fun pending(): String? {
        val message = runCatching { settings.configs()[KEY] }
            .onSuccess { fresh -> store(KEY_LATEST, fresh?.trim().orEmpty()) }
            .getOrElse { read(KEY_LATEST) }
        return pendingOf(message, lastSeen())
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
