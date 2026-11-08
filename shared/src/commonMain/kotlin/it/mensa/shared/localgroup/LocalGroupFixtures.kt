package it.mensa.shared.localgroup

import it.mensa.shared.model.DealExpand
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.EventExpand
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LeaderboardRegionModel
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeMonthlyStatsModel
import it.mensa.shared.model.LocalOfficeStatsModel
import it.mensa.shared.model.LocationModel
import it.mensa.shared.model.MemberChangeKind
import it.mensa.shared.model.MemberChangeModel
import it.mensa.shared.model.RegSociModel
import it.mensa.shared.time.Season
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours

/**
 * A made-up local office for capturing the "Gruppo locale" tab and its admin
 * area without signing in. Only debug harnesses switch it on; on, the
 * repository answers this data, stands in for the `local_group_enabled` flag
 * and never touches the network. Every person here is invented.
 */
object LocalGroupFixtures {

    /** True only when a debug harness turns it on. Set it before the first [LocalGroupRepository.observe]. */
    var enabled: Boolean = false

    /** Whether the fixture member is an admin of the office (the admin area shows). */
    var admin: Boolean = false

    const val OFFICE_ID: String = "fixture-lombardia"
    private const val REGION = "Lombardia"

    private val office = LocalOfficeModel(
        id = OFFICE_ID,
        name = "Mensa Lombardia",
        region = REGION,
        slug = "lombardia",
        bio = "Il gruppo locale dei soci che vivono in Lombardia, da Milano a Sondrio. " +
            "Ci troviamo ogni mese per cene, visite guidate, giochi e conferenze.",
        image = "",
        regionKey = REGION,
    )

    private val admins = listOf(
        admin("a1", "Chiara Valenti", isOfficer = true),
        admin("a2", "Lorenzo Gatti"),
        admin("a3", "Sofia Marchetti"),
    )

    private val links = listOf(
        link("l1", "Email", "mailto:lombardia@mensa.it", "email", 1),
        link("l2", "Instagram", "https://www.instagram.com/mensa.lombardia", "instagram", 2),
        link("l3", "Facebook", "https://www.facebook.com/mensalombardia", "facebook", 3),
        link("l4", "Sito", "https://www.mensa.it/lombardia", "web", 4),
    )

    private val places = listOf(
        LocationModel(id = "p1", name = "Circolo Filologico Milanese", address = "Via Clerici 10, 20121 Milano MI", state = REGION),
        LocationModel(id = "p2", name = "Accademia Carrara", address = "Piazza Giacomo Carrara 82, 24121 Bergamo BG", state = REGION),
        LocationModel(id = "p3", name = "Villa Olmo", address = "Via Simone Cantoni 1, 22100 Como CO", state = REGION),
    )

    private val eventNames = listOf(
        "Cena sociale di novembre", "Visita guidata all'Accademia Carrara", "Torneo di giochi da tavolo",
        "Conferenza: il cervello che impara", "Aperitivo di benvenuto ai nuovi soci", "Passeggiata sul lago di Como",
        "Serata quiz", "Gita a Mantova",
    )

    private val changeNames = listOf(
        "Matteo Bellini", "Aurora Costa", "Edoardo Fontana", "Beatrice Leone", "Davide Moretti", "Martina Galli",
        "Riccardo Conti", "Elena Barbieri", "Francesco Testa", "Alice Serra", "Pietro Longo", "Giorgia Bianchi",
    )

    /** The tab's snapshot, with events from next weekend on. */
    internal fun snapshot(now: Instant = Clock.System.now()): LocalGroupSnapshot = LocalGroupSnapshot(
        office = office,
        admins = admins,
        links = links,
        stats = LocalOfficeStatsModel(
            id = OFFICE_ID, localOffice = OFFICE_ID, regionKey = REGION,
            membersActive = 412, membersNew7d = 3, membersNew30d = 11, membersLeft30d = 4,
            eventsSeason = 9, eventsUpcoming = 2, attendanceSeason = 187, dealsActive = 5,
            season = Season.label(Season.current(now)), computedAt = now.toString(),
        ),
        ranking = LeaderboardRegionModel(
            id = OFFICE_ID, season = Season.current(now).toDouble(), name = office.name, region = REGION,
            regionKey = REGION, position = 2.0, score = 41.0, membersActive = 412.0, previousPosition = 3.0,
            eventsTotal = 9.0, attendanceTotal = 187.0,
        ),
        nextEvents = futureEvents(now, count = 3),
        deals = deals,
        isAdmin = admin,
        regionKey = REGION,
    )

    /** Twelve movements spread over [from]..[to], newest first. */
    internal fun changes(from: LocalDate, to: LocalDate): List<MemberChangeModel> {
        val span = from.daysUntil(to).coerceAtLeast(0)
        val kinds = listOf(
            MemberChangeKind.JOINED, MemberChangeKind.JOINED, MemberChangeKind.LEFT,
            MemberChangeKind.MOVED_IN, MemberChangeKind.JOINED, MemberChangeKind.MOVED_OUT,
        )
        return changeNames.mapIndexed { i, name ->
            val day = from.plus(span * i / (changeNames.size - 1).coerceAtLeast(1), DateTimeUnit.DAY)
            MemberChangeModel(
                id = "fixture-change-$i", member = "fixture-member-$i", name = name, regionKey = REGION,
                localOffice = OFFICE_ID, kind = kinds[i % kinds.size].wire, day = "$day 00:00:00.000Z",
            )
        }.sortedWith(compareByDescending<MemberChangeModel> { it.dayIso }.thenBy { it.name })
    }

    /** Three members who joined this week, by name. */
    internal fun newMembers(): List<RegSociModel> =
        listOf("Aurora Costa", "Edoardo Fontana", "Matteo Bellini").mapIndexed { i, name ->
            RegSociModel(id = "fixture-member-$i", name = name, city = listOf("Milano", "Bergamo", "Como")[i])
        }

    /** One row per month from October of the previous season to [today]'s month, like the server. */
    internal fun monthly(today: LocalDate): List<LocalOfficeMonthlyStatsModel> {
        val season = Season.of(today.atNoon())
        var month = LocalDate(season - 1, 10, 1)
        var active = 360
        val out = mutableListOf<LocalOfficeMonthlyStatsModel>()
        var i = 0
        while (month <= today) {
            val joined = 6 + (i * 5) % 7
            val left = 2 + (i * 3) % 4
            active += joined - left
            val events = 2 + i % 3
            val key = "${month.year}-${month.monthNumber.toString().padStart(2, '0')}"
            out += LocalOfficeMonthlyStatsModel(
                id = "${OFFICE_ID}_$key", localOffice = OFFICE_ID, regionKey = REGION, month = key,
                season = Season.label(if (month.monthNumber >= 10) month.year else month.year - 1),
                events = events, attendance = events * (18 + i % 5), distinctAttendees = events * 14 + i,
                membersNew = joined, membersLeft = left, membersActive = active,
            )
            month = month.plus(1, DateTimeUnit.MONTH)
            i++
        }
        return out
    }

    /** Eight events of the office: three to come and five past, latest first. */
    internal fun officeEvents(now: Instant = Clock.System.now()): List<EventModel> {
        val future = futureEvents(now, count = 3)
        val past = (1..5).map { i ->
            event(index = 2 + i, start = now.minus((i * 17 * 24).hours))
        }
        return (future + past).sortedByDescending { it.whenStart }
    }

    /** A few bytes standing in for the .xlsx (a zip's signature and nothing else). */
    internal fun exportBytes(): ByteArray = byteArrayOf(0x50, 0x4B, 0x03, 0x04, 0x14, 0x00, 0x00, 0x00)

    private val deals = listOf(
        deal("d1", "Libreria Hoepli", "Libri", "Sconto del 10% sui libri, esclusi i testi scolastici.", 0),
        deal("d2", "Teatro Franco Parenti", "Spettacoli", "Biglietti ridotti per gli spettacoli della stagione.", 1),
        deal("d3", "Palestra Milano Centro", "Sport", "Iscrizione gratuita e 15% sull'abbonamento annuale.", 2),
    )

    private fun futureEvents(now: Instant, count: Int): List<EventModel> {
        val today = now.toLocalDateTime(Season.ZONE).date
        val untilSaturday = (DayOfWeek.SATURDAY.ordinal - today.dayOfWeek.ordinal + 7) % 7
        val saturday = today.plus(if (untilSaturday == 0) 7 else untilSaturday, DateTimeUnit.DAY)
        return (0 until count).map { i ->
            val day = saturday.plus(i * 8, DateTimeUnit.DAY)
            event(index = i, start = LocalDateTime(day.year, day.monthNumber, day.dayOfMonth, 19, 30).toInstant(Season.ZONE))
        }
    }

    private fun event(index: Int, start: Instant): EventModel {
        val place = places[index % places.size]
        return EventModel(
            id = "fixture-event-$index",
            name = eventNames[index % eventNames.size],
            description = "Un incontro del gruppo locale aperto a tutti i soci.",
            whenStart = start,
            whenEnd = start.plus(3.hours),
            contact = "lombardia@mensa.it",
            ownerId = "fixture-a1",
            positionId = place.id,
            localOfficeId = OFFICE_ID,
            expand = EventExpand(position = place),
        )
    }

    private fun deal(id: String, name: String, sector: String, details: String, place: Int) = DealModel(
        id = "fixture-$id",
        name = name,
        commercialSector = sector,
        positionId = places[place].id,
        expand = DealExpand(position = places[place].copy(state = REGION)),
        isLocal = true,
        isActive = true,
        details = details,
        who = "Soci in regola con la quota",
    )

    private fun admin(id: String, name: String, isOfficer: Boolean = false): LocalOfficeAdminModel {
        val email = name.lowercase().replace(' ', '.') + "@mensa.it"
        return LocalOfficeAdminModel(
            id = "fixture-$id", localOffice = OFFICE_ID, localOfficeName = office.name, region = REGION,
            user = "fixture-$id", name = name, email = email, isTheOfficer = isOfficer,
        )
    }

    private fun link(id: String, title: String, url: String, icon: String, order: Int) = LocalOfficeLinktreeRowModel(
        id = "fixture-$id", localOffice = OFFICE_ID, localOfficeName = office.name, region = REGION,
        slug = office.slug, title = title, url = url, icon = icon, kind = "link", sortOrder = order,
    )

    private fun LocalDate.atNoon(): Instant = LocalDateTime(year, monthNumber, dayOfMonth, 12, 0).toInstant(Season.ZONE)
}
