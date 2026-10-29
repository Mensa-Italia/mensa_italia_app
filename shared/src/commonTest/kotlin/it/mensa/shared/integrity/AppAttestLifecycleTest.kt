package it.mensa.shared.integrity

import com.russhwolf.settings.Settings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.resume
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Il ciclo di vita della chiave App Attest, provato con Apple finto
 * ([FakeAppAttestApi]) e senza HTTP: record, adozione dal Keychain, errori
 * classificati, regola dell'eta', timeout.
 */
@OptIn(ExperimentalEncodingApi::class, ExperimentalCoroutinesApi::class)
class AppAttestLifecycleTest {

    private val user = "user-1"

    /** Tutto quello che serve a un ciclo di vita, e il modo di riaverne uno nuovo sullo stesso store (un riavvio). */
    private inner class Rig(
        val api: FakeAppAttestApi = FakeAppAttestApi(),
        val prefs: InMemorySettings = InMemorySettings(),
        val keychain: InMemorySettings = InMemorySettings(),
        var wallClock: Long = START,
    ) {
        val server = FakeEnrollmentServer()
        val store get() = KeyRecordStore(prefs)
        var lifecycle = newLifecycle()

        fun newLifecycle() = AppAttestLifecycle(api, KeyRecordStore(prefs), legacy = keychain, now = { wallClock })

        /** Un riavvio dell'app: stesso container e Keychain, processo nuovo. */
        fun restart() {
            lifecycle = newLifecycle()
        }

        fun record(u: String = user) = store.get(u)
        suspend fun enroll(u: String = user) = lifecycle.ensureEnrolled(u, server)
    }

    private companion object {
        const val START = 1_700_000_000_000L
        const val OLD_POINTER = "integrity.appattest.key.user-1"
    }

    private fun sha256(text: String) = IntegrityClientData.clientDataHash(text)

    // ── Registrazione ────────────────────────────────────────────────────────

    @Test
    fun aFreshEnrollmentGeneratesAttestsRegistersAndMarksTheKeyReady() = runTest {
        val rig = Rig()

        rig.enroll()

        assertEquals(listOf("generateKey", "attestKey:key-1"), rig.api.calls)
        assertEquals(1, rig.server.challenges)
        // clientDataHash dell'attestazione = SHA256(UTF8(challenge)).
        assertContentEquals(sha256("chal-1"), rig.api.attestHashes.single())
        val (keyId, attestation, challenge) = rig.server.registered.single()
        assertEquals("key-1", keyId)
        assertEquals(Base64.encode("att(key-1)".encodeToByteArray()), attestation)
        assertEquals("chal-1", challenge)
        assertEquals(KeyRecord("key-1", KeyPhase.READY, START), rig.record())
    }

    @Test
    fun theRecordIsSavedPendingRightAfterGenerateKeyBeforeAnythingElse() = runTest {
        val rig = Rig()
        var seenWhileChallenging: KeyRecord? = null
        rig.server.onChallenge = { seenWhileChallenging = rig.record() }

        rig.enroll()

        assertEquals(KeyRecord("key-1", KeyPhase.PENDING, START), seenWhileChallenging)
    }

    @Test
    fun aReadyKeyMeansNoAppleCallAndNoChallenge() = runTest {
        val rig = Rig()
        rig.enroll()
        val calls = rig.api.calls.size

        rig.enroll()

        assertEquals(calls, rig.api.calls.size)
        assertEquals(1, rig.server.challenges)
    }

    @Test
    fun everyUserHasHisOwnKey() = runTest {
        val rig = Rig()

        rig.enroll("user-1")
        rig.enroll("user-2")

        assertEquals("key-1", rig.record("user-1")?.keyId)
        assertEquals("key-2", rig.record("user-2")?.keyId)
        assertEquals(2, rig.api.generateKeyCalls)
    }

    @Test
    fun theRecordLivesInThePreferencesNotInTheOldKeychainEntry() = runTest {
        val rig = Rig()
        rig.enroll()

        assertTrue(rig.prefs.keys.single().startsWith("integrity.appattest.v2."))
        assertTrue(rig.keychain.keys.isEmpty())
    }

    @Test
    fun anUnreadableRecordIsNoRecord() = runTest {
        val rig = Rig()
        rig.prefs.putString("integrity.appattest.v2.$user", "{non e' json")

        rig.enroll()

        assertEquals("key-1", rig.record()?.keyId)
    }

    // ── attestKey: cosa si butta e cosa no ───────────────────────────────────

    @Test
    fun aTransientAttestErrorKeepsThePendingKeyAndTheNextTryUsesTheSameOne() = runTest {
        val rig = Rig()
        rig.api.attestErrors += appleError(4)

        val e = assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        assertEquals("attest_key/4", e.failure?.tag)
        assertEquals(Disposition.TRANSIENT, e.failure?.disposition)
        assertEquals(KeyPhase.PENDING, rig.record()?.phase)

        rig.enroll()

        // Nessuna seconda generateKey: attestKey riceve lo STESSO key id, con una sfida nuova.
        assertEquals(listOf("generateKey", "attestKey:key-1", "attestKey:key-1"), rig.api.calls)
        assertEquals(2, rig.server.challenges)
        assertContentEquals(sha256("chal-2"), rig.api.attestHashes.last())
        assertEquals(listOf("key-1"), rig.server.registered.map { it.first })
        assertEquals(KeyPhase.READY, rig.record()?.phase)
    }

    @Test
    fun aNetworkErrorOnAttestKeyIsTransientToo() = runTest {
        val rig = Rig()
        rig.api.attestErrors += appleError(-1009, domain = "NSURLErrorDomain")

        val e = assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        assertEquals("attest_key/-1009", e.failure?.tag)
        assertEquals(KeyPhase.PENDING, rig.record()?.phase)
    }

    @Test
    fun anyOtherAttestErrorDiscardsTheKeyAndTheNextTryGeneratesANewOne() = runTest {
        for (code in listOf(0, 2, 3, 99)) {
            val rig = Rig()
            rig.api.attestErrors += appleError(code)

            val e = assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

            assertEquals("attest_key/$code", e.failure?.tag)
            assertEquals(Disposition.DISCARD_KEY, e.failure?.disposition)
            assertNull(rig.record(), "code $code: record cancellato")

            rig.enroll()

            assertEquals(listOf("generateKey", "attestKey:key-1", "generateKey", "attestKey:key-2"), rig.api.calls, "code $code")
            assertEquals("key-2", rig.record()?.keyId)
        }
    }

    @Test
    fun theRealCauseSurvivesIncludingTheUnderlyingError() = runTest {
        val rig = Rig()
        rig.api.attestErrors += appleError(
            2, detail = "The operation couldn't be completed.", underlying = "NSOSStatusErrorDomain/-1: Invalid appUUID",
        )

        val e = assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        val f = assertNotNull(e.failure)
        assertEquals(DC_DOMAIN, f.domain)
        assertEquals(2, f.code)
        assertEquals("The operation couldn't be completed.", f.detail)
        assertEquals("NSOSStatusErrorDomain/-1: Invalid appUUID", f.underlying)
        assertEquals("pending", f.keyPhase)
        assertEquals(0, f.keyAgeSeconds)
    }

    @Test
    fun featureUnsupportedIsUnsupportedAndClearsTheRecord() = runTest {
        val rig = Rig()
        rig.api.attestErrors += appleError(1)

        val e = assertFailsWith<IntegrityUnsupportedException> { rig.enroll() }

        assertEquals("attest_key/1", e.failure?.tag)
        assertNull(rig.record())
    }

    @Test
    fun generateKeyFailuresAreTransientAndLeaveNoRecord() = runTest {
        val rig = Rig()
        rig.api.generateKeyErrors += appleError(2)

        val e = assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        assertEquals("generate_key/2", e.failure?.tag)
        assertEquals(Disposition.TRANSIENT, e.failure?.disposition)
        assertEquals("none", e.failure?.keyPhase)
        assertEquals(-1, e.failure?.keyAgeSeconds)
        assertNull(rig.record())
        assertEquals(0, rig.server.challenges, "senza chiave non si chiede nemmeno la sfida")
    }

    @Test
    fun generateKeyUnsupportedIsUnsupported() = runTest {
        val rig = Rig()
        rig.api.generateKeyErrors += appleError(1)

        assertFailsWith<IntegrityUnsupportedException> { rig.enroll() }
        assertNull(rig.record())
    }

    // ── Riavvio e cancellazione ──────────────────────────────────────────────

    @Test
    fun aRestartedLifecycleResumesFromPendingWithoutRegeneratingTheKey() = runTest {
        val rig = Rig()
        rig.api.attestErrors += appleError(4)
        assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        rig.restart()
        rig.enroll()

        assertEquals(1, rig.api.generateKeyCalls)
        assertEquals(listOf("generateKey", "attestKey:key-1", "attestKey:key-1"), rig.api.calls)
        assertEquals(KeyPhase.READY, rig.record()?.phase)
    }

    @Test
    fun aRestartedLifecycleWithAReadyKeyCallsNobody() = runTest {
        val rig = Rig()
        rig.enroll()
        val calls = rig.api.calls.size

        rig.restart()
        rig.enroll()

        assertEquals(calls, rig.api.calls.size)
    }

    @Test
    fun cancellingBetweenGenerateKeyAndAttestKeyDoesNotLoseTheKey() = runTest {
        val rig = Rig()
        // La sfida non arriva mai: la richiesta e' sospesa dopo generateKey, prima di attestKey.
        rig.server.onChallenge = { awaitCancellation() }

        val job: Job = launch { rig.enroll() }
        advanceUntilIdle()
        job.cancel()
        job.join()

        assertEquals(KeyRecord("key-1", KeyPhase.PENDING, START), rig.record())
        // Il giro dopo riprende da quella chiave.
        rig.server.onChallenge = {}
        rig.restart()
        rig.enroll()
        assertEquals(listOf("generateKey", "attestKey:key-1"), rig.api.calls)
    }

    // ── Registrazione sul server ─────────────────────────────────────────────

    @Test
    fun aRefusedRegistrationConsumesTheKeyAndNextTimeANewOneIsMade() = runTest {
        val rig = Rig()
        val refusal = ProofFailure(AttestStage.REGISTER, HTTP_DOMAIN, 503, "HTTP 503", null, Disposition.DISCARD_KEY)
        rig.server.onRegister = { throw IntegrityUnavailableException("rifiutata", failure = refusal) }

        val e = assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        // attestKey vale una volta per chiave: il record e' sparito, e la causa dice in che stato era la chiave.
        assertNull(rig.record())
        assertEquals("register/503", e.failure?.tag)
        assertEquals("pending", e.failure?.keyPhase)

        rig.server.onRegister = {}
        rig.enroll()
        assertEquals(listOf("key-2"), rig.server.registered.map { it.first })
    }

    @Test
    fun serverVerdictsAndNetworkErrorsOnRegistrationPassThroughUntouchedAndStillConsumeTheKey() = runTest {
        val errors = listOf(
            IntegrityFailedException(),
            AccountBlockedException(),
            kotlinx.io.IOException("offline"),
        )
        for (error in errors) {
            val rig = Rig()
            rig.server.onRegister = { throw error }

            val thrown = assertFailsWith<Throwable> { rig.enroll() }

            assertEquals(error, thrown)
            assertNull(rig.record(), "$error: chiave consumata")
        }
    }

    @Test
    fun cancellingDuringTheRegistrationConsumesTheKeyToo() = runTest {
        val rig = Rig()
        rig.server.onRegister = { awaitCancellation() }

        val job = launch { rig.enroll() }
        advanceUntilIdle()
        job.cancel()
        job.join()

        // L'attestazione era solo in memoria: la chiave non si puo' piu' attestare.
        assertNull(rig.record())
    }

    // ── Timeout ──────────────────────────────────────────────────────────────

    @Test
    fun anAttestKeyThatNeverAnswersFailsAtExactly30SecondsWithTheTimeoutTag() = runTest {
        val rig = Rig()
        rig.api.onAttestKey = { awaitCancellation() }
        var startedAt = 0L
        rig.server.onChallenge = { startedAt = currentTime }

        val e = assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        assertEquals(30_000L, currentTime - startedAt)
        assertEquals("attest_key/-2", e.failure?.tag)
        assertEquals("it.mensa.timeout", e.failure?.domain)
        assertEquals(Disposition.TRANSIENT, e.failure?.disposition)
        // Transitorio: la chiave resta, il giro dopo e' la stessa.
        assertEquals(KeyPhase.PENDING, rig.record()?.phase)
    }

    @Test
    fun generateKeyAndAssertionHaveTheirOwn30SecondTimeout() = runTest {
        val gen = Rig()
        gen.api.onGenerateKey = { awaitCancellation() }
        val t0 = currentTime
        val g = assertFailsWith<IntegrityUnavailableException> { gen.enroll() }
        assertEquals("generate_key/-2", g.failure?.tag)
        assertEquals(30_000L, currentTime - t0)
        assertNull(gen.record())

        val rig = Rig()
        rig.enroll()
        val hungApi = object : AppAttestApi by rig.api {
            override suspend fun generateAssertion(keyId: String, clientDataHash: ByteArray): ByteArray = awaitCancellation()
        }
        val lifecycle = AppAttestLifecycle(hungApi, KeyRecordStore(rig.prefs), now = { START })
        val t1 = currentTime
        val a = assertFailsWith<IntegrityUnavailableException> { lifecycle.prove(user, "chal|GET|/x") }
        assertEquals("assertion/-2", a.failure?.tag)
        assertEquals(30_000L, currentTime - t1)
        assertEquals("key-1", rig.record()?.keyId, "un timeout non fa ruotare la chiave")
    }

    @Test
    fun anOuterCancellationIsNotMistakenForOurTimeout() = runTest {
        val rig = Rig()
        rig.api.onAttestKey = { awaitCancellation() }

        val job = launch {
            try {
                rig.enroll()
            } catch (e: CancellationException) {
                throw e
            }
        }
        delay(5_000)
        job.cancel()
        job.join()

        assertTrue(job.isCancelled)
        // Nessuna conversione in errore di Apple (non e' un timeout, non c'e' un tag): resta il
        // segno che attestKey era in corso, e di qui Apple potrebbe averla gia' consumata.
        assertEquals(KeyPhase.ATTESTING, rig.record()?.phase)
    }

    // ── attestKey interrotta ─────────────────────────────────────────────────

    @Test
    fun theRecordSaysAttestingWhileAppleIsAttestingAndPendingAgainOnATransientError() = runTest {
        val rig = Rig()
        var seenInsideAttestKey: KeyPhase? = null
        rig.api.onAttestKey = { _ ->
            seenInsideAttestKey = rig.record()?.phase
            throw appleError(4)
        }

        assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        assertEquals(KeyPhase.ATTESTING, seenInsideAttestKey)
        assertEquals(KeyPhase.PENDING, rig.record()?.phase, "Apple non l'ha attestata: la chiave si puo' riprovare")
    }

    /** Una richiesta annullata mentre Apple attesta: la chiave puo' essere gia' consumata, non la si riprova. */
    @Test
    fun aRequestCancelledInsideAttestKeyDoesNotRetryThatKeyNextTime() = runTest {
        val rig = Rig()
        rig.api.onAttestKey = { awaitCancellation() }

        val job = launch { rig.enroll() }
        delay(5_000) // dentro attestKey, prima del timeout di 30 s
        job.cancel()
        job.join()
        assertEquals(KeyRecord("key-1", KeyPhase.ATTESTING, START), rig.record())
        assertTrue(rig.server.registered.isEmpty())

        rig.api.onAttestKey = null
        rig.enroll()

        // Niente secondo attestKey su key-1: si butta e se ne fa una nuova, senza errori inventati da noi.
        assertEquals(listOf("generateKey", "attestKey:key-1", "generateKey", "attestKey:key-2"), rig.api.calls)
        assertEquals(listOf("key-2"), rig.server.registered.map { it.first })
        assertEquals(KeyRecord("key-2", KeyPhase.READY, START), rig.record())
    }

    /** Il processo ucciso dentro attestKey lascia lo stesso segno: al riavvio non si riprova quella chiave. */
    @Test
    fun anAttestingRecordLeftByADeadProcessIsDiscardedAtRestart() = runTest {
        val rig = Rig()
        rig.store.put(user, KeyRecord("dead-key", KeyPhase.ATTESTING, START))

        rig.restart()
        rig.enroll()

        assertEquals(listOf("generateKey", "attestKey:key-1"), rig.api.calls)
        assertEquals(listOf("key-1"), rig.server.registered.map { it.first })
        assertEquals("key-1", rig.record()?.keyId)
    }

    @Test
    fun anAttestingRecordIsReportedAsPending() = runTest {
        val rig = Rig()
        rig.api.onAttestKey = { throw appleError(2) }

        val e = assertFailsWith<IntegrityUnavailableException> { rig.enroll() }

        assertEquals("pending", e.failure?.keyPhase, "la lista chiusa del server e' none, pending, ready")
    }

    /**
     * Il timeout non annulla la chiamata di Apple: se la callback arriva dopo, il
     * `resume` finisce su una continuation gia' annullata, che lo ignora. Qui si
     * ricostruisce quel caso con la stessa primitiva che usa `DcAppAttestApi`.
     */
    @Test
    fun aLateAppleCallbackAfterTheTimeoutIsIgnored() = runTest {
        val rig = Rig()
        var late: kotlin.coroutines.Continuation<ByteArray>? = null
        rig.api.onAttestKey = { suspendCancellableCoroutine { cont -> late = cont } }

        assertFailsWith<IntegrityUnavailableException> { rig.enroll() }
        // Apple risponde dopo il timeout: nessuna eccezione, nessun effetto.
        late!!.resume("tardi".encodeToByteArray())

        assertEquals(KeyPhase.PENDING, rig.record()?.phase)
        assertTrue(rig.server.registered.isEmpty())
    }

    // ── Asserzione ───────────────────────────────────────────────────────────

    @Test
    fun anAssertionReturnsTheKeyIdAndTheBase64AssertionOverTheClientData() = runTest {
        val rig = Rig()
        rig.enroll()

        val headers = rig.lifecycle.prove(user, "chal-2|GET|/api/cs/me")

        assertEquals("key-1", headers[IntegrityHeaders.KEY_ID])
        assertEquals(Base64.encode("asr(key-1)".encodeToByteArray()), headers[IntegrityHeaders.ASSERTION])
        assertEquals(2, headers.size)
    }

    @Test
    fun anAssertionOnTheWrongStateAsksForANewKey() = runTest {
        val rig = Rig()
        // Nessun record: non c'e' niente su cui firmare.
        assertFailsWith<IntegrityKeyInvalidException> { rig.lifecycle.prove(user, "x") }
    }

    @Test
    fun transientAndUnsupportedAssertionErrorsKeepTheKey() = runTest {
        val rig = Rig()
        rig.enroll()
        rig.wallClock += 5.hours.inWholeMilliseconds // vecchia: se fosse da scartare, si scarterebbe

        rig.api.assertionErrors += appleError(4)
        val transient = assertFailsWith<IntegrityUnavailableException> { rig.lifecycle.prove(user, "x") }
        assertEquals("assertion/4", transient.failure?.tag)

        rig.api.assertionErrors += appleError(-1001, domain = "NSURLErrorDomain")
        assertFailsWith<IntegrityUnavailableException> { rig.lifecycle.prove(user, "x") }

        rig.api.assertionErrors += appleError(1)
        val unsupported = assertFailsWith<IntegrityUnsupportedException> { rig.lifecycle.prove(user, "x") }
        assertEquals("assertion/1", unsupported.failure?.tag)

        assertEquals("key-1", rig.record()?.keyId, "la chiave non si e' mossa")
    }

    // ── Regola dell'eta' ─────────────────────────────────────────────────────

    @Test
    fun anAssertionThatFailsOnAKeyOlderThanAnHourRotatesItWhateverTheCode() = runTest {
        for (code in listOf(0, 2, 3, 99)) {
            val rig = Rig()
            rig.enroll()
            rig.wallClock += 1.hours.inWholeMilliseconds
            rig.api.assertionErrors += appleError(code, underlying = "sotto/$code")

            val e = assertFailsWith<IntegrityKeyInvalidException> { rig.lifecycle.prove(user, "x") }

            assertEquals("key-1", e.keyId)
            assertEquals("assertion/$code", e.failure?.tag, "code $code")
            assertEquals("sotto/$code", e.failure?.underlying)
            assertEquals("ready", e.failure?.keyPhase)
            assertEquals(3600, e.failure?.keyAgeSeconds)
        }
    }

    /** Una chiave nuova che non firma non e' una chiave stantia: e' il dispositivo. */
    @Test
    fun anAssertionThatFailsOnAYoungKeyIsADeviceFaultNotARotation() = runTest {
        val rig = Rig()
        rig.enroll()
        rig.wallClock += 59.minutes.inWholeMilliseconds + 59_000
        rig.api.assertionErrors += appleError(2)

        val e = assertFailsWith<IntegrityUnavailableException> { rig.lifecycle.prove(user, "x") }

        assertEquals("assertion/2", e.failure?.tag)
        // Riportata per quello che e' diventata: nessun scarto.
        assertEquals(Disposition.TRANSIENT, e.failure?.disposition)
        assertEquals("key-1", rig.record()?.keyId)
        assertEquals(1, rig.api.generateKeyCalls, "nessuna seconda chiave")
    }

    /**
     * L'orologio di sistema torna indietro dopo la nascita della chiave (data
     * manuale, sincronizzazione sbagliata): l'eta' viene negativa. Non e' "giovane":
     * sarebbe un freno alla rotazione fino a `createdAt + 1 ora`, e una chiave davvero
     * stantia non si ruoterebbe piu'.
     */
    @Test
    fun aKeyBornInTheFutureBecauseTheClockWentBackIsTreatedAsOld() = runTest {
        val rig = Rig()
        rig.enroll()
        rig.wallClock -= 1.hours.inWholeMilliseconds * 24
        rig.api.assertionErrors += appleError(2)

        val e = assertFailsWith<IntegrityKeyInvalidException> { rig.lifecycle.prove(user, "x") }

        assertEquals("key-1", e.keyId)
        assertEquals("assertion/2", e.failure?.tag)
        assertEquals(-1, e.failure?.keyAgeSeconds, "un'eta' negativa e' ignota, non zero")
    }

    // ── Migrazione dal Keychain ──────────────────────────────────────────────

    @Test
    fun theOldKeychainKeyIsAdoptedAsReadyAndOldAndRemovedFromTheKeychain() = runTest {
        val rig = Rig()
        rig.keychain.putString(OLD_POINTER, "old-key")

        rig.enroll()

        // Adottata, non buttata: chi ha una chiave buona non si ri-registra.
        assertEquals(KeyRecord("old-key", KeyPhase.READY, 0), rig.record())
        assertNull(rig.keychain.getStringOrNull(OLD_POINTER))
        assertTrue(rig.api.calls.isEmpty())
        assertEquals(0, rig.server.challenges)
        assertEquals("old-key", rig.lifecycle.prove(user, "x")[IntegrityHeaders.KEY_ID])
    }

    @Test
    fun theMigrationRunsOnceAndNeverReadoptsADiscardedKey() = runTest {
        val rig = Rig()
        rig.keychain.putString(OLD_POINTER, "old-key")
        rig.enroll()
        rig.lifecycle.discardKey(user, "old-key")
        assertNull(rig.record())

        // Scartata la chiave adottata se ne registra una nuova: non torna la vecchia.
        rig.enroll()

        assertEquals("key-1", rig.record()?.keyId)
        // Se per qualunque ragione la voce vecchia ricomparisse (un Keychain che non
        // ha mai rimosso), con un record gia' READY si toglie senza adottarla.
        rig.restart()
        rig.keychain.putString(OLD_POINTER, "old-key")
        rig.enroll()
        assertEquals("key-1", rig.record()?.keyId)
        assertNull(rig.keychain.getStringOrNull(OLD_POINTER))
    }

    @Test
    fun aBlankOldEntryIsNotAKey() = runTest {
        val rig = Rig()
        rig.keychain.putString(OLD_POINTER, "")

        rig.enroll()

        assertEquals("key-1", rig.record()?.keyId)
    }

    /** Il telefono del titolare: un puntatore vecchio a una chiave che l'OS non ha piu'. */
    @Test
    fun aStaleAdoptedKeyFailingWithAnyCodeIsRotatedExactlyOnce() = runTest {
        for (code in listOf(0, 2, 3)) {
            val rig = Rig()
            rig.keychain.putString(OLD_POINTER, "stale")
            rig.api.brokenKeys["stale"] = appleError(code, underlying = "Invalid appUUID")
            rig.enroll()

            val e = assertFailsWith<IntegrityKeyInvalidException> { rig.lifecycle.prove(user, "x") }
            // L'errore e' quello VERO della chiave vecchia, e la sua eta' e' ignota.
            assertEquals("assertion/$code", e.failure?.tag)
            assertEquals("Invalid appUUID", e.failure?.underlying)
            assertEquals("ready", e.failure?.keyPhase)
            assertEquals(-1, e.failure?.keyAgeSeconds)

            // Quello che fa il guard: scarta quella chiave, registra, riprova.
            rig.lifecycle.discardKey(user, e.keyId)
            rig.enroll()
            val headers = rig.lifecycle.prove(user, "y")

            assertEquals("key-1", headers[IntegrityHeaders.KEY_ID], "code $code")
            assertEquals(1, rig.api.generateKeyCalls, "code $code: una sola chiave nuova")
        }
    }

    // ── Scarto: compare-and-set ──────────────────────────────────────────────

    @Test
    fun discardingIsACompareAndSetOnTheKeyId() = runTest {
        val rig = Rig()
        rig.enroll()

        rig.lifecycle.discardKey(user, "another-key")
        assertEquals("key-1", rig.record()?.keyId)

        rig.lifecycle.discardKey(user, "key-1")
        assertNull(rig.record())
    }

    @Test
    fun aNullKeyIdDiscardsWhateverIsThere() = runTest {
        val rig = Rig()
        rig.enroll()

        rig.lifecycle.discardKey(user, null)

        assertNull(rig.record())
        rig.lifecycle.discardKey(user, null) // senza record e' un no-op
    }

    /** Due prove che falliscono insieme sulla stessa chiave vecchia: una sola rotazione. */
    @Test
    fun twoProofsFailingOnTheSameOldKeyCauseASingleRotation() = runTest {
        val rig = Rig()
        rig.keychain.putString(OLD_POINTER, "old")
        rig.api.brokenKeys["old"] = appleError(2)
        rig.enroll()
        val a = assertFailsWith<IntegrityKeyInvalidException> { rig.lifecycle.prove(user, "a") }
        val b = assertFailsWith<IntegrityKeyInvalidException> { rig.lifecycle.prove(user, "b") }

        // La prima scarta e registra; la seconda arriva tardi: il compare-and-set non tocca la chiave nuova.
        rig.lifecycle.discardKey(user, a.keyId)
        rig.enroll()
        rig.lifecycle.discardKey(user, b.keyId)
        rig.enroll()

        assertEquals(1, rig.api.generateKeyCalls)
        assertEquals("key-1", rig.record()?.keyId)
        assertEquals(KeyPhase.READY, rig.record()?.phase)
    }

    /** Una rimozione che non attacca e' un errore, non qualcosa da inghiottire. */
    @Test
    fun aFailingRemovalIsNotSwallowed() = runTest {
        val backing = InMemorySettings()
        val prefs = object : Settings by backing {
            override fun remove(key: String) = throw IllegalStateException("rimozione fallita")
        }
        val lifecycle = AppAttestLifecycle(FakeAppAttestApi(), KeyRecordStore(prefs), now = { START })
        lifecycle.ensureEnrolled(user, FakeEnrollmentServer())

        assertFailsWith<IllegalStateException> { lifecycle.discardKey(user, "key-1") }
    }
}
