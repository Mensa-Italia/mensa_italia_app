package it.mensa.shared.members

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BirthdayGreetingTest {
    private val born = Instant.parse("1990-10-31T23:00:00Z") // 1° novembre a Roma
    private val today = LocalDate(2026, 11, 1)

    @Test
    fun wishesOnTheBirthdayOncePerYear() {
        assertEquals(BirthdayWish("Matteo", 36), BirthdayGreeting.pendingOf("MATTEO SIPIONE", born, today, lastSeenYear = null))
        assertEquals(BirthdayWish("Matteo", 36), BirthdayGreeting.pendingOf("Matteo Sipione", born, today, lastSeenYear = 2025))
        assertNull(BirthdayGreeting.pendingOf("Matteo Sipione", born, today, lastSeenYear = 2026))
    }

    @Test
    fun noWishesOnOtherDays() {
        assertNull(BirthdayGreeting.pendingOf("Matteo Sipione", born, LocalDate(2026, 10, 31), lastSeenYear = null))
        assertNull(BirthdayGreeting.pendingOf("Matteo Sipione", born, LocalDate(2026, 11, 2), lastSeenYear = null))
    }

    @Test
    fun firstNameIsTheFirstWordCapitalized() {
        assertEquals("Chiara", BirthdayGreeting.firstName("  chiara valenti "))
        assertEquals("Anna", BirthdayGreeting.firstName("ANNA"))
    }
}
