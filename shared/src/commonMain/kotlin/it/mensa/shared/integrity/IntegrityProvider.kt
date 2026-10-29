package it.mensa.shared.integrity

/** Nomi degli header della prova per-richiesta: il contratto col server. */
object IntegrityHeaders {
    const val PLATFORM = "X-Integrity-Platform"
    const val CHALLENGE = "X-Integrity-Challenge"

    /** Android: il token di Play Integrity. */
    const val TOKEN = "X-Integrity-Token"

    /** iOS: l'identificatore della chiave App Attest. */
    const val KEY_ID = "X-Integrity-Key-Id"

    /** iOS: l'asserzione CBOR, in base64. */
    const val ASSERTION = "X-Integrity-Assertion"

    /**
     * Android: la sessione che il server manda dopo una prova Play Integrity
     * riuscita, e che vale al posto di sfida + token per qualche minuto (vedi
     * [IntegrityGuard]).
     */
    const val SESSION = "X-Integrity-Session"

    /** In risposta, accanto a [SESSION]: i secondi di vita della sessione. */
    const val SESSION_TTL = "X-Integrity-Session-Ttl"

    internal val ALL = listOf(PLATFORM, CHALLENGE, TOKEN, KEY_ID, ASSERTION, SESSION)
}

/**
 * Su quale dispositivo e' successo: la build dell'app, il sistema e, su iOS, se
 * l'app per iPhone gira su un Mac con Apple silicon. Va nel report al server
 * ([IntegrityReporter]); nessuno di questi dati cambia mai una decisione.
 */
data class DeviceEnvironment(
    /** `CFBundleVersion` / `versionCode`. */
    val app: String,
    /** Per esempio `iOS 27.0.1 (24A446)`. */
    val os: String,
    val iosAppOnMac: Boolean,
) {
    companion object {
        val UNKNOWN = DeviceEnvironment(app = "", os = "", iosAppOnMac = false)
    }
}

/** `X-Integrity-Platform` di App Attest. */
internal const val PLATFORM_IOS = "ios"

/**
 * Le due chiamate al nostro server che servono a registrare una chiave App
 * Attest. Le fa il [IntegrityGuard] (che ha il client HTTP e il lock); il
 * provider le usa dentro [IntegrityProvider.ensureEnrolled] senza saperne di piu'.
 */
interface EnrollmentServer {
    /** Una sfida nuova (`POST /api/integrity/challenge`). */
    suspend fun challenge(): String

    /**
     * `POST /api/integrity/ios/attest`. Torna normalmente solo con un 2xx; un
     * non-2xx, compreso il 403 `integrity_failed` con cui il server rifiuta
     * un'attestazione, e' un [IntegrityUnavailableException] con un [ProofFailure]
     * di stage [AttestStage.REGISTER] (`account_blocked` passa com'e').
     */
    suspend fun register(keyId: String, attestation: String, challenge: String)
}

/**
 * La parte specifica di piattaforma della prova di integrita': Play Integrity
 * su Android, App Attest su iOS. Tutto il resto (quando chiederla, la sfida, la
 * costruzione di `clientData`, i retry, la memoria dei fallimenti) sta in
 * [IntegrityGuard] ed e' uguale ovunque.
 *
 * Il ciclo di vita della chiave App Attest NON trapela qui: il guard sa solo
 * che prima di una prova va chiamato [ensureEnrolled], e che una chiave
 * rifiutata si butta con [discardKey]. Cosa sia una chiave, dove stia e quando
 * vada rifatta lo sa solo il provider.
 *
 * Le implementazioni lanciano solo tre eccezioni di [IntegrityException]:
 * [IntegrityUnsupportedException] se il dispositivo non puo' farlo,
 * [IntegrityUnavailableException] se non puo' farlo adesso, e (solo da [prove])
 * [IntegrityKeyInvalidException] se la chiave va rifatta. Le eccezioni del
 * server (`AccountBlockedException`, `IntegrityFailedException`) passano cosi'
 * come sono.
 */
interface IntegrityProvider {

    /** Valore di `X-Integrity-Platform`: `android` o `ios` ([PLATFORM_IOS]). */
    val platform: String

    /** Se questo dispositivo e' in grado di produrre la prova. */
    suspend fun isSupported(): Boolean

    /**
     * Gli header propri della piattaforma per [clientData] = `challenge|METHOD|path`
     * (il token; oppure key id e asserzione). `X-Integrity-Platform` e
     * `X-Integrity-Challenge` li aggiunge [IntegrityGuard].
     */
    suspend fun prove(userId: String, challenge: String, clientData: String): Map<String, String>

    /**
     * Solo App Attest: garantisce che [userId] abbia una chiave registrata sul
     * server, registrandola (sfida, `attestKey`, [EnrollmentServer.register]) se
     * manca o se un tentativo precedente si e' interrotto a meta'. Android: niente da fare.
     */
    suspend fun ensureEnrolled(userId: String, server: EnrollmentServer) {}

    /**
     * Solo App Attest: butta la chiave [keyId] di [userId] (rifiutata dal server
     * o da Apple). Compare-and-set: se nel frattempo la chiave e' gia' stata
     * sostituita non fa niente. Con [keyId] null butta quella corrente.
     */
    suspend fun discardKey(userId: String, keyId: String?) {}

    /** Il dispositivo, per il report. */
    fun environment(): DeviceEnvironment = DeviceEnvironment.UNKNOWN
}

/**
 * Provider per le piattaforme che non possono dimostrare nulla (watchOS, test).
 * Lancia [IntegrityUnsupportedException] solo quando la prova serve davvero, cioe'
 * dopo un 428: chi non e' `watched` non se ne accorge.
 */
class UnsupportedIntegrityProvider(override val platform: String = "unsupported") : IntegrityProvider {
    override suspend fun isSupported(): Boolean = false

    override suspend fun prove(userId: String, challenge: String, clientData: String): Map<String, String> =
        throw IntegrityUnsupportedException("Questo dispositivo non puo' dimostrare la propria integrita'")
}
