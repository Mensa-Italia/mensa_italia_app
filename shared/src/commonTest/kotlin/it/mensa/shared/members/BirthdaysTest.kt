package it.mensa.shared.members

import it.mensa.shared.model.RegSociModel
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class BirthdaysTest {
    private fun at(iso: String) = Instant.parse(iso)

    @Test
    fun midnightInRomeStoredAsUtcIsTheNextDay() {
        val today = LocalDate(2026, 10, 26)
        // Ora legale (+2) e solare (+1): entrambe le forme del registro. Il
        // 25 ottobre era ancora ora legale nel 2010, non nel 1977 (fino al
        // 1995 finiva a settembre).
        assertTrue(Birthdays.isBirthday(at("2010-10-25T22:00:00Z"), today))
        assertTrue(Birthdays.isBirthday(at("1977-10-25T23:00:00Z"), today))
        // Qualche record ha la mezzanotte UTC del giorno giusto.
        assertTrue(Birthdays.isBirthday(at("1990-10-26T00:00:00Z"), today))
        assertFalse(Birthdays.isBirthday(at("1990-10-26T23:00:00Z"), today))
        assertFalse(Birthdays.isBirthday(at("1990-10-24T23:00:00Z"), today))
    }

    @Test
    fun leapDayBirthdaysFallOnFebruary28InCommonYears() {
        val leapBorn = at("2000-02-28T23:00:00Z") // 29 febbraio a Roma
        assertTrue(Birthdays.isBirthday(leapBorn, LocalDate(2027, 2, 28)))
        assertFalse(Birthdays.isBirthday(leapBorn, LocalDate(2028, 2, 28)))
        assertTrue(Birthdays.isBirthday(leapBorn, LocalDate(2028, 2, 29)))
    }

    @Test
    fun ageIsYearsCompletedTodayAndHidesImplausibleDates() {
        val today = LocalDate(2026, 10, 26)
        assertEquals(38, Birthdays.age(at("1988-10-25T23:00:00Z"), today))
        assertNull(Birthdays.age(at("2026-10-25T22:00:00Z"), today))
        assertNull(Birthdays.age(at("1899-10-25T23:00:00Z"), today))
    }

    @Test
    fun ofKeepsOnlyTodaysSortedByName() {
        val today = LocalDate(2026, 10, 26)
        val members = listOf(
            RegSociModel(id = "b", name = "BIANCHI LUCA", birthdate = at("1980-10-25T23:00:00Z")),
            RegSociModel(id = "x", name = "VERDI ANNA", birthdate = at("1980-10-26T23:00:00Z")),
            RegSociModel(id = "n", name = "NERI SARA"),
            RegSociModel(id = "a", name = "AZZURRI PIO", birthdate = at("2005-10-25T22:00:00Z")),
        )
        val result = Birthdays.of(members, today)
        assertEquals(listOf("a", "b"), result.map { it.member.id })
        assertEquals(listOf(21, 46), result.map { it.age })
    }

    @Test
    fun serverFilterCoversTheDayBeforeAndMidnight() {
        assertEquals(
            "birthdate ~ '-10-25 22:' || birthdate ~ '-10-25 23:' || birthdate ~ '-10-26 00:'",
            Birthdays.serverFilter(LocalDate(2026, 10, 26)),
        )
        // Il giorno prima del 1° marzo e' il 28 o il 29 febbraio.
        val march1 = Birthdays.serverFilter(LocalDate(2026, 3, 1))
        assertTrue("'-02-28 22:'" in march1 && "'-02-29 23:'" in march1 && "'-03-01 00:'" in march1)
        // Il 28 febbraio di un anno comune cerca anche i nati il 29.
        val feb28 = Birthdays.serverFilter(LocalDate(2027, 2, 28))
        assertTrue("'-02-28 22:'" in feb28 && "'-02-29 00:'" in feb28)
    }
}
