package it.mensa.shared.integrity

import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** Il backoff di un fallimento ricordato: 30 s x 4^(n-1), fino a 10 minuti (30 s, 2 min, 8 min, 10 min...). */
private val BACKOFF_BASE = 30.seconds
private val BACKOFF_CAP = 10.minutes

/** 4^8 x 30 s e' gia' sopra il tetto: oltre non serve calcolare. */
private const val MAX_BACKOFF_EXPONENT = 8

/**
 * L'ultimo fallimento della prova di [user]: l'errore da rilanciare, quante
 * volte di fila e' fallita ([attempts]) e fino a quando vale ([retryAt], null
 * = per sempre, nel processo).
 */
internal class Failure(val user: String, val error: IntegrityException, val retryAt: TimeMark?, val attempts: Int)

/**
 * La memoria del fallimento di [IntegrityGuard]: UN fallimento alla volta, di un
 * solo utente, in RAM.
 *
 * Lo legge e lo scrive chiunque: le richieste del guard, su thread diversi
 * (l'engine di Ktor, `Dispatchers.Default`). Un
 * `@Volatile` con "leggo, decido, scrivo" perderebbe un fallimento appena
 * ricordato da un'altra richiesta: la prima aveva letto "niente" e scrive
 * "niente", oppure la risposta tardiva di una richiesta dell'utente A cancella
 * il backoff di B, che nel frattempo ha fatto login. Per questo ogni modifica e'
 * un compare-and-set su quello che si e' letto, e le due cancellazioni
 * ([forgetNotOf], [forgetOf]) scrivono solo quando hanno davvero qualcosa da
 * cancellare.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class FailureMemory(private val clock: TimeSource) {

    private val ref = AtomicReference<Failure?>(null)

    /** Il fallimento di [user] ancora valido: dello stesso utente, e senza scadenza o non ancora scaduto. */
    fun known(user: String): Failure? =
        ref.load()?.takeIf { it.user == user && (it.retryAt == null || !it.retryAt.hasPassedNow()) }

    /**
     * Ricorda [e] come fallimento di [user]: un tentativo in piu' se il precedente
     * era dello stesso utente, uno solo altrimenti. Con [expires] false (il
     * dispositivo non puo' farlo) non scade nel processo, altrimenti vale
     * `30 s x 4^(n-1)`. Torna il numero di tentativi.
     */
    fun remember(user: String, e: IntegrityException, expires: Boolean): Int {
        while (true) {
            val previous = ref.load()
            val attempts = (previous?.takeIf { it.user == user }?.attempts ?: 0) + 1
            val retryAt = if (expires) clock.markNow() + backoff(attempts) else null
            if (ref.compareAndSet(previous, Failure(user, e, retryAt, attempts))) return attempts
        }
    }

    /** Quanti tentativi consecutivi sono falliti per [user] (0 se il fallimento ricordato e' di un altro o non c'e'). */
    fun attemptsOf(user: String): Int = ref.load()?.takeIf { it.user == user }?.attempts ?: 0

    /** Dimentica il fallimento se NON e' di [user] (cambio utente, logout). Quello di [user] non si tocca. */
    fun forgetNotOf(user: String?) {
        while (true) {
            val current = ref.load() ?: return
            if (current.user == user || ref.compareAndSet(current, null)) return
        }
    }

    /**
     * Un successo di [user]: dimentica il SUO fallimento. Quello di un altro utente
     * resta (la risposta tardiva di una richiesta di A non deve cancellare il
     * backoff di B).
     */
    fun forgetOf(user: String) {
        while (true) {
            val current = ref.load() ?: return
            if (current.user != user || ref.compareAndSet(current, null)) return
        }
    }

    /** Dimentica tutto (logout, [IntegrityGuard.reset]). */
    fun clear() = ref.store(null)

    /** 30 s x 4^(n-1), tetto 10 minuti. */
    internal fun backoff(attempts: Int): Duration {
        val exponent = (attempts - 1).coerceIn(0, MAX_BACKOFF_EXPONENT)
        return minOf(BACKOFF_BASE * (1 shl (2 * exponent)), BACKOFF_CAP)
    }
}
