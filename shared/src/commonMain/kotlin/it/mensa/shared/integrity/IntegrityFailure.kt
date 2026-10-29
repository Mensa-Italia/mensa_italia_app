package it.mensa.shared.integrity

/**
 * Dove, nella costruzione della prova, e' andato storto qualcosa. [wire] e' il
 * nome che va nel tag diagnostico (`attest_key/2`) e nel report al server: e'
 * una lista chiusa lato server, quindi non si inventano nomi nuovi qui.
 */
enum class AttestStage(val wire: String) {
    /** `DCAppAttestService.generateKey`. */
    GENERATE_KEY("generate_key"),

    /** `DCAppAttestService.attestKey`. */
    ATTEST_KEY("attest_key"),

    /** `DCAppAttestService.generateAssertion`. */
    ASSERT("assertion"),

    /** `POST /api/integrity/ios/attest`: il server non ha accettato l'attestazione. */
    REGISTER("register"),

    /** `POST /api/integrity/challenge`. */
    CHALLENGE("challenge"),

    /** Il server ha rifiutato anche la prova appena costruita (secondo 428). */
    SERVER("server"),
}

/** Cosa fare dopo un errore, deciso una volta sola da [classify]. */
enum class Disposition(val wire: String) {
    /** Questo dispositivo non puo' farlo: non si riprova nel processo. */
    UNSUPPORTED("unsupported"),

    /** Guasto passeggero: stessa chiave, niente rotazione, si riprova col backoff. */
    TRANSIENT("transient"),

    /** La chiave non va piu' bene: si butta, se ne registra un'altra. */
    DISCARD_KEY("discard_key"),
}

/**
 * La causa di un fallimento della prova, cosi' com'e' e non com'era comodo
 * leggerla: stage, dominio e codice dell'errore nativo, il suo
 * `NSUnderlyingErrorKey`, e come l'abbiamo classificata. E' quello che prima
 * si buttava via e che ora arriva al messaggio d'errore ([tag]) e al server
 * ([IntegrityReporter]).
 *
 * [keyPhase] (`none`, `pending`, `ready`) e [keyAgeSeconds] (-1 se ignota)
 * descrivono la chiave nel momento dell'errore, prima che il ciclo di vita la
 * tocchi: servono solo al report.
 */
data class ProofFailure(
    val stage: AttestStage,
    val domain: String?,
    val code: Int?,
    val detail: String,
    val underlying: String?,
    val disposition: Disposition,
    val keyPhase: String = KEY_PHASE_NONE,
    val keyAgeSeconds: Long = -1,
) {
    /** `<stage>/<codice>`: `attest_key/2`, `assertion/0`, `register/503`, `attest_key/-2` per il nostro timeout. */
    val tag: String get() = "${stage.wire}/${code ?: "?"}"

    companion object {
        const val KEY_PHASE_NONE = "none"
        const val KEY_PHASE_PENDING = "pending"
        const val KEY_PHASE_READY = "ready"
    }
}

/** Dominio di `DCError` (`DCErrorDomain`). */
internal const val DC_DOMAIN = "com.apple.devicecheck.error"

/** `DCErrorFeatureUnsupported`. */
private const val DC_FEATURE_UNSUPPORTED = 1

/** `DCErrorServerUnavailable`: Apple non risponde. */
private const val DC_SERVER_UNAVAILABLE = 4

private const val NSURL_ERROR_DOMAIN = "NSURLErrorDomain"

/** Il nostro timeout sulle chiamate ad Apple: non e' un `NSError`, lo inventiamo noi. */
internal const val TIMEOUT_DOMAIN = "it.mensa.timeout"
internal const val TIMEOUT_CODE = -2

/** Gli errori che nascono da una risposta HTTP del nostro server (stage `register`, `challenge`, `server`). */
internal const val HTTP_DOMAIN = "http"

/** `DCAppAttestService.isSupported` falso: il dispositivo non puo' fare App Attest, senza che Apple sia stata interpellata. */
internal const val UNSUPPORTED_DOMAIN = "it.mensa.unsupported"

/**
 * La tassonomia degli errori, in un posto solo e senza effetti (si prova in JVM).
 *
 *  - codice 1 (`featureUnsupported`) in qualunque stage: [Disposition.UNSUPPORTED];
 *  - codice 4 (`serverUnavailable`), qualunque errore di `NSURLErrorDomain` e il
 *    nostro timeout: [Disposition.TRANSIENT] (e' la rete, la chiave non c'entra);
 *  - `generateKey`: [Disposition.TRANSIENT] (non c'e' ancora niente da scartare);
 *  - `attestKey`, asserzione e registrazione: [Disposition.DISCARD_KEY], per
 *    QUALUNQUE altro codice. E' la regola di Apple (per ogni errore diverso da
 *    `serverUnavailable` si butta il key id) e non si indovina il codice: il 3
 *    (`invalidKey`) e' solo quello documentato, non l'unico che puo' capitare
 *    a una chiave rimasta indietro (0 e 2 ne sono i candidati);
 *  - sfida e server: [Disposition.TRANSIENT].
 *
 * Il tetto d'eta' della chiave che frena la rotazione sulle asserzioni non e'
 * qui: lo applica [AppAttestLifecycle], perche' dipende da un dato (quando e'
 * nata la chiave) e non dall'errore.
 */
internal fun classify(stage: AttestStage, domain: String?, code: Int?): Disposition {
    if (domain == DC_DOMAIN && code == DC_FEATURE_UNSUPPORTED) return Disposition.UNSUPPORTED
    if (domain == DC_DOMAIN && code == DC_SERVER_UNAVAILABLE) return Disposition.TRANSIENT
    if (domain == NSURL_ERROR_DOMAIN || domain == TIMEOUT_DOMAIN) return Disposition.TRANSIENT
    return when (stage) {
        AttestStage.GENERATE_KEY -> Disposition.TRANSIENT
        AttestStage.ATTEST_KEY, AttestStage.ASSERT, AttestStage.REGISTER -> Disposition.DISCARD_KEY
        AttestStage.CHALLENGE, AttestStage.SERVER -> Disposition.TRANSIENT
    }
}
