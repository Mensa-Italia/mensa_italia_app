package it.mensa.shared.integrity

import com.russhwolf.settings.Settings
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

/**
 * Dove e' arrivata una chiave: generata e mai data ad Apple ([PENDING]), con
 * `attestKey` iniziata e non conclusa ([ATTESTING]), o registrata ([READY]).
 */
@Serializable
internal enum class KeyPhase {
    PENDING,

    /**
     * Scritta subito prima di `attestKey` e tolta solo quando l'esito e' noto.
     * Se un record resta cosi' (richiesta annullata, processo ucciso a meta') non
     * si sa se Apple ha gia' attestato la chiave, e una chiave si attesta una
     * volta sola: la si butta invece di riprovarla.
     */
    ATTESTING,
    READY,
}

/**
 * Il fatto persistito su una chiave App Attest: [keyId], a che punto e'
 * ([phase]) e quando e' nata ([createdAtMillis], orologio di sistema).
 * `createdAtMillis = 0` vuol dire "vecchia, non so di quando": e' quello che
 * hanno le chiavi adottate dal vecchio Keychain.
 */
@Serializable
internal data class KeyRecord(
    val keyId: String,
    val phase: KeyPhase,
    val createdAtMillis: Long,
)

/**
 * Il record della chiave di ogni utente, in un JSON sotto
 * `integrity.appattest.v2.<userId>`.
 *
 * Il [Settings] che gli si passa DEVE stare nel container dell'app
 * (`NSUserDefaults`), non nel Keychain: la chiave App Attest sta nel Secure
 * Enclave e muore con l'installazione, e il puntatore deve morire con lei. Il
 * Keychain sopravvive alla cancellazione dell'app (e' proprio il guaio del
 * vecchio `integrity.appattest.key.<utente>`).
 *
 * Tutte sincrone e senza `runCatching`: una rimozione che non attacca e' un
 * errore da vedere, non da inghiottire. L'unica tolleranza e' in lettura, dove
 * un JSON illeggibile vale "nessun record".
 */
internal class KeyRecordStore(private val settings: Settings) {

    fun get(userId: String): KeyRecord? {
        val raw = settings.getStringOrNull(name(userId))?.takeIf { it.isNotBlank() } ?: return null
        return try {
            json.decodeFromString(KeyRecord.serializer(), raw)
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    fun put(userId: String, record: KeyRecord) {
        settings.putString(name(userId), json.encodeToString(KeyRecord.serializer(), record))
    }

    fun remove(userId: String) {
        settings.remove(name(userId))
    }

    private fun name(userId: String) = "$NAME_PREFIX$userId"

    private companion object {
        const val NAME_PREFIX = "integrity.appattest.v2."
        val json = Json { ignoreUnknownKeys = true }
    }
}

/**
 * L'unico proprietario del ciclo di vita della chiave App Attest: dove sta il
 * key id, a che punto e', quando va rifatta e perche'. Prima questi fatti erano
 * sparsi in quattro posti che non si conoscevano (il key id nel Keychain, un
 * mutex, un'epoca, un flag nel guard) e la tassonomia degli errori Apple era un
 * solo `if`.
 *
 * Il modello: un utente ha al piu' UN record `{keyId, phase, createdAt}`.
 *
 *  - nessun record: [ensureEnrolled] genera una chiave e SUBITO salva il record
 *    `PENDING`, prima di qualunque altra sospensione, cosi' una richiesta
 *    cancellata a meta' non perde la chiave;
 *  - `PENDING`: la chiave esiste ma Apple non l'ha ancora attestata. Si
 *    riprende da `attestKey` con lo STESSO key id (un errore di rete di Apple
 *    non brucia chiavi: al tentativo dopo e' la stessa chiave con una sfida nuova);
 *  - `ATTESTING`: `attestKey` e' partita e non ha un esito. Dura quanto la
 *    chiamata; se un record e' trovato cosi' (richiesta annullata, processo
 *    ucciso) Apple potrebbe aver gia' attestato la chiave, e l'attestazione vale
 *    una volta: il record si butta e si riparte da una chiave nuova;
 *  - `READY`: registrata. [prove] firma con lei.
 *
 * Ogni errore di Apple e' classificato UNA volta da [classify]. `attestKey`
 * vale una volta per chiave: se Apple risponde ma poi la registrazione sul
 * server fallisce per qualunque motivo (o la richiesta viene annullata mentre
 * Apple attesta), la chiave e' consumata e il record si cancella.
 *
 * **Rotazione.** Un'asserzione che fallisce con un errore non transitorio
 * (qualunque, non solo `invalidKey`) vuol dire "cambia la chiave": e' la regola
 * di Apple. Ma una chiave nata da meno di un'ora che non sa firmare non e' una
 * chiave stantia, e' il dispositivo: li' la rotazione non scatta e l'errore e'
 * un guasto ([IntegrityUnavailableException]) che il guard ricorda col backoff.
 * E' il freno strutturale contro un'altra cascata di chiavi (18 in tre ore
 * sul telefono del titolare). Una chiave adottata dal vecchio Keychain ha
 * `createdAt = 0`, cioe' e' vecchia: la sua prima asserzione fallita la ruota.
 * Anche una `createdAt` nel futuro (l'orologio e' tornato indietro dopo la
 * nascita della chiave) vale "vecchia": un'eta' negativa non e' un'eta', e
 * trattarla da giovane bloccherebbe la rotazione finche' l'orologio non
 * raggiunge `createdAt + 1 ora`.
 *
 * **Concorrenza.** Nessun mutex qui: ogni chiamata arriva dal guard sotto il
 * suo `sessionLock`, una alla volta. `discardKey` e' un compare-and-set sul
 * key id al posto dell'epoca di prima, senza stato da propagare per le firme.
 *
 * **Timeout.** Ogni chiamata ad Apple ha [appleTimeout]; allo scadere diventa
 * un [AppAttestNativeError] di dominio [TIMEOUT_DOMAIN] e codice [TIMEOUT_CODE],
 * transitorio. Non annulla la chiamata di Apple: se la callback arriva dopo, il
 * risultato e' perso (una continuation gia' annullata ignora il resume).
 */
@OptIn(ExperimentalEncodingApi::class)
internal class AppAttestLifecycle(
    private val api: AppAttestApi,
    private val store: KeyRecordStore,
    /** Il vecchio Keychain: ci sta `integrity.appattest.key.<utente>` delle versioni fino alla 22.2.20. */
    private val legacy: Settings? = null,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    private val appleTimeout: Duration = APPLE_TIMEOUT,
) {
    private var migrated: Set<String> = emptySet()

    suspend fun isSupported(): Boolean = api.isSupported()

    /**
     * Porta la chiave di [userId] a `READY`, riprendendo da dove si era fermata.
     * Lancia solo [IntegrityUnsupportedException] / [IntegrityUnavailableException]
     * con il loro [ProofFailure], e le eccezioni di [server] cosi' come sono.
     */
    suspend fun ensureEnrolled(userId: String, server: EnrollmentServer) {
        adoptLegacyKey(userId)
        var record = store.get(userId)
        if (record?.phase == KeyPhase.READY) return
        if (record?.phase == KeyPhase.ATTESTING) {
            // Un tentativo e' morto dentro attestKey: potrebbe averla gia' consumata.
            discardKey(userId, record.keyId)
            record = null
        }

        if (record == null) {
            val keyId = try {
                timed { api.generateKey() }
            } catch (e: AppAttestNativeError) {
                throw exceptionOf(failureOf(AttestStage.GENERATE_KEY, e, null), e)
            }
            // SUBITO, prima di qualunque altra sospensione: se la richiesta viene
            // cancellata da qui in poi, la chiave non si perde.
            record = KeyRecord(keyId, KeyPhase.PENDING, now())
            store.put(userId, record)
        }
        val keyId = record.keyId

        val challenge = server.challenge()
        // Da qui Apple puo' attestare la chiave senza che noi lo sappiamo (una
        // richiesta annullata o un processo ucciso non fermano la sua chiamata): il
        // record lo dice finche' non c'e' un esito.
        record = record.copy(phase = KeyPhase.ATTESTING)
        store.put(userId, record)
        val attestation = try {
            timed { api.attestKey(keyId, IntegrityClientData.clientDataHash(challenge)) }
        } catch (e: AppAttestNativeError) {
            val failure = failureOf(AttestStage.ATTEST_KEY, e, record)
            // Transitorio (4, rete, timeout): Apple non l'ha attestata, torna PENDING e
            // al giro dopo e' la stessa chiave. Qualunque altro errore: la chiave e' da buttare.
            if (failure.disposition == Disposition.TRANSIENT) {
                store.put(userId, record.copy(phase = KeyPhase.PENDING))
            } else {
                discardKey(userId, keyId)
            }
            throw exceptionOf(failure, e)
        }

        // Da qui `attestKey` e' consumata: comunque vada (rifiuto, 5xx, rete,
        // cancellazione) la chiave non si puo' piu' attestare. Si cancella il
        // record e si rilancia; il rifiuto del server con la sua causa prende la
        // fase e l'eta' della chiave, tutto il resto (AccountBlocked,
        // IntegrityFailed, le IOException) passa intatto.
        try {
            server.register(keyId, Base64.encode(attestation), challenge)
        } catch (e: IntegrityUnavailableException) {
            discardKey(userId, keyId)
            val failure = e.failure ?: throw e
            throw IntegrityUnavailableException(
                e.message.orEmpty(), e.cause,
                failure.copy(keyPhase = phaseOf(record), keyAgeSeconds = ageOf(record)),
            )
        } catch (t: Throwable) {
            discardKey(userId, keyId)
            throw t
        }
        store.put(userId, record.copy(phase = KeyPhase.READY))
    }

    /**
     * L'asserzione per [clientData] con la chiave `READY` di [userId]:
     * `X-Integrity-Key-Id` e `X-Integrity-Assertion`.
     *
     * Su errore, classificato: [Disposition.UNSUPPORTED] -> [IntegrityUnsupportedException];
     * [Disposition.TRANSIENT] -> [IntegrityUnavailableException]; [Disposition.DISCARD_KEY]
     * -> [IntegrityKeyInvalidException] se la chiave ha almeno un'ora, altrimenti
     * [IntegrityUnavailableException] (vedi la classe).
     */
    suspend fun prove(userId: String, clientData: String): Map<String, String> {
        val record = store.get(userId)?.takeIf { it.phase == KeyPhase.READY }
            ?: throw IntegrityKeyInvalidException(keyId = "", failure = null)
        val keyId = record.keyId

        val assertion = try {
            timed { api.generateAssertion(keyId, IntegrityClientData.clientDataHash(clientData)) }
        } catch (e: AppAttestNativeError) {
            val failure = failureOf(AttestStage.ASSERT, e, record)
            throw when {
                failure.disposition != Disposition.DISCARD_KEY -> exceptionOf(failure, e)
                isOld(record) -> IntegrityKeyInvalidException(keyId, failure, e)
                // Una chiave giovane che non firma: il guasto e' del dispositivo.
                else -> exceptionOf(failure.copy(disposition = Disposition.TRANSIENT), e)
            }
        }
        return mapOf(
            IntegrityHeaders.KEY_ID to keyId,
            IntegrityHeaders.ASSERTION to Base64.encode(assertion),
        )
    }

    /**
     * Butta il record di [userId] SOLO se la sua chiave e' ancora [keyId]
     * (compare-and-set): se un'altra richiesta l'ha gia' sostituita non si butta
     * la chiave nuova. Con [keyId] null butta quella che c'e'. La rimozione non
     * si inghiotte: se non attacca, e' un errore.
     */
    fun discardKey(userId: String, keyId: String?) {
        val current = store.get(userId) ?: return
        if (keyId == null || current.keyId == keyId) store.remove(userId)
    }

    // ── Dettagli ─────────────────────────────────────────────────────────────

    /**
     * Migrazione una tantum dal Keychain: la chiave che le versioni fino alla
     * 22.2.20 salvavano sotto `integrity.appattest.key.<utente>` si ADOTTA come
     * `READY` con `createdAt = 0` (vecchia) e si toglie dal Keychain. Non si
     * butta: chi ha una chiave buona non deve registrarsi di nuovo. E se il
     * puntatore e' stantio (la chiave non c'e' piu') la prima asserzione fallisce,
     * la rotazione scatta, e il report porta l'errore REALE di Apple.
     *
     * La voce vecchia si toglie anche se un record nuovo c'e' gia': non deve
     * poter essere riadottata dopo. La rimozione e' best-effort (il record
     * nuovo e' gia' scritto; al prossimo avvio si riprova).
     */
    private fun adoptLegacyKey(userId: String) {
        val keychain = legacy ?: return
        if (userId in migrated) return
        val name = "$LEGACY_PREFIX$userId"
        val read = runCatching { keychain.getStringOrNull(name) }
        if (read.isFailure) return // Keychain illeggibile adesso (telefono bloccato): si riprova al giro dopo
        migrated = migrated + userId
        val old = read.getOrNull()?.takeIf { it.isNotBlank() } ?: return
        if (store.get(userId) == null) store.put(userId, KeyRecord(old, KeyPhase.READY, createdAtMillis = 0))
        runCatching { keychain.remove(name) }
    }

    private suspend fun <T : Any> timed(block: suspend () -> T): T =
        withTimeoutOrNull(appleTimeout) { block() } ?: throw AppAttestNativeError(
            domain = TIMEOUT_DOMAIN,
            code = TIMEOUT_CODE,
            detail = "Apple non ha risposto entro ${appleTimeout.inWholeSeconds} s",
            underlying = null,
        )

    private fun failureOf(stage: AttestStage, e: AppAttestNativeError, record: KeyRecord?) = ProofFailure(
        stage = stage,
        domain = e.domain,
        code = e.code,
        detail = e.detail,
        underlying = e.underlying,
        disposition = classify(stage, e.domain, e.code),
        keyPhase = phaseOf(record),
        keyAgeSeconds = ageOf(record),
    )

    private fun exceptionOf(failure: ProofFailure, cause: Throwable?): IntegrityException =
        if (failure.disposition == Disposition.UNSUPPORTED) {
            IntegrityUnsupportedException(
                "App Attest non supportato su questo dispositivo (${failure.tag}): ${failure.detail}", cause, failure,
            )
        } else {
            IntegrityUnavailableException("App Attest (${failure.tag}): ${failure.detail}", cause, failure)
        }

    /** Per il report la lista chiusa e' `none`, `pending`, `ready`: `ATTESTING` e' una `PENDING` che Apple sta attestando. */
    private fun phaseOf(record: KeyRecord?): String = when (record?.phase) {
        null -> ProofFailure.KEY_PHASE_NONE
        KeyPhase.PENDING, KeyPhase.ATTESTING -> ProofFailure.KEY_PHASE_PENDING
        KeyPhase.READY -> ProofFailure.KEY_PHASE_READY
    }

    /** Eta' in secondi, -1 se ignota (nessuna chiave, adottata dal vecchio Keychain, o nata "nel futuro"). */
    private fun ageOf(record: KeyRecord?): Long {
        if (record == null || record.createdAtMillis <= 0) return -1
        val age = now() - record.createdAtMillis
        return if (age < 0) -1 else age / 1000
    }

    /** Vecchia abbastanza da ruotare. Un'eta' negativa (orologio tornato indietro) e' implausibile e vale "vecchia". */
    private fun isOld(record: KeyRecord): Boolean {
        val age = now() - record.createdAtMillis
        return age < 0 || age >= MIN_KEY_AGE_FOR_ROTATION.inWholeMilliseconds
    }

    companion object {
        /** Il timeout di ogni chiamata ad Apple. */
        val APPLE_TIMEOUT: Duration = 30.seconds

        /** Sotto questa eta' un'asserzione fallita non fa ruotare la chiave. */
        val MIN_KEY_AGE_FOR_ROTATION: Duration = 1.hours

        internal const val LEGACY_PREFIX = "integrity.appattest.key."
    }
}
