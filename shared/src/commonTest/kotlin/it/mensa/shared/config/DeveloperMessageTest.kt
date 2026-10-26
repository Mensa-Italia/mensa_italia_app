package it.mensa.shared.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class DeveloperMessageTest {
    @Test
    fun emptyOrMissingShowsNothing() {
        assertNull(DeveloperMessage.pendingOf(null, null))
        assertNull(DeveloperMessage.pendingOf("", null))
        assertNull(DeveloperMessage.pendingOf("  \n ", null))
    }

    @Test
    fun newMessageShowsTrimmed() {
        assertEquals("# Ciao", DeveloperMessage.pendingOf("\n# Ciao  ", null))
    }

    @Test
    fun lastReadDoesNotComeBackUntilTheTextChanges() {
        val x = "Problema X"
        assertNull(DeveloperMessage.pendingOf(x, DeveloperMessage.fingerprint(x)))
        assertNull(DeveloperMessage.pendingOf(" $x\n", DeveloperMessage.fingerprint(x)))
    }

    @Test
    fun aTextPutBackAfterAnotherShowsAgain() {
        // X, poi Y, poi di nuovo X: tutti e tre compaiono. Conta solo l'ultimo letto.
        val x = "Problema X"
        val y = "Problema Y"
        assertEquals(x, DeveloperMessage.pendingOf(x, null))
        assertEquals(y, DeveloperMessage.pendingOf(y, DeveloperMessage.fingerprint(x)))
        assertEquals(x, DeveloperMessage.pendingOf(x, DeveloperMessage.fingerprint(y)))
    }

    @Test
    fun fingerprintIsStableAndTellsTextsApart() {
        // FNV-1a 64 di "a": valore noto, uguale su ogni piattaforma.
        assertEquals("af63dc4c8601ec8c", DeveloperMessage.fingerprint("a"))
        assertNotEquals(DeveloperMessage.fingerprint("Ciao"), DeveloperMessage.fingerprint("ciao"))
        assertEquals(DeveloperMessage.fingerprint("è"), DeveloperMessage.fingerprint(" è "))
    }
}
