package it.mensa.shared.integrity

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import platform.Foundation.NSLocalizedDescriptionKey
import platform.Foundation.NSUnderlyingErrorKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * La traduzione `NSError` -> [AppAttestNativeError], l'unico pezzo di logica
 * rimasto in appleMain: dominio, codice, descrizione e `NSUnderlyingErrorKey`
 * (dove comparirebbe "Invalid appUUID"). Gira sul simulatore.
 */
@OptIn(ExperimentalForeignApi::class)
class AppleErrorTranslationTest {

    @Test
    fun domainCodeAndDescriptionAreKept() {
        val error = NSError.errorWithDomain(
            "com.apple.devicecheck.error", code = 2, userInfo = mapOf(NSLocalizedDescriptionKey to "The operation couldn't be completed."),
        )

        val native = error.toNativeError()

        assertEquals("com.apple.devicecheck.error", native.domain)
        assertEquals(2, native.code)
        assertEquals("The operation couldn't be completed.", native.detail)
        assertNull(native.underlying)
    }

    @Test
    fun theUnderlyingErrorBecomesDomainCodeAndDescription() {
        val inner = NSError.errorWithDomain(
            "NSOSStatusErrorDomain", code = -1, userInfo = mapOf(NSLocalizedDescriptionKey to "Invalid appUUID"),
        )
        val error = NSError.errorWithDomain(
            "com.apple.devicecheck.error", code = 0, userInfo = mapOf(NSUnderlyingErrorKey to inner),
        )

        val native = error.toNativeError()

        assertEquals(0, native.code)
        assertEquals("NSOSStatusErrorDomain/-1: Invalid appUUID", native.underlying)
    }

    @Test
    fun anErrorWithoutUserInfoStillHasADescription() {
        val native = NSError.errorWithDomain("NSURLErrorDomain", code = -1009, userInfo = null).toNativeError()

        assertEquals("NSURLErrorDomain", native.domain)
        assertEquals(-1009, native.code)
        assertNotNull(native.detail)
    }

    @Test
    fun aMissingErrorIsAnUnknownOne() {
        val native = (null as NSError?).toNativeError()

        assertNull(native.domain)
        assertNull(native.code)
        assertEquals("errore sconosciuto", native.detail)
    }

    @Test
    fun theTranslatedErrorsAreClassifiedLikeTheJvmTableSays() {
        val unsupported = NSError.errorWithDomain("com.apple.devicecheck.error", code = 1, userInfo = null).toNativeError()
        val offline = NSError.errorWithDomain("NSURLErrorDomain", code = -1009, userInfo = null).toNativeError()

        assertEquals(Disposition.UNSUPPORTED, classify(AttestStage.ATTEST_KEY, unsupported.domain, unsupported.code))
        assertEquals(Disposition.TRANSIENT, classify(AttestStage.ASSERT, offline.domain, offline.code))
    }
}
