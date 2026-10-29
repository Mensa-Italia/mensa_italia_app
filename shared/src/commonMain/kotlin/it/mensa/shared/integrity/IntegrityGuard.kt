package it.mensa.shared.integrity

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.expectSuccess
import io.ktor.client.plugins.Sender
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.takeFrom
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import it.mensa.shared.api.ApiConfig
import it.mensa.shared.auth.AccountStatus
import it.mensa.shared.net.ServerReachability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private val apiHost: String = ApiConfig.BASE_URL.substringAfter("://").substringBefore('/')

/** 428: Ktor non ha la costante. */
internal val PreconditionRequired = HttpStatusCode(428, "Precondition Required")

private const val CODE_ACCOUNT_BLOCKED = "account_blocked"
private const val CODE_INTEGRITY_FAILED = "integrity_failed"
private const val CODE_INTEGRITY_REQUIRED = "integrity_required"
private const val CODE_ATTEST_KEY_REQUIRED = "attest_key_required"

/** Si smette di usare una sessione questi secondi prima che scada sul server. */
private const val SESSION_MARGIN_SECONDS = 30L

private const val PATH_CHALLENGE = "/api/integrity/challenge"
private const val PATH_ATTEST = "/api/integrity/ios/attest"

@Serializable
private data class ChallengeResponse(val challenge: String = "")

@Serializable
private data class AttestRequest(val key_id: String, val attestation: String, val challenge: String)

/**
 * Il lato client del livello di accesso `watched` / `blocked`: un interceptor di
 * [HttpSend], come `installAuthRetry`, che fa queste cose per ogni richiesta al
 * nostro server.
 *
 *  1. **Account sospeso.** Un 403 `account_blocked` da *qualunque* risposta
 *     dell'API aggiorna [AccountStatus] e solleva [AccountBlockedException]. Niente
 *     retry, niente logout.
 *  2. **Prova di integrita'.** Un 428 `integrity_required` / `attest_key_required`
 *     fa chiedere una sfida (`POST /api/integrity/challenge`), produrre la prova
 *     con [IntegrityProvider] e ripetere la richiesta UNA volta con gli header.
 *     Se anche il retry viene rifiutato: [IntegrityUnavailableException].
 *     Un 403 `integrity_failed` solleva [IntegrityFailedException] senza retry.
 *  3. **Prova proattiva.** Dopo il primo 428 di un utente, ogni richiesta
 *     successiva *non esente* ([IntegrityScope]) parte gia' con la prova, per non
 *     raddoppiare le chiamate. Una sfida per richiesta: la sfida e' monouso, e le
 *     richieste concorrenti non la condividono mai. Si azzera da sola quando
 *     l'utente cambia o sparisce (logout) e quando una richiesta nuda, durante un
 *     fallimento (punto 5), riceve una risposta buona: l'account e' tornato
 *     libero e l'app non resta chiusa fino al riavvio.
 *  4. **Sessione.** Dopo una prova completa riuscita il server manda una
 *     sessione (`X-Integrity-Session`) che per qualche minuto vale al posto di
 *     sfida + prova, su tutto. Le richieste che non ce l'hanno si mettono in fila
 *     dietro [sessionLock]: la prima fa la prova completa, le altre usano la
 *     sessione che ne torna. Su Android cosi' una schermata che parte con venti
 *     richieste costa una decode di Play Integrity e non venti; su iOS le
 *     asserzioni App Attest non partono mai in parallelo con la stessa chiave,
 *     che e' quello che nella 22.2.18 le faceva fallire a catena. E' anche la
 *     ragione per cui il ciclo di vita della chiave ([AppAttestLifecycle]) non
 *     ha un mutex suo: gli arriva solo roba da qui sotto, una alla volta.
 *  5. **Memoria del fallimento.** Se la prova non si riesce a costruire
 *     ([IntegrityUnsupportedException], [IntegrityUnavailableException] con la
 *     loro causa, un 5xx sulla sfida, o un server che rifiuta anche il retry) lo
 *     si ricorda, per utente e in RAM: [IntegrityUnavailableException] fino a
 *     `30 s x 4^(n-1)` (tetto 10 minuti), [IntegrityUnsupportedException] senza
 *     scadenza nel processo. Finche' vale, nessuna chiamata ad Apple, nessuna
 *     sfida, nessun [sessionLock]: le richieste partono NUDE, decide il server, e
 *     su un 428 l'errore si rilancia dalla memoria. Chi era in coda dietro il
 *     tentativo fallito prende l'errore gia' noto invece di rifarlo. Vale anche
 *     per Android: un [IntegrityUnavailableException] senza [ProofFailure]
 *     (Play Integrity non ne allega) si ricorda col suo backoff, ma non si riporta
 *     al server perche' non c'e' uno stage da descrivere. Non si
 *     ricordano `account_blocked`, un `integrity_failed` su una richiesta
 *     dell'utente (verdetti del server) e le `IOException`: offline non e' un
 *     guasto di integrita' e deve riprendersi appena torna la rete. Un
 *     `integrity_failed` sulla registrazione della chiave (`/ios/attest`) invece
 *     SI ricorda: e' il server che non accetta l'attestazione, la chiave e'
 *     consumata, e senza memoria ogni richiesta ne conierebbe un'altra. Un
 *     successo (la sessione, o una risposta buona a una richiesta con la prova
 *     completa) azzera tutto. Un nuovo avvio e' un tentativo nuovo, di proposito:
 *     un cooldown persistito potrebbe chiudere fuori un socio per ore.
 *
 * Ogni fallimento FRESCO (non quello servito dalla memoria) va al server con
 * [IntegrityReporter], che lo descrive e basta: la decisione del server non
 * dipende mai da cio' che il client dichiara.
 *
 * Il corpo viene rimandato com'e': col ContentNegotiation e' gia' un
 * `TextContent`/`ByteArrayContent`, e il multipart si riscrive. Un corpo che e'
 * solo un canale da leggere (`ReadChannelContent`) non si puo' ripetere; l'app non
 * ne manda, ma se capitasse il retry viene rifiutato con un errore chiaro.
 *
 * [userId] e' una funzione perche' l'`AuthRepository` nasce dopo il client:
 * si legge al momento della richiesta.
 *
 * [clock] misura sessioni e backoff (i test ne iniettano uno a mano); [scope] e'
 * dove girano i report, che non devono mai stare sul percorso della richiesta.
 */
class IntegrityGuard(
    private val client: HttpClient,
    private val provider: IntegrityProvider,
    private val userId: () -> String?,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
    private val clock: TimeSource = TimeSource.Monotonic,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    /** L'utente per cui il server ha gia' chiesto la prova in questa sessione. */
    @Volatile
    private var proofRequiredFor: String? = null

    private class Session(val user: String, val token: String, val usableUntil: TimeMark)

    @Volatile
    private var session: Session? = null

    private val sessionLock = Mutex()

    /** Il fallimento ricordato (vedi il punto 5 della classe); vedi [FailureMemory] per come si modifica. */
    private val memory = FailureMemory(clock)

    private val reporter = IntegrityReporter(client, scope)

    /**
     * Una richiesta mandata, la sessione che portava (null: prova completa o
     * nessuna) e se portava la prova completa ([full]).
     */
    private class Sent(val call: HttpClientCall, val session: String?, val full: Boolean = false)

    /** Registra l'interceptor sul client. */
    fun install() {
        client.plugin(HttpSend).intercept { request -> handle(this, request) }
    }

    /** Dimentica che la prova serviva (logout o cambio utente). */
    fun reset() {
        proofRequiredFor = null
        session = null
        memory.clear()
    }

    internal suspend fun handle(sender: Sender, request: HttpRequestBuilder): HttpClientCall {
        if (request.url.host != apiHost) return sender.execute(request)

        val method = request.method.value.uppercase()
        val path = IntegrityClientData.serverPath(request.url.encodedPath)
        val eligible = !IntegrityScope.isExempt(method, path) &&
            request.headers.contains(HttpHeaders.Authorization)

        val user = userId()
        if (user == null) proofRequiredFor = null
        // Il fallimento ricordato e' di un utente: se cambia o sparisce, si dimentica.
        memory.forgetNotOf(user)

        val wanted = eligible && user != null && proofRequiredFor == user
        // Durante un fallimento la richiesta parte NUDA e decide il server.
        val failing = wanted && memory.known(user) != null
        val proactive = wanted && !failing

        val first = if (proactive) {
            sendWithProof(sender, request, user, method, path, rejectedKeyId = null)
        } else {
            Sent(sender.execute(request), session = null)
        }

        return when (val verdict = classify(first.call, request)) {
            is Verdict.Fine -> verdict.call.also {
                noteSuccess(request, it)
                if (first.full && user != null) memory.forgetOf(user)
                // Una risposta buona a una richiesta nuda mentre il fallimento e' ricordato:
                // il server non chiede piu' la prova (l'account e' tornato free). Il
                // fallimento non si tocca, cosi' il backoff non si perde se il server
                // se lo ricorda ancora.
                if (failing && it.response.status.value < 400) proofRequiredFor = null
            }
            // 428 su una richiesta esente o anonima non e' roba nostra: la si restituisce.
            is Verdict.Needs ->
                if (!eligible) verdict.call
                else {
                    // Sessione rifiutata (scaduta, server riavviato): la si butta, a
                    // meno che un'altra richiesta non l'abbia gia' sostituita.
                    first.session?.let { used -> if (session?.token == used) session = null }
                    retryWithProof(sender, request, verdict, first, user ?: "", method, path)
                }
        }
    }

    private suspend fun retryWithProof(
        sender: Sender,
        request: HttpRequestBuilder,
        needed: Verdict.Needs,
        first: Sent,
        user: String,
        method: String,
        path: String,
    ): HttpClientCall {
        if (request.body is OutgoingContent.ReadChannelContent) {
            throw IntegrityUnavailableException("La richiesta non si puo' ripetere con la prova di integrita'")
        }
        proofRequiredFor = user
        // Un fallimento ricordato: nessuna sfida, nessuna chiamata ad Apple, nessun lock.
        memory.known(user)?.let { throw it.error }

        val started = clock.markNow()
        // La chiave da buttare e' quella che la richiesta rifiutata ha davvero
        // mandato, non "quella che abbiamo adesso": nel frattempo un'altra
        // richiesta potrebbe averla gia' sostituita.
        val rejectedKeyId = if (needed.code == CODE_ATTEST_KEY_REQUIRED) {
            first.call.request.headers[IntegrityHeaders.KEY_ID]
        } else null

        val retry = sendWithProof(sender, request, user, method, path, rejectedKeyId)
        return when (val verdict = classify(retry.call, request)) {
            is Verdict.Fine -> verdict.call.also {
                noteSuccess(request, it)
                if (retry.full) memory.forgetOf(user)
            }
            is Verdict.Needs -> {
                // Il server ha rifiutato anche la prova appena costruita: un solo
                // retry, poi errore. Se non riconosce la chiave, la si butta (quella
                // che questa seconda richiesta ha mandato), cosi' alla prossima se ne
                // registra una nuova; e il rifiuto si ricorda, cosi' un server che
                // rifiuta non viene martellato con due prove per richiesta.
                if (verdict.code == CODE_ATTEST_KEY_REQUIRED) {
                    retry.call.request.headers[IntegrityHeaders.KEY_ID]?.let { provider.discardKey(user, it) }
                }
                serverAnswered()
                val cause = ProofFailure(
                    stage = AttestStage.SERVER,
                    domain = HTTP_DOMAIN,
                    code = PreconditionRequired.value,
                    detail = "Il server ha rifiutato la prova di integrita' (${verdict.code})",
                    underlying = null,
                    disposition = Disposition.TRANSIENT,
                )
                throw remember(
                    user,
                    IntegrityUnavailableException(
                        "Il server ha rifiutato la prova di integrita' (${verdict.code})", failure = cause,
                    ),
                    started,
                )
            }
        }
    }

    /** Una risposta fine non e' mai un 403 bloccante: quelli sono eccezioni. */
    private sealed interface Verdict {
        class Fine(val call: HttpClientCall) : Verdict
        class Needs(val call: HttpClientCall, val code: String) : Verdict
    }

    /**
     * Legge solo i 403 e i 428: il resto passa senza toccare il corpo (che
     * potrebbe essere uno stream). Per quelli due il corpo si salva, cosi' resta
     * leggibile anche dal chiamante.
     */
    private suspend fun classify(call: HttpClientCall, request: HttpRequestBuilder): Verdict {
        val status = call.response.status
        if (status != HttpStatusCode.Forbidden && status != PreconditionRequired) {
            return Verdict.Fine(call)
        }
        val saved = call.save()
        val body = runCatching { json.parseToJsonElement(saved.response.bodyAsText()).jsonObject }.getOrNull()
        val code = body?.get("code")?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }
        val message = body?.get("message")?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }

        return when {
            status == HttpStatusCode.Forbidden && code == CODE_ACCOUNT_BLOCKED -> {
                serverAnswered()
                // Si segna "sospeso" solo se a rispondere cosi' e' stata una
                // richiesta autenticata: un 403 al login non e' una sessione sospesa.
                if (request.headers.contains(HttpHeaders.Authorization)) AccountStatus.markSuspended()
                throw AccountBlockedException(message ?: "Account sospeso")
            }
            status == HttpStatusCode.Forbidden && code == CODE_INTEGRITY_FAILED -> {
                serverAnswered()
                throw IntegrityFailedException(message ?: "Verifica di integrita' non superata")
            }
            status == PreconditionRequired &&
                (code == CODE_INTEGRITY_REQUIRED || code == CODE_ATTEST_KEY_REQUIRED) ->
                Verdict.Needs(saved, code)
            else -> Verdict.Fine(saved)
        }
    }

    /**
     * Una risposta riuscita a una richiesta autenticata vuol dire che l'account
     * non e' (piu') sospeso — ma non una su una route dell'allowlist dei
     * bloccati, che il server serve comunque: vedi [BlockedAllowlist].
     */
    private fun noteSuccess(request: HttpRequestBuilder, call: HttpClientCall) {
        if (call.response.status.value >= 400) return
        if (!request.headers.contains(HttpHeaders.Authorization)) return
        val path = IntegrityClientData.serverPath(request.url.encodedPath)
        if (BlockedAllowlist.allows(request.method.value, path)) return
        AccountStatus.clear()
    }

    /**
     * Il server ha risposto, anche se noi sollevamo un'eccezione. Il tracciamento
     * della raggiungibilita' (`installReachabilityTracking`) registra solo le
     * risposte che gli passano davanti: se questa eccezione parte da dentro,
     * senza questo il server risulterebbe muto e l'app scivolerebbe verso il
     * blocco "offline da piu' di un giorno" per un account semplicemente sospeso.
     */
    private fun serverAnswered() = ServerReachability.onResponse()

    // ── Memoria del fallimento ───────────────────────────────────────────────

    /**
     * Ricorda [e] per [user] se e' un fallimento della prova e, se ne conosce la
     * causa, lo manda al server. Torna [e], da rilanciare. Non tocca
     * `account_blocked` e un `integrity_failed` su una richiesta dell'utente
     * (verdetti del server, non guasti di costruzione della prova). Una sfida che
     * risponde male si ricorda solo se e' un 5xx.
     *
     * Senza [ProofFailure] (Play Integrity non ne allega; un dispositivo non
     * supportato che non e' iOS) un [IntegrityUnavailableException] si ricorda
     * comunque, col suo backoff: e' il guasto passeggero di Google (troppe
     * richieste, rete, errore interno) che altrimenti ripeterebbe sfida +
     * chiamata a Play Integrity a ogni richiesta. Non si riporta, perche' non c'e'
     * uno stage da descrivere, e un [IntegrityUnsupportedException] senza causa
     * non si ricorda: non chiama nessuno e a ogni richiesta costa zero.
     *
     * Si chiama una volta sola per fallimento, dove nasce: chi lo riceve dalla
     * memoria non passa di qui, quindi non riporta e non allunga il backoff.
     */
    private fun remember(user: String, e: IntegrityException, started: TimeMark): IntegrityException {
        val cause = when (e) {
            is IntegrityUnsupportedException -> e.failure
            is IntegrityUnavailableException -> e.failure
            else -> null
        }
        if (cause == null) {
            if (e is IntegrityUnavailableException) memory.remember(user, e, expires = true)
            return e
        }
        if (cause.stage == AttestStage.CHALLENGE && (cause.code ?: 0) !in 500..599) return e

        val attempts = memory.remember(user, e, expires = e !is IntegrityUnsupportedException)
        // Una riga nel log per ogni fallimento fresco, qui dove nasce: leggere
        // `userMessage` non scrive niente, perche' una schermata lo rilegge a ogni
        // ridisegno. Solo il tag (`attest_key/2`): nessun carattere che un
        // `println` di Kotlin/Native possa scambiare per un formato.
        println("MENSA_INTEGRITY_FAILURE: ${cause.tag} attempt=$attempts")
        reporter.report(
            platform = provider.platform,
            environment = provider.environment(),
            failure = cause,
            outcome = ReportOutcome.FAILED,
            attempt = attempts,
            elapsedMs = started.elapsedNow().inWholeMilliseconds,
        )
        return e
    }

    // ── Costruzione della prova ──────────────────────────────────────────────

    /**
     * Manda [request] con la sessione se ce n'e' una valida, altrimenti con la
     * prova completa, una alla volta. Vedi i punti 4 e 5 della classe.
     */
    private suspend fun sendWithProof(
        sender: Sender,
        request: HttpRequestBuilder,
        user: String,
        method: String,
        path: String,
        rejectedKeyId: String?,
    ): Sent {
        val full: suspend () -> Sent = {
            val call = sender.execute(withProof(request, user, method, path, rejectedKeyId))
            captureSession(user, call)
            Sent(call, session = null, full = true)
        }
        sessionFor(user)?.let { return Sent(sender.execute(withSession(request, it)), it) }
        // Un fallimento gia' noto, prima di mettersi in coda...
        memory.known(user)?.let { throw it.error }
        return sessionLock.withLock {
            sessionFor(user)?.let { return@withLock Sent(sender.execute(withSession(request, it)), it) }
            // ...e di nuovo dentro: chi era in coda dietro un tentativo fallito prende
            // l'errore che c'e' gia', invece di rifare sfida + chiave + attestazione.
            memory.known(user)?.let { throw it.error }
            full()
        }
    }

    private fun sessionFor(user: String): String? {
        val s = session ?: return null
        if (s.user != user || s.usableUntil.hasPassedNow()) return null
        return s.token
    }

    private fun captureSession(user: String, call: HttpClientCall) {
        val headers = call.response.headers
        val token = headers[IntegrityHeaders.SESSION]?.takeIf { it.isNotBlank() } ?: return
        val ttl = headers[IntegrityHeaders.SESSION_TTL]?.toLongOrNull() ?: return
        val usable = (ttl - SESSION_MARGIN_SECONDS).coerceAtLeast(0)
        session = Session(user, token, clock.markNow() + usable.seconds)
    }

    /** Una copia di [request] con la sola sessione al posto della prova. */
    private fun withSession(request: HttpRequestBuilder, token: String): HttpRequestBuilder =
        HttpRequestBuilder().apply {
            takeFrom(request)
            IntegrityHeaders.ALL.forEach { this.headers.remove(it) }
            header(IntegrityHeaders.PLATFORM, provider.platform)
            header(IntegrityHeaders.SESSION, token)
        }

    /** Una copia di [request] con gli header della prova, una sfida nuova. */
    private suspend fun withProof(
        request: HttpRequestBuilder,
        user: String,
        method: String,
        path: String,
        rejectedKeyId: String?,
    ): HttpRequestBuilder {
        val headers = proofHeaders(user, method, path, rejectedKeyId)
        return HttpRequestBuilder().apply {
            takeFrom(request)
            IntegrityHeaders.ALL.forEach { this.headers.remove(it) }
            headers.forEach { (name, value) -> header(name, value) }
        }
    }

    /**
     * Gli header completi della prova per ([method], [path]); lancia le eccezioni
     * tipizzate. [rejectedKeyId] e' la chiave che il server ha appena rifiutato
     * (`attest_key_required`): si butta prima di ogni altra cosa.
     *
     * Un fallimento della prova si ricorda qui ([remember]), una volta, nel punto
     * in cui nasce.
     */
    internal suspend fun proofHeaders(
        user: String,
        method: String,
        path: String,
        rejectedKeyId: String?,
    ): Map<String, String> {
        val started = clock.markNow()
        try {
            return buildProof(user, method, path, rejectedKeyId, started)
        } catch (e: IntegrityException) {
            throw remember(user, e, started)
        }
    }

    private suspend fun buildProof(
        user: String,
        method: String,
        path: String,
        rejectedKeyId: String?,
        started: TimeMark,
    ): Map<String, String> {
        if (!provider.isSupported()) {
            val detail = "Questo dispositivo non puo' dimostrare la propria integrita'"
            throw IntegrityUnsupportedException(detail, failure = unsupportedFailure(detail))
        }
        if (rejectedKeyId != null) provider.discardKey(user, rejectedKeyId)
        provider.ensureEnrolled(user, enrollmentServer)

        var rotated = false
        while (true) {
            val challenge = fetchChallenge()
            val clientData = IntegrityClientData.build(challenge, method, path)
            try {
                val specific = provider.prove(user, challenge, clientData)
                return buildMap {
                    put(IntegrityHeaders.PLATFORM, provider.platform)
                    put(IntegrityHeaders.CHALLENGE, challenge)
                    putAll(specific)
                }
            } catch (e: IntegrityKeyInvalidException) {
                // La chiave locale non vale piu': se ne registra una nuova, una volta.
                // Il compare-and-set sul key id fa il lavoro dell'epoca di prima: se
                // un'altra richiesta l'ha gia' sostituita, qui non si butta niente.
                if (rotated) {
                    throw IntegrityUnavailableException("Chiave di attestazione non valida", e, e.failure)
                }
                rotated = true
                // L'errore ORIGINALE della chiave scartata va al server anche se poi
                // tutto riesce: e' cosi' che il DCError di una chiave stantia si vede.
                e.failure?.let {
                    reporter.report(
                        platform = provider.platform,
                        environment = provider.environment(),
                        failure = it,
                        outcome = ReportOutcome.ROTATED,
                        attempt = memory.attemptsOf(user) + 1,
                        elapsedMs = started.elapsedNow().inWholeMilliseconds,
                    )
                }
                provider.discardKey(user, e.keyId)
                provider.ensureEnrolled(user, enrollmentServer)
            }
        }
    }

    /**
     * La causa di un dispositivo che dichiara di non poter dimostrare nulla,
     * prima ancora di interpellare Apple: per App Attest e' `generate_key/1`
     * (la stessa di un `DCError` 1 vero, che e' quello che `isSupported` falso
     * vuol dire). Per le altre piattaforme null: gli stage sono quelli di App
     * Attest e un `generate_key` su Android o su watchOS sarebbe una bugia nel
     * messaggio e nel log del server. L'errore resta quello di sempre, senza
     * tag e senza memoria ne' report: non chiama nessuno, costa zero a ogni richiesta.
     */
    private fun unsupportedFailure(detail: String): ProofFailure? {
        if (provider.platform != PLATFORM_IOS) return null
        return ProofFailure(
            stage = AttestStage.GENERATE_KEY,
            domain = UNSUPPORTED_DOMAIN,
            code = 1,
            detail = detail,
            underlying = null,
            disposition = Disposition.UNSUPPORTED,
        )
    }

    /** Le due chiamate al server che la registrazione di una chiave usa; vedi [IntegrityProvider.ensureEnrolled]. */
    private val enrollmentServer = object : EnrollmentServer {
        override suspend fun challenge(): String = fetchChallenge()

        override suspend fun register(keyId: String, attestation: String, challenge: String) {
            val response = try {
                client.post(PATH_ATTEST) {
                    // Lo stato lo legge qui sotto: diventa un fallimento REGISTER.
                    expectSuccess = false
                    contentType(ContentType.Application.Json)
                    setBody(AttestRequest(keyId, attestation, challenge))
                }
            } catch (e: IntegrityFailedException) {
                // Un 403 `integrity_failed` su QUESTA route non e' il verdetto su una
                // richiesta dell'utente (che si rilancia cosi' com'e'): e' il server
                // che non accetta l'attestazione (ambiente App Attest sbagliato, catena
                // di certificati, sfida scaduta, key id gia' registrato). L'interceptor
                // lo trasforma prima che arrivi qui sotto come status, quindi senza
                // questa conversione non sarebbe un fallimento REGISTER: nessuna
                // memoria, nessun backoff, nessun report, e ogni richiesta in coda
                // conierebbe un'altra chiave, la cascata che si vuole fermare.
                throw registrationRefused(HttpStatusCode.Forbidden.value, CODE_INTEGRITY_FAILED, e)
            }
            // Gli altri errori del server (account_blocked) li solleva gia'
            // l'interceptor; qui resta solo cio' che non e' un 2xx.
            val status = response.status.value
            if (status !in 200..299) throw registrationRefused(status, null, null)
        }

        private fun registrationRefused(status: Int, code: String?, cause: Throwable?): IntegrityUnavailableException {
            val detail = "Registrazione della chiave rifiutata: HTTP $status" + (code?.let { " ($it)" } ?: "")
            val failure = ProofFailure(
                stage = AttestStage.REGISTER,
                domain = HTTP_DOMAIN,
                code = status,
                detail = detail,
                underlying = null,
                disposition = classify(AttestStage.REGISTER, HTTP_DOMAIN, status),
            )
            return IntegrityUnavailableException(detail, cause, failure)
        }
    }

    private suspend fun fetchChallenge(): String {
        // Lo stato lo legge qui sotto: diventa un fallimento CHALLENGE.
        val response = client.post(PATH_CHALLENGE) { expectSuccess = false }
        val status = response.status.value
        if (status !in 200..299) {
            throw IntegrityUnavailableException(
                "Sfida di integrita' non disponibile: HTTP $status",
                failure = challengeFailure(status, "Sfida di integrita' non disponibile: HTTP $status"),
            )
        }
        val challenge = response.body<ChallengeResponse>().challenge
        if (challenge.isBlank()) {
            throw IntegrityUnavailableException(
                "Sfida di integrita' vuota",
                failure = challengeFailure(status, "Sfida di integrita' vuota"),
            )
        }
        return challenge
    }

    private fun challengeFailure(status: Int, detail: String) = ProofFailure(
        stage = AttestStage.CHALLENGE,
        domain = HTTP_DOMAIN,
        code = status,
        detail = detail,
        underlying = null,
        disposition = Disposition.TRANSIENT,
    )
}
