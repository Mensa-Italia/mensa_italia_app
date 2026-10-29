package it.mensa.shared.integrity

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La tabella di [classify], per intero: stage x errore. E' una funzione pura, e
 * sta in commonMain proprio perche' qui si possa provare in JVM (prima era un
 * `if` dentro il provider Kotlin/Native).
 */
class ClassifyTest {

    private val appleStages = listOf(AttestStage.GENERATE_KEY, AttestStage.ATTEST_KEY, AttestStage.ASSERT)

    private fun every(
        domain: String?,
        code: Int?,
        expected: Disposition,
        stages: List<AttestStage> = AttestStage.entries,
    ) = stages.forEach { stage ->
        assertEquals(expected, classify(stage, domain, code), "$stage $domain/$code")
    }

    // ── Uguale in ogni stage ─────────────────────────────────────────────────

    @Test
    fun featureUnsupportedIsUnsupportedInEveryStage() = every(DC_DOMAIN, 1, Disposition.UNSUPPORTED)

    @Test
    fun serverUnavailableIsTransientInEveryStage() = every(DC_DOMAIN, 4, Disposition.TRANSIENT)

    @Test
    fun anyNetworkErrorIsTransientInEveryStage() {
        listOf(-1009, -1001, -1004, -1200, 1, 3, 0).forEach { every("NSURLErrorDomain", it, Disposition.TRANSIENT) }
    }

    @Test
    fun ourOwnTimeoutIsTransientInEveryStage() = every(TIMEOUT_DOMAIN, TIMEOUT_CODE, Disposition.TRANSIENT)

    // ── Dipende dallo stage ──────────────────────────────────────────────────

    /** Il 3 (`invalidKey`) e' quello documentato, ma NON l'unico: 0 e 2 sono candidati e ogni altro codice vale lo stesso. */
    @Test
    fun anyOtherDeviceCheckCodeDiscardsTheKeyOnAttestAndAssertion() {
        listOf(0, 2, 3, 5, 99, -1).forEach { code ->
            every(DC_DOMAIN, code, Disposition.DISCARD_KEY, listOf(AttestStage.ATTEST_KEY, AttestStage.ASSERT))
        }
    }

    @Test
    fun theThreeAndTheTwoOnAssertionAreDiscardKey() {
        assertEquals(Disposition.DISCARD_KEY, classify(AttestStage.ASSERT, DC_DOMAIN, 3))
        assertEquals(Disposition.DISCARD_KEY, classify(AttestStage.ASSERT, DC_DOMAIN, 2))
        assertEquals(Disposition.DISCARD_KEY, classify(AttestStage.ASSERT, DC_DOMAIN, 0))
    }

    /** Alla generazione non c'e' ancora niente da scartare. */
    @Test
    fun generateKeyFailuresAreTransientWhateverTheCode() {
        listOf(0, 2, 3, 5, 99).forEach {
            assertEquals(Disposition.TRANSIENT, classify(AttestStage.GENERATE_KEY, DC_DOMAIN, it), "code $it")
        }
    }

    @Test
    fun aRegistrationTheServerRefusedConsumesTheKey() {
        listOf(400, 403, 500, 503).forEach {
            assertEquals(Disposition.DISCARD_KEY, classify(AttestStage.REGISTER, HTTP_DOMAIN, it), "HTTP $it")
        }
    }

    @Test
    fun challengeAndServerFailuresAreTransient() {
        listOf(AttestStage.CHALLENGE, AttestStage.SERVER).forEach { stage ->
            listOf(428, 429, 500, 503).forEach {
                assertEquals(Disposition.TRANSIENT, classify(stage, HTTP_DOMAIN, it), "$stage $it")
            }
        }
    }

    // ── Cio' che non e' DeviceCheck ──────────────────────────────────────────

    /** I codici 1 e 4 hanno quel significato solo nel dominio DeviceCheck. */
    @Test
    fun codesOneAndFourMeanNothingOutsideTheDeviceCheckDomain() {
        appleStages.forEach { stage ->
            val byStage = classify(stage, "NSPOSIXErrorDomain", 0)
            assertEquals(byStage, classify(stage, "NSPOSIXErrorDomain", 1), "$stage code 1")
            assertEquals(byStage, classify(stage, "NSPOSIXErrorDomain", 4), "$stage code 4")
        }
    }

    @Test
    fun anUnknownDomainFallsBackToTheStageDefault() {
        assertEquals(Disposition.TRANSIENT, classify(AttestStage.GENERATE_KEY, "com.example", 7))
        assertEquals(Disposition.DISCARD_KEY, classify(AttestStage.ATTEST_KEY, "com.example", 7))
        assertEquals(Disposition.DISCARD_KEY, classify(AttestStage.ASSERT, "com.example", 7))
    }

    @Test
    fun anErrorWithoutDomainOrCodeFallsBackToTheStageDefault() {
        assertEquals(Disposition.TRANSIENT, classify(AttestStage.GENERATE_KEY, null, null))
        assertEquals(Disposition.DISCARD_KEY, classify(AttestStage.ATTEST_KEY, null, null))
        assertEquals(Disposition.DISCARD_KEY, classify(AttestStage.ASSERT, null, null))
    }

    // ── Il tag ───────────────────────────────────────────────────────────────

    @Test
    fun tagIsStageSlashCodeWithTheWireNames() {
        fun tag(stage: AttestStage, code: Int?) = ProofFailure(stage, null, code, "", null, Disposition.TRANSIENT).tag
        assertEquals("attest_key/2", tag(AttestStage.ATTEST_KEY, 2))
        assertEquals("generate_key/0", tag(AttestStage.GENERATE_KEY, 0))
        assertEquals("assertion/3", tag(AttestStage.ASSERT, 3))
        assertEquals("register/503", tag(AttestStage.REGISTER, 503))
        assertEquals("challenge/500", tag(AttestStage.CHALLENGE, 500))
        assertEquals("server/428", tag(AttestStage.SERVER, 428))
        // Il nostro timeout.
        assertEquals("attest_key/-2", tag(AttestStage.ATTEST_KEY, TIMEOUT_CODE))
        assertEquals("assertion/?", tag(AttestStage.ASSERT, null))
    }

    /** I nomi di filo sono una lista chiusa lato server: cambiarli senza il server li fa diventare "other". */
    @Test
    fun wireNamesAreTheServerClosedLists() {
        assertEquals(
            listOf("generate_key", "attest_key", "assertion", "register", "challenge", "server"),
            AttestStage.entries.map { it.wire },
        )
        assertEquals(listOf("unsupported", "transient", "discard_key"), Disposition.entries.map { it.wire })
    }
}
