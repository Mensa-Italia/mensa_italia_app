package it.mensa.shared.members

import it.mensa.shared.model.RegSociModel
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

/** Un socio che compie gli anni oggi. [age] e' null se la data di nascita non e' credibile. */
data class Birthday(
    val member: RegSociModel,
    val age: Int?,
)

/**
 * I compleanni del giorno, come nella Home della vecchia app Flutter
 * (`getTodaysBirthdays`).
 *
 * Il registro soci salva la data di nascita come mezzanotte italiana
 * convertita in UTC: chi e' nato il 26 ottobre ha `…-10-25 22:00:00Z` (ora
 * legale) o `…-10-25 23:00:00Z` (solare), e pochi record hanno la mezzanotte
 * UTC del giorno giusto. Il giorno di nascita va quindi letto in
 * [BIRTH_ZONE], non in UTC ne' nel fuso del telefono; "oggi" invece e' quello
 * del telefono, cosi' chi e' all'estero vede i compleanni del suo giorno.
 */
object Birthdays {
    val BIRTH_ZONE: TimeZone = TimeZone.of("Europe/Rome")

    fun today(clock: Clock = Clock.System, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDate =
        clock.now().toLocalDateTime(zone).date

    fun birthDate(birthdate: Instant): LocalDate = birthdate.toLocalDateTime(BIRTH_ZONE).date

    /** Chi e' nato il 29 febbraio festeggia il 28 negli anni non bisestili. */
    fun isBirthday(birthdate: Instant, today: LocalDate): Boolean {
        val born = birthDate(birthdate)
        if (born.monthNumber == today.monthNumber && born.dayOfMonth == today.dayOfMonth) return true
        return born.monthNumber == 2 && born.dayOfMonth == 29 &&
            today.monthNumber == 2 && today.dayOfMonth == 28 && !isLeap(today.year)
    }

    /** Gli anni compiuti oggi; null per date vuote o di prova (anno futuro, 1900). */
    fun age(birthdate: Instant, today: LocalDate): Int? =
        (today.year - birthDate(birthdate).year).takeIf { it in 1..120 }

    /** I festeggiati di [today] tra [members], in ordine di nome. */
    fun of(members: List<RegSociModel>, today: LocalDate): List<Birthday> =
        members
            .filter { m -> m.birthdate?.let { isBirthday(it, today) } == true }
            .distinctBy { it.id }
            .sortedBy { it.name }
            .map { Birthday(it, age(it.birthdate!!, today)) }

    /**
     * Filtro PocketBase che restringe il registro ai possibili festeggiati,
     * cosi' la Home scarica una manciata di record invece di tutti i soci.
     *
     * PocketBase confronta le date come testo (`1988-06-08 22:00:00.000Z`),
     * quindi si cercano i tre modi in cui il giorno compare nel record: le
     * 22 e le 23 del giorno prima, la mezzanotte del giorno stesso. Il giorno
     * prima dipende dall'anno di nascita (il 1° marzo segue il 28 o il 29
     * febbraio), e si mettono entrambi. Il risultato va comunque ripassato da
     * [of], che decide col fuso giusto.
     */
    fun serverFilter(today: LocalDate): String {
        val days = buildList {
            add(today.monthNumber to today.dayOfMonth)
            if (today.monthNumber == 2 && today.dayOfMonth == 28 && !isLeap(today.year)) add(2 to 29)
        }
        val clauses = days.flatMap { (month, day) ->
            // Un anno bisestile e uno no: il giorno prima del 1° marzo cambia.
            val dayBefore = listOf(2000, 2001)
                .mapNotNull { y -> runCatching { LocalDate(y, month, day).minus(DatePeriod(days = 1)) }.getOrNull() }
                .map { it.monthNumber to it.dayOfMonth }
                .distinct()
            dayBefore.flatMap { (m, d) -> listOf("${md(m, d)} 22:", "${md(m, d)} 23:") } +
                "${md(month, day)} 00:"
        }
        return clauses.distinct().joinToString(" || ") { "birthdate ~ '$it'" }
    }

    private fun md(month: Int, day: Int) =
        "-${month.toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

    private fun isLeap(year: Int) = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
}
