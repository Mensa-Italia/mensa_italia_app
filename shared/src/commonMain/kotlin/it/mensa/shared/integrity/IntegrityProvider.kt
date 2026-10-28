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

    internal val ALL = listOf(PLATFORM, CHALLENGE, TOKEN, KEY_ID, ASSERTION)
}

/** La chiave App Attest appena generata e attestata, da mandare a `POST /api/integrity/ios/attest`. */
class KeyAttestation(
    /** L'identificatore della chiave cosi' come lo restituisce Apple (gia' base64 standard). */
    val keyId: String,
    /** L'oggetto di attestazione CBOR, in base64 standard. */
    val attestation: String,
)

/**
 * La parte specifica di piattaforma della prova di integrita': Play Integrity
 * su Android, App Attest su iOS. Tutto il resto (quando chiederla, la sfida, la
 * costruzione di `clientData`, i retry) sta in [IntegrityGuard] ed e' uguale
 * ovunque.
 *
 * Le implementazioni lanciano solo le eccezioni di [IntegrityException]:
 * [IntegrityUnsupportedException] se il dispositivo non puo' farlo,
 * [IntegrityUnavailableException] se non puo' farlo adesso.
 */
interface IntegrityProvider {

    /** Valore di `X-Integrity-Platform`: `android` o `ios`. */
    val platform: String

    /**
     * True se la prova richiede una chiave registrata sul server per utente
     * (App Attest). Android no: il token di Play Integrity si basta da solo.
     */
    val requiresKeyEnrollment: Boolean get() = false

    /** Se questo dispositivo e' in grado di produrre la prova. */
    suspend fun isSupported(): Boolean

    /**
     * Gli header propri della piattaforma per [clientData] = `challenge|METHOD|path`
     * (il token; oppure key id e asserzione). `X-Integrity-Platform` e
     * `X-Integrity-Challenge` li aggiunge [IntegrityGuard].
     *
     * Lancia [IntegrityKeyInvalidException] se la chiave App Attest dell'utente
     * non c'e' o non vale piu'.
     */
    suspend fun prove(userId: String, challenge: String, clientData: String): Map<String, String>

    /** Solo App Attest: c'e' una chiave registrata per questo utente? */
    suspend fun hasKey(userId: String): Boolean = true

    /**
     * Solo App Attest: genera una chiave nuova e la attesta con
     * `clientDataHash = SHA256(UTF8(challenge))`. La chiave NON va salvata
     * finche' il server non l'ha accettata: lo si fa con [saveKey].
     */
    suspend fun attest(challenge: String): KeyAttestation =
        throw IntegrityUnsupportedException("Questa piattaforma non registra chiavi")

    /** Solo App Attest: ricorda [keyId] come chiave di [userId], dopo l'ok del server. */
    suspend fun saveKey(userId: String, keyId: String) {}

    /** Solo App Attest: dimentica la chiave di [userId] (non piu' valida o sconosciuta al server). */
    suspend fun forgetKey(userId: String) {}
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
