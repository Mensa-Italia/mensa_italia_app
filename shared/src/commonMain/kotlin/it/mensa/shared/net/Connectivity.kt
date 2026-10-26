package it.mensa.shared.net

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.get
import it.mensa.shared.api.ApiConfig
import it.mensa.shared.api.SkipAuthAttribute
import it.mensa.shared.db.MensaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * Se il server risponde, e quando ha risposto l'ultima volta.
 *
 * Lo scrive ogni richiesta vera a `svc.mensa.it` (vedi
 * [installReachabilityTracking]): una risposta vuol dire raggiungibile, un
 * errore di rete ([NetworkErrors]) no. `null` finche' nessuna richiesta e'
 * finita, cioe' nei primi istanti dopo l'avvio.
 */
object ServerReachability {
    private val _reachable = MutableStateFlow<Boolean?>(null)
    val reachable: StateFlow<Boolean?> = _reachable.asStateFlow()

    private val _lastContact = MutableStateFlow<Instant?>(null)
    val lastContact: StateFlow<Instant?> = _lastContact.asStateFlow()

    internal fun onResponse(now: Instant = Clock.System.now()) {
        _reachable.value = true
        _lastContact.value = now
    }

    internal fun onUnreachable() {
        _reachable.value = false
    }

    /** L'ultimo contatto salvato, letto all'avvio; non sovrascrive uno piu' recente. */
    internal fun restore(lastContact: Instant) {
        val current = _lastContact.value
        if (current == null || current < lastContact) _lastContact.value = lastContact
    }
}

private val apiHost: String = ApiConfig.BASE_URL.substringAfter("://").substringBefore('/')

/**
 * Registra in [ServerReachability] l'esito di ogni richiesta al backend.
 * Le risposte 5xx non contano come contatto: arrivano dal proxy anche con il
 * backend giu'.
 */
fun HttpClient.installReachabilityTracking() {
    plugin(HttpSend).intercept { request ->
        val ours = request.url.host == apiHost
        try {
            val call = execute(request)
            if (ours && call.response.status.value < 500) ServerReachability.onResponse()
            call
        } catch (t: Throwable) {
            if (ours && NetworkErrors.isUnavailable(t)) ServerReachability.onUnreachable()
            throw t
        }
    }
}

/** Vedi [Connectivity.gate]. */
enum class OnlineGate {
    /** Si entra (online, oppure offline da poco: con l'avviso). */
    OPEN,
    /** Offline troppo a lungo secondo l'ultimo contatto, la rete non ha ancora risposto: si aspetta. */
    CHECKING,
    /** Offline da piu' di [Connectivity.MAX_OFFLINE]: serve collegarsi. */
    BLOCKED,
}

/**
 * Lo stato della connessione per le UI: l'avviso offline in Home e il blocco
 * dopo [MAX_OFFLINE] senza nessun contatto col server.
 *
 * Il blocco esiste perche' l'app parte dalla cache: senza un limite, chi non
 * si collega mai continuerebbe a usare una tessera, timbri e dati che nessuno
 * ha piu' verificato.
 */
class Connectivity(
    private val db: MensaDatabase,
    private val client: HttpClient,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val reachable: StateFlow<Boolean?> = ServerReachability.reachable
    val lastContact: StateFlow<Instant?> = ServerReachability.lastContact

    // Il blocco scatta anche restando offline senza toccare niente: il tempo
    // passa e il limite va ricontrollato.
    private val ticks = flow {
        while (true) {
            emit(Unit)
            delay(TICK_MS)
        }
    }

    val gate: StateFlow<OnlineGate> =
        combine(reachable, lastContact, ticks) { r, last, _ -> gate(r, last, Clock.System.now()) }
            .stateIn(scope, SharingStarted.Eagerly, OnlineGate.OPEN)

    init {
        scope.launch {
            readStored()?.let(ServerReachability::restore)
            persistContacts()
        }
        scope.launch { probeWhileOffline() }
    }

    /** Chiede al server se c'e'. Per il tasto "Riprova"; true se ha risposto. */
    @Throws(Exception::class)
    suspend fun probeNow(): Boolean {
        runCatching {
            client.get("${ApiConfig.BASE_URL}/api/health") { attributes.put(SkipAuthAttribute, true) }
        }
        return reachable.value == true
    }

    /**
     * Finche' il server non risponde, un controllo leggero ogni [PROBE_MS]:
     * la connessione che torna si vede da sola, senza aspettare che l'utente
     * faccia qualcosa che passi dalla rete. All'avvio ne parte uno subito, cosi'
     * lo stato non resta sconosciuto.
     */
    private suspend fun probeWhileOffline() {
        reachable.collectLatest { r ->
            if (r == true) return@collectLatest
            if (r != null) delay(PROBE_MS)
            while (true) {
                probeNow()
                delay(PROBE_MS)
            }
        }
    }

    private suspend fun persistContacts() {
        var written: Instant? = null
        lastContact.collect { at ->
            if (at == null) return@collect
            val prev = written
            if (prev != null && at - prev < PERSIST_EVERY) return@collect
            written = at
            runCatching {
                db.keyValueQueries.insertOrReplace(key = KEY_LAST_CONTACT, value_ = at.toEpochMilliseconds().toString())
            }
        }
    }

    private suspend fun readStored(): Instant? = runCatching {
        db.keyValueQueries.selectById(KEY_LAST_CONTACT).awaitAsOneOrNull()?.value_
            ?.toLongOrNull()?.let(Instant::fromEpochMilliseconds)
    }.getOrNull()

    companion object {
        /** Oltre questo senza nessun contatto col server, l'app chiede di collegarsi. */
        val MAX_OFFLINE: Duration = 24.hours

        private const val KEY_LAST_CONTACT = "net.last_server_contact"
        private const val PROBE_MS = 15_000L
        private const val TICK_MS = 60_000L
        private val PERSIST_EVERY: Duration = Duration.parse("1m")

        /**
         * La regola, separata per i test. Senza un ultimo contatto noto (app
         * appena aggiornata, mai risposto) non si blocca: il primo contatto lo
         * registra.
         */
        fun gate(reachable: Boolean?, lastContact: Instant?, now: Instant): OnlineGate {
            if (reachable == true) return OnlineGate.OPEN
            val last = lastContact ?: return OnlineGate.OPEN
            if (now - last <= MAX_OFFLINE) return OnlineGate.OPEN
            return if (reachable == null) OnlineGate.CHECKING else OnlineGate.BLOCKED
        }
    }
}
