package it.mensa.shared.integrity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource

/**
 * La memoria del fallimento da sola: i tentativi, il backoff, e soprattutto
 * chi puo' cancellare cosa. Il guard la tocca da piu' thread, quindi le
 * cancellazioni non possono essere "leggo, decido, scrivo".
 */
class FailureMemoryTest {

    private val clock = TestTimeSource()
    private val memory = FailureMemory(clock)
    private val error = IntegrityUnavailableException("guasto")

    @Test
    fun attemptsCountOnlyForTheSameUser() {
        assertEquals(1, memory.remember("A", error, expires = true))
        assertEquals(2, memory.remember("A", error, expires = true))
        assertEquals(2, memory.attemptsOf("A"))

        // Un altro utente riparte da uno, e il fallimento di A non c'e' piu'.
        assertEquals(1, memory.remember("B", error, expires = true))
        assertEquals(0, memory.attemptsOf("A"))
        assertEquals(1, memory.attemptsOf("B"))
    }

    @Test
    fun theBackoffIs30SecondsTimesFourToTheNMinusOneCappedAtTenMinutes() {
        val expected = listOf(30.seconds, 2.minutes, 8.minutes, 10.minutes, 10.minutes, 10.minutes)
        expected.forEachIndexed { i, wait ->
            memory.remember("A", error, expires = true)
            assertNotNull(memory.known("A"), "tentativo ${i + 1}: vale subito")

            clock += wait - 1.seconds
            assertNotNull(memory.known("A"), "tentativo ${i + 1}: vale ancora a $wait - 1 s")
            clock += 1.seconds
            assertNull(memory.known("A"), "tentativo ${i + 1}: scaduto a $wait")
        }
        assertEquals(30.seconds, memory.backoff(1))
        assertEquals(10.minutes, memory.backoff(1_000), "un numero enorme di tentativi non fa overflow")
    }

    @Test
    fun aFailureThatDoesNotExpireLastsForTheWholeProcess() {
        memory.remember("A", error, expires = false)

        clock += 100.hours

        assertSame(error, memory.known("A")?.error)
        assertNull(memory.known("B"), "e' di A")
    }

    @Test
    fun forgetNotOfKeepsTheUsersFailureAndDropsAnyoneElses() {
        memory.remember("A", error, expires = true)

        memory.forgetNotOf("A")
        assertNotNull(memory.known("A"))

        memory.forgetNotOf("B")
        assertNull(memory.known("A"))
        assertEquals(0, memory.attemptsOf("A"))
    }

    @Test
    fun noUserDropsEveryFailure() {
        memory.remember("A", error, expires = true)

        memory.forgetNotOf(null)

        assertNull(memory.known("A"))
    }

    /** La risposta tardiva di una richiesta di A non cancella il backoff di B, che nel frattempo ha fatto login. */
    @Test
    fun aSuccessOfOneUserNeverForgetsAnotherUsersFailure() {
        memory.remember("B", error, expires = true)

        memory.forgetOf("A")
        assertNotNull(memory.known("B"))
        assertEquals(1, memory.attemptsOf("B"))

        memory.forgetOf("B")
        assertNull(memory.known("B"))
    }

    @Test
    fun clearForgetsEverything() {
        memory.remember("A", error, expires = true)

        memory.clear()

        assertNull(memory.known("A"))
        assertEquals(1, memory.remember("A", error, expires = true), "i tentativi ripartono")
    }

    // ── Piu' thread insieme ──────────────────────────────────────────────────

    /** Senza compare-and-set due fallimenti simultanei dello stesso utente si contano una volta sola. */
    @Test
    fun concurrentFailuresOfTheSameUserAreAllCounted() = runTest {
        val workers = 8
        val each = 2_000

        withContext(Dispatchers.Default) {
            List(workers) { async { repeat(each) { memory.remember("A", error, expires = true) } } }.awaitAll()
        }

        assertEquals(workers * each, memory.attemptsOf("A"))
    }

    /**
     * Chi non ha niente da cancellare non scrive: le richieste che controllano "il
     * fallimento e' di un altro utente?" mentre un'altra ne ricorda uno non lo
     * cancellano (leggevano "niente" e scrivevano "niente" sopra il fallimento nuovo).
     */
    @Test
    fun checkingForAForeignFailureNeverWipesTheOneJustRemembered() = runTest {
        repeat(20) {
            val memory = FailureMemory(clock)

            withContext(Dispatchers.Default) {
                val checkers = List(4) { async { repeat(5_000) { memory.forgetNotOf("A") } } }
                val failing = async { memory.remember("A", error, expires = true) }
                (checkers + failing).awaitAll()
            }

            assertNotNull(memory.known("A"), "il fallimento ricordato deve sopravvivere ai controlli")
        }
    }
}
