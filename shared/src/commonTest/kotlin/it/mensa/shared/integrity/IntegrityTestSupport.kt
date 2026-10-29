package it.mensa.shared.integrity

import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import it.mensa.shared.api.ApiConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlin.concurrent.Volatile
import kotlin.time.Duration.Companion.milliseconds

// Doppioni di test condivisi dai test dell'integrita'.

internal val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())

internal fun MockRequestHandleScope.error(status: HttpStatusCode, code: String) =
    respond("""{"status":${status.value},"message":"msg $code","code":"$code"}""", status, jsonHeaders)

internal fun HttpRequestData.hasProof() = headers.contains(IntegrityHeaders.CHALLENGE)

/** Un `Settings` in memoria (il modulo di test della libreria non e' una dipendenza). */
internal class InMemorySettings : Settings {
    val values = linkedMapOf<String, Any>()
    var removals = 0

    override val keys: Set<String> get() = values.keys
    override val size: Int get() = values.size
    override fun clear() = values.clear()
    override fun remove(key: String) {
        removals++
        values.remove(key)
    }
    override fun hasKey(key: String) = key in values

    override fun putInt(key: String, value: Int) { values[key] = value }
    override fun getInt(key: String, defaultValue: Int) = values[key] as? Int ?: defaultValue
    override fun getIntOrNull(key: String) = values[key] as? Int
    override fun putLong(key: String, value: Long) { values[key] = value }
    override fun getLong(key: String, defaultValue: Long) = values[key] as? Long ?: defaultValue
    override fun getLongOrNull(key: String) = values[key] as? Long
    override fun putString(key: String, value: String) { values[key] = value }
    override fun getString(key: String, defaultValue: String) = values[key] as? String ?: defaultValue
    override fun getStringOrNull(key: String) = values[key] as? String
    override fun putFloat(key: String, value: Float) { values[key] = value }
    override fun getFloat(key: String, defaultValue: Float) = values[key] as? Float ?: defaultValue
    override fun getFloatOrNull(key: String) = values[key] as? Float
    override fun putDouble(key: String, value: Double) { values[key] = value }
    override fun getDouble(key: String, defaultValue: Double) = values[key] as? Double ?: defaultValue
    override fun getDoubleOrNull(key: String) = values[key] as? Double
    override fun putBoolean(key: String, value: Boolean) { values[key] = value }
    override fun getBoolean(key: String, defaultValue: Boolean) = values[key] as? Boolean ?: defaultValue
    override fun getBooleanOrNull(key: String) = values[key] as? Boolean
}

/** Un errore di Apple, come lo tradurrebbe `DcAppAttestApi`. */
internal fun appleError(
    code: Int,
    domain: String = DC_DOMAIN,
    detail: String = "errore $code",
    underlying: String? = null,
) = AppAttestNativeError(domain, code, detail, underlying)

/**
 * Apple, programmabile chiamata per chiamata. Ogni chiamata passa da un
 * ritardo (virtuale nei `runTest`) per lasciar correre le altre coroutine, e
 * tiene il conto di quante ne vede insieme ([maxConcurrentCalls]).
 *
 * Di default tutto riesce: chiavi `key-1`, `key-2`..., attestazione `att(<keyId>)`,
 * asserzione `asr(<keyId>)`. Per far fallire una chiamata si accodano errori in
 * [generateKeyErrors] / [attestErrors] / [assertionErrors] (null = quella riesce),
 * oppure si sostituisce il comportamento con [onAttestKey] ecc.
 */
internal class FakeAppAttestApi : AppAttestApi {
    var supported = true

    /** Nell'ordine: `generateKey`, `attestKey:<keyId>`, `assertion:<keyId>`. */
    val calls = mutableListOf<String>()
    val attestHashes = mutableListOf<ByteArray>()

    var generateKeyCalls = 0
    var attestKeyCalls = 0
    var assertionCalls = 0

    val generateKeyErrors = ArrayDeque<AppAttestNativeError?>()
    val attestErrors = ArrayDeque<AppAttestNativeError?>()
    val assertionErrors = ArrayDeque<AppAttestNativeError?>()

    /** Se impostato, rimpiazza il comportamento di `attestKey` (per esempio non rispondere mai). */
    var onAttestKey: (suspend (keyId: String) -> ByteArray)? = null
    var onGenerateKey: (suspend () -> String)? = null

    /** Gli errori che si accodano a chi prova con una chiave data: `assertion` fallisce se la chiave e' qui. */
    val brokenKeys = mutableMapOf<String, AppAttestNativeError>()

    private var inFlight = 0
    var maxConcurrentCalls = 0

    private suspend fun <T> call(block: suspend () -> T): T {
        inFlight++
        maxConcurrentCalls = maxOf(maxConcurrentCalls, inFlight)
        try {
            delay(10.milliseconds)
            return block()
        } finally {
            inFlight--
        }
    }

    override suspend fun isSupported() = supported

    override suspend fun generateKey(): String {
        calls += "generateKey"
        val n = ++generateKeyCalls
        return call {
            onGenerateKey?.invoke()
                ?: generateKeyErrors.removeFirstOrNull()?.let { throw it }
                ?: "key-$n"
        }
    }

    override suspend fun attestKey(keyId: String, clientDataHash: ByteArray): ByteArray {
        calls += "attestKey:$keyId"
        attestKeyCalls++
        attestHashes += clientDataHash
        return call {
            onAttestKey?.invoke(keyId)
                ?: attestErrors.removeFirstOrNull()?.let { throw it }
                ?: "att($keyId)".encodeToByteArray()
        }
    }

    override suspend fun generateAssertion(keyId: String, clientDataHash: ByteArray): ByteArray {
        calls += "assertion:$keyId"
        assertionCalls++
        return call {
            brokenKeys[keyId]?.let { throw it }
            assertionErrors.removeFirstOrNull()?.let { throw it }
            "asr($keyId)".encodeToByteArray()
        }
    }
}

/** Il server dal punto di vista di [EnrollmentServer], senza HTTP. */
internal class FakeEnrollmentServer : EnrollmentServer {
    var challenges = 0
    val registered = mutableListOf<Triple<String, String, String>>()

    /** Per far fallire o far attendere la registrazione. */
    var onRegister: suspend (keyId: String) -> Unit = {}
    var onChallenge: suspend () -> Unit = {}

    override suspend fun challenge(): String {
        onChallenge()
        return "chal-${++challenges}"
    }

    override suspend fun register(keyId: String, attestation: String, challenge: String) {
        onRegister(keyId)
        registered += Triple(keyId, attestation, challenge)
    }
}

/**
 * Lo stesso collegamento tra [IntegrityProvider] e [AppAttestLifecycle] che fa
 * `AppAttestProvider` in appleMain, per provare guard e ciclo di vita insieme
 * (il vero sta in Kotlin/Native e non gira in JVM).
 */
internal class LifecycleBackedProvider(
    val lifecycle: AppAttestLifecycle,
    private val env: DeviceEnvironment = TEST_ENVIRONMENT,
) : IntegrityProvider {
    override val platform = "ios"
    override suspend fun isSupported() = lifecycle.isSupported()
    override suspend fun ensureEnrolled(userId: String, server: EnrollmentServer) =
        lifecycle.ensureEnrolled(userId, server)
    override suspend fun discardKey(userId: String, keyId: String?) = lifecycle.discardKey(userId, keyId)
    override suspend fun prove(userId: String, challenge: String, clientData: String) =
        lifecycle.prove(userId, clientData)
    override fun environment() = env
}

internal val TEST_ENVIRONMENT = DeviceEnvironment(app = "31000984", os = "iOS 27.0.1 (24A446)", iosAppOnMac = false)

/**
 * Un provider scritto a mano. Con [requiresKeyEnrollment] si comporta come
 * App Attest: [ensureEnrolled] chiede una sfida, "genera" una chiave e la
 * registra sul server; senza, come Play Integrity (niente da registrare).
 */
internal class FakeProvider(
    override val platform: String = "android",
    val requiresKeyEnrollment: Boolean = false,
    var supported: Boolean = true,
    var key: String? = null,
    private val env: DeviceEnvironment = DeviceEnvironment.UNKNOWN,
) : IntegrityProvider {
    var keysGenerated = 0

    /** Quante volte e' stata davvero tentata una registrazione (non contano le chiamate con la chiave gia' c'e'). */
    var enrollAttempts = 0
    val discarded = mutableListOf<String?>()

    /** Fa fallire la prossima `prove` come se la chiave fosse invalida. */
    var invalidateKeyOnNextProve = false

    /** L'errore originale di Apple che accompagna la chiave invalida. */
    var invalidKeyFailure: ProofFailure? = null

    /** Chiavi che `prove` rifiuta come invalide (app reinstallata). */
    val staleKeys = mutableSetOf<String>()

    /** Se impostato, `ensureEnrolled` fa la sua sfida e poi fallisce cosi' (come un `attestKey` che non va). */
    var enrollmentFailure: IntegrityException? = null

    /** Se impostato, `prove` fallisce cosi' (senza passare dalla chiave). */
    var proveFailure: IntegrityException? = null

    var proveCalls = 0

    /** Quante `prove` sono in corso adesso, e il massimo visto. */
    private var proving = 0
    var maxConcurrentProofs = 0

    override suspend fun isSupported() = supported

    override fun environment() = env

    override suspend fun ensureEnrolled(userId: String, server: EnrollmentServer) {
        if (!requiresKeyEnrollment || key != null) return
        enrollAttempts++
        val challenge = server.challenge()
        enrollmentFailure?.let { throw it }
        val keyId = "key-${++keysGenerated}"
        server.register(keyId, "att($challenge)", challenge)
        key = keyId
    }

    override suspend fun discardKey(userId: String, keyId: String?) {
        discarded += keyId
        if (keyId == null || key == keyId) key = null
    }

    override suspend fun prove(userId: String, challenge: String, clientData: String): Map<String, String> {
        proveCalls++
        if (invalidateKeyOnNextProve) {
            invalidateKeyOnNextProve = false
            throw IntegrityKeyInvalidException(key.orEmpty(), invalidKeyFailure)
        }
        if (key != null && key in staleKeys) throw IntegrityKeyInvalidException(key.orEmpty(), invalidKeyFailure)
        proveFailure?.let { throw it }
        proving++
        maxConcurrentProofs = maxOf(maxConcurrentProofs, proving)
        try {
            delay(10) // come generateAssertion: lascia correre le altre coroutine
        } finally {
            proving--
        }
        return if (requiresKeyEnrollment) {
            mapOf(IntegrityHeaders.KEY_ID to key!!, IntegrityHeaders.ASSERTION to "asr($clientData)")
        } else {
            mapOf(IntegrityHeaders.TOKEN to "tok($clientData)")
        }
    }
}

/** Un server finto: registra ogni richiesta e risponde alla sfida e al report da solo. */
internal class FakeServer(private val route: MockRequestHandleScope.(HttpRequestData) -> io.ktor.client.request.HttpResponseData?) {
    // Il motore finto gira su altri thread (e il report parte in background): la lista
    // delle richieste e' immutabile e si sostituisce sotto lock, cosi' chi legge ne vede
    // sempre una copia coerente.
    @Volatile
    private var seen: List<HttpRequestData> = emptyList()
    private val seenLock = Mutex()
    val requests: List<HttpRequestData> get() = seen
    fun clearRequests() {
        seen = emptyList()
    }

    var challenges = 0
    var attestStatus = HttpStatusCode.OK
    var attestBody = """{"ok":true}"""

    /** Come risponde la sfida; con [challengeError] la richiesta stessa fallisce (rete caduta). */
    var challengeStatus = HttpStatusCode.OK
    var challengeError: Throwable? = null

    /** Come risponde il report del fallimento, o l'errore che lo fa fallire. */
    var reportStatus = HttpStatusCode.OK
    var reportResponseBody = """{"ok":true}"""
    var reportError: Throwable? = null

    /** Per far attendere il report (anche per sempre) prima di rispondere. */
    var onReport: suspend () -> Unit = {}

    val api: List<HttpRequestData>
        get() = requests.filter { it.url.encodedPath != CHALLENGE && it.url.encodedPath != ATTEST && it.url.encodedPath != REPORT }
    val challengeCalls: Int get() = requests.count { it.url.encodedPath == CHALLENGE }
    val reports: List<HttpRequestData> get() = requests.filter { it.url.encodedPath == REPORT }

    fun client(authenticated: Boolean = true): HttpClient {
        val engine = MockEngine(MockEngineConfig().apply {
            addHandler { request ->
                seenLock.withLock { seen = seen + request }
                when (request.url.encodedPath) {
                    CHALLENGE -> {
                        challengeError?.let { throw it }
                        if (challengeStatus == HttpStatusCode.OK) {
                            respond("""{"challenge":"chal-${++challenges}","expires_in":120}""", HttpStatusCode.OK, jsonHeaders)
                        } else {
                            respond("boom", challengeStatus)
                        }
                    }
                    ATTEST -> respond(attestBody, attestStatus, jsonHeaders)
                    REPORT -> {
                        onReport()
                        reportError?.let { throw it }
                        respond(reportResponseBody, reportStatus, jsonHeaders)
                    }
                    else -> route(request) ?: respond("{}", HttpStatusCode.OK, jsonHeaders)
                }
            }
        })
        return HttpClient(engine) {
            // Come il client di produzione (`mensaDefaults`): una risposta non
            // 2xx che arriva a chi chiama e' un'eccezione.
            expectSuccess = true
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            defaultRequest {
                url(ApiConfig.BASE_URL)
                contentType(ContentType.Application.Json)
                if (authenticated) header(HttpHeaders.Authorization, "Bearer test-token")
            }
        }
    }

    companion object {
        const val CHALLENGE = "/api/integrity/challenge"
        const val ATTEST = "/api/integrity/ios/attest"
        const val REPORT = "/api/integrity/report"
    }
}

/**
 * Aspetta, in tempo REALE, che il server abbia visto [expected] report (e un
 * attimo in piu', per accorgersi di uno di troppo; con 0 e' solo quell'attimo).
 * I report partono in background nello scope del test e il motore finto gira su
 * altri thread: `runCurrent()` da solo non basta a farli arrivare.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal suspend fun TestScope.awaitReports(server: FakeServer, expected: Int) {
    runCurrent()
    withContext(Dispatchers.Default) {
        withTimeout(5_000) { while (server.reports.size < expected) delay(5) }
        delay(40)
    }
    runCurrent()
}

/**
 * Lo scope dei report in tempo REALE, cancellato a fine test. Il motore finto
 * risponde su altri thread: con lo scope virtuale del test, `runTest` darebbe per
 * scaduto il timeout dei 3 s del report non appena il test si ferma ad aspettare, e
 * annullerebbe la richiesta prima che arrivi la risposta.
 */
internal fun TestScope.realTimeScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Default).also { reports ->
        backgroundScope.launch {
            try {
                awaitCancellation()
            } finally {
                reports.cancel()
            }
        }
    }
