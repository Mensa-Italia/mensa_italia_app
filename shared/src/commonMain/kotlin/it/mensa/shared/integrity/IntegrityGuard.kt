package it.mensa.shared.integrity

import io.ktor.client.HttpClient
import io.ktor.client.call.HttpClientCall
import io.ktor.client.call.body
import io.ktor.client.call.save
import io.ktor.client.plugins.HttpSend
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.concurrent.Volatile

private val apiHost: String = ApiConfig.BASE_URL.substringAfter("://").substringBefore('/')

/** 428: Ktor non ha la costante. */
internal val PreconditionRequired = HttpStatusCode(428, "Precondition Required")

private const val CODE_ACCOUNT_BLOCKED = "account_blocked"
private const val CODE_INTEGRITY_FAILED = "integrity_failed"
private const val CODE_INTEGRITY_REQUIRED = "integrity_required"
private const val CODE_ATTEST_KEY_REQUIRED = "attest_key_required"

private const val PATH_CHALLENGE = "/api/integrity/challenge"
private const val PATH_ATTEST = "/api/integrity/ios/attest"

@Serializable
private data class ChallengeResponse(val challenge: String = "")

@Serializable
private data class AttestRequest(val key_id: String, val attestation: String, val challenge: String)

/**
 * Il lato client del livello di accesso `watched` / `blocked`: un interceptor di
 * [HttpSend], come `installAuthRetry`, che fa tre cose per ogni richiesta al
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
 *     l'utente cambia o sparisce (logout).
 *
 * Il corpo viene rimandato com'e': col ContentNegotiation e' gia' un
 * `TextContent`/`ByteArrayContent`, e il multipart si riscrive. Un corpo che e'
 * solo un canale da leggere (`ReadChannelContent`) non si puo' ripetere; l'app non
 * ne manda, ma se capitasse il retry viene rifiutato con un errore chiaro.
 *
 * [userId] e' una funzione perche' l'`AuthRepository` nasce dopo il client:
 * si legge al momento della richiesta.
 */
class IntegrityGuard(
    private val client: HttpClient,
    private val provider: IntegrityProvider,
    private val userId: () -> String?,
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    /** L'utente per cui il server ha gia' chiesto la prova in questa sessione. */
    @Volatile
    private var proofRequiredFor: String? = null

    /** Cresce a ogni chiave App Attest registrata: evita che due richieste concorrenti la registrino due volte. */
    @Volatile
    private var enrollmentEpoch: Int = 0

    private val enrollmentLock = Mutex()

    /** Registra l'interceptor sul client. */
    fun install() {
        client.plugin(HttpSend).intercept { request -> handle(this, request) }
    }

    /** Dimentica che la prova serviva (logout o cambio utente). */
    fun reset() {
        proofRequiredFor = null
    }

    internal suspend fun handle(sender: Sender, request: HttpRequestBuilder): HttpClientCall {
        if (request.url.host != apiHost) return sender.execute(request)

        val method = request.method.value.uppercase()
        val path = IntegrityClientData.serverPath(request.url.encodedPath)
        val eligible = !IntegrityScope.isExempt(method, path) &&
            request.headers.contains(HttpHeaders.Authorization)

        val user = userId()
        if (user == null) proofRequiredFor = null
        val epoch = enrollmentEpoch
        val proactive = eligible && user != null && proofRequiredFor == user

        val first = if (proactive) {
            withProof(request, user, method, path, forceEnroll = false, epoch = epoch)
        } else {
            request
        }

        return when (val verdict = classify(sender.execute(first), request)) {
            is Verdict.Fine -> verdict.call.also { noteSuccess(request, it) }
            // 428 su una richiesta esente o anonima non e' roba nostra: la si restituisce.
            is Verdict.Needs ->
                if (!eligible) verdict.call
                else retryWithProof(sender, request, verdict, user ?: "", method, path, epoch)
        }
    }

    private suspend fun retryWithProof(
        sender: Sender,
        request: HttpRequestBuilder,
        needed: Verdict.Needs,
        user: String,
        method: String,
        path: String,
        epoch: Int,
    ): HttpClientCall {
        if (request.body is OutgoingContent.ReadChannelContent) {
            throw IntegrityUnavailableException("La richiesta non si puo' ripetere con la prova di integrita'")
        }
        proofRequiredFor = user

        val retry = withProof(
            request, user, method, path,
            forceEnroll = needed.code == CODE_ATTEST_KEY_REQUIRED,
            epoch = epoch,
        )
        return when (val verdict = classify(sender.execute(retry), request)) {
            is Verdict.Fine -> verdict.call.also { noteSuccess(request, it) }
            is Verdict.Needs -> {
                // Il server ha rifiutato anche la prova appena costruita: un solo
                // retry, poi errore. Se non riconosce la chiave, la si butta, cosi'
                // alla prossima richiesta se ne registra una nuova.
                if (verdict.code == CODE_ATTEST_KEY_REQUIRED) runCatching { provider.forgetKey(user) }
                serverAnswered()
                throw IntegrityUnavailableException("Il server ha rifiutato la prova di integrita' (${verdict.code})")
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

    // ── Costruzione della prova ──────────────────────────────────────────────

    /** Una copia di [request] con gli header della prova, una sfida nuova. */
    private suspend fun withProof(
        request: HttpRequestBuilder,
        user: String,
        method: String,
        path: String,
        forceEnroll: Boolean,
        epoch: Int,
    ): HttpRequestBuilder {
        val headers = proofHeaders(user, method, path, forceEnroll, epoch)
        return HttpRequestBuilder().apply {
            takeFrom(request)
            IntegrityHeaders.ALL.forEach { this.headers.remove(it) }
            headers.forEach { (name, value) -> header(name, value) }
        }
    }

    /** Gli header completi della prova per ([method], [path]); lancia le eccezioni tipizzate. */
    internal suspend fun proofHeaders(
        user: String,
        method: String,
        path: String,
        forceEnroll: Boolean,
        epoch: Int,
    ): Map<String, String> {
        if (!provider.isSupported()) {
            throw IntegrityUnsupportedException("Questo dispositivo non puo' dimostrare la propria integrita'")
        }
        if (provider.requiresKeyEnrollment) ensureKey(user, forceEnroll, epoch)

        var reenrolled = false
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
                if (reenrolled) throw IntegrityUnavailableException("Chiave di attestazione non valida", e)
                reenrolled = true
                provider.forgetKey(user)
                ensureKey(user, force = true, epoch = enrollmentEpoch)
            }
        }
    }

    /**
     * Garantisce che esista una chiave registrata per [user]. Sotto lock: due
     * richieste che scoprono insieme di non averla ne registrano una sola. Con
     * [force] (il server non riconosce la chiave) si registra di nuovo, a meno
     * che qualcun altro non l'abbia gia' fatto dopo che la richiesta e' partita.
     */
    private suspend fun ensureKey(user: String, force: Boolean, epoch: Int) {
        enrollmentLock.withLock {
            val alreadyRefreshed = force && enrollmentEpoch != epoch
            if (alreadyRefreshed) return
            if (!force && provider.hasKey(user)) return

            val challenge = fetchChallenge()
            val attestation = provider.attest(challenge)
            val response = client.post(PATH_ATTEST) {
                contentType(ContentType.Application.Json)
                setBody(AttestRequest(attestation.keyId, attestation.attestation, challenge))
            }
            // Gli errori del server (403 integrity_failed, account_blocked) li
            // solleva gia' l'interceptor; qui resta solo cio' che non e' un 2xx.
            if (response.status.value !in 200..299) {
                throw IntegrityUnavailableException("Registrazione della chiave rifiutata: HTTP ${response.status.value}")
            }
            provider.saveKey(user, attestation.keyId)
            enrollmentEpoch++
        }
    }

    private suspend fun fetchChallenge(): String {
        val response = client.post(PATH_CHALLENGE)
        if (response.status.value !in 200..299) {
            throw IntegrityUnavailableException("Sfida di integrita' non disponibile: HTTP ${response.status.value}")
        }
        val challenge = response.body<ChallengeResponse>().challenge
        if (challenge.isBlank()) throw IntegrityUnavailableException("Sfida di integrita' vuota")
        return challenge
    }
}
