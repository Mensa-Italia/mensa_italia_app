package it.mensa.shared.config

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class DeveloperMessageTest {
    @Test
    fun emptyOrMissingShowsNothing() {
        assertNull(DeveloperMessage.pendingOf(null, emptySet()))
        assertNull(DeveloperMessage.pendingOf("", emptySet()))
        assertNull(DeveloperMessage.pendingOf("  \n ", emptySet()))
    }

    @Test
    fun newMessageShowsTrimmed() {
        assertEquals("# Ciao", DeveloperMessage.pendingOf("\n# Ciao  ", emptySet()))
    }

    @Test
    fun seenMessageDoesNotComeBackEvenAfterAnother() {
        val first = "Primo messaggio"
        val second = "Secondo messaggio"
        val seen = setOf(DeveloperMessage.fingerprint(first), DeveloperMessage.fingerprint(second))
        assertNull(DeveloperMessage.pendingOf(first, seen))
        assertNull(DeveloperMessage.pendingOf(" $first\n", seen))
        assertEquals("Terzo", DeveloperMessage.pendingOf("Terzo", seen))
    }

    @Test
    fun fingerprintIsStableAndTellsTextsApart() {
        // FNV-1a 64 di "a": valore noto, uguale su ogni piattaforma.
        assertEquals("af63dc4c8601ec8c", DeveloperMessage.fingerprint("a"))
        assertNotEquals(DeveloperMessage.fingerprint("Ciao"), DeveloperMessage.fingerprint("ciao"))
        assertEquals(DeveloperMessage.fingerprint("è"), DeveloperMessage.fingerprint(" è "))
    }
}
