package it.mensa.shared.localgroup

import it.mensa.shared.model.DealModel
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LeaderboardRegionModel
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeLinktreeRowModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocalOfficeStatsModel
import it.mensa.shared.time.Season
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable

/**
 * Everything the "Gruppo locale" tab shows, for the member's local office.
 * Kept whole in SQLite (KeyValue, as JSON), so after a cold start the tab
 * shows the last one read while the network brings it up to date.
 */
@Serializable
data class LocalGroupSnapshot(
    val office: LocalOfficeModel,
    /** The secretary (`isTheOfficer`) first, then the co-secretaries by name. */
    val admins: List<LocalOfficeAdminModel>,
    /** The office's active links (no sections), in their `sort_order`. */
    val links: List<LocalOfficeLinktreeRowModel>,
    /** The office's figures; null when the server has none (or didn't answer). */
    val stats: LocalOfficeStatsModel?,
    /** The office's row in the regions' ranking; null when the server hides it (under 3 points). */
    val ranking: LeaderboardRegionModel?,
    /** Events of the region not over yet, soonest first, at most 5. */
    val nextEvents: List<EventModel>,
    /** Active local deals of the region, at most 10. */
    val deals: List<DealModel>,
    /** The signed-in member is one of [admins] or has the `super` power: the admin area exists. */
    val isAdmin: Boolean,
    /** The merged region key the office was found by, e.g. "Puglia" (also for Basilicata). */
    val regionKey: String = "",
) {
    /** The secretary, if the office has one. */
    val secretary: LocalOfficeAdminModel? get() = admins.firstOrNull { it.isTheOfficer }

    /** The co-secretaries, by name. */
    val coSecretaries: List<LocalOfficeAdminModel> get() = admins.filterNot { it.isTheOfficer }

    /** The soonest event of [nextEvents], if any. */
    val nextEvent: EventModel? get() = nextEvents.firstOrNull()
}

/** What the "Gruppo locale" tab is showing. */
sealed interface LocalGroupState {
    /** Nothing read yet, and nothing saved from an earlier read. */
    data object Loading : LocalGroupState

    /** The registry has no region for the member (they live abroad, or it's unknown). */
    data object NoRegion : LocalGroupState

    /** The region is known, but no local office has that `region_key`. */
    data object NoOffice : LocalGroupState

    /** The office's page. While a refresh runs this stays the previous (or saved) snapshot. */
    data class Ready(val snapshot: LocalGroupSnapshot) : LocalGroupState

    /** The office couldn't be resolved; [cached] is the last snapshot read, to keep showing under an error. */
    data class Failed(val message: String?, val cached: LocalGroupSnapshot?) : LocalGroupState
}

/** Which members `members-export` writes in the .xlsx. */
enum class ExportKind(val wire: String) {
    /** Members who joined or moved in during the period. */
    NEW("new"),

    /** Members who left or moved out during the period. */
    LEFT("left"),

    /** Every active member of the region now (the period is ignored). */
    ACTIVE("active"),
}

/** A range of days, both ends included. */
data class LocalGroupRange(val from: LocalDate, val to: LocalDate)

/** The periods the admin area offers for the members' movements. */
enum class LocalGroupPeriod(val days: Int) {
    DAYS_7(7),
    DAYS_30(30),
    DAYS_90(90),

    /** From 1 October of the current season to today. */
    SEASON(0),
    ;

    /** The range ending [today] (Europe/Rome by default). */
    fun range(today: LocalDate = LocalGroupDates.today()): LocalGroupRange = when (this) {
        SEASON -> LocalGroupRange(Season.bounds(Season.of(today.atStartOfDayIn(Season.ZONE))).first, today)
        else -> LocalGroupRange(today.minus(days, DateTimeUnit.DAY), today)
    }
}

/** Dates for the admin area, in Europe/Rome like the server's. */
object LocalGroupDates {
    /** Today in Europe/Rome. */
    fun today(): LocalDate = Clock.System.now().toLocalDateTime(Season.ZONE).date

    /** [date] from "YYYY-MM-DD" (or a PocketBase datetime), null when unreadable. Handy from Swift. */
    fun parse(date: String): LocalDate? = runCatching { LocalDate.parse(date.take(ISO_DATE_LENGTH)) }.getOrNull()

    /** A date from its parts, for Swift callers. */
    fun of(year: Int, month: Int, day: Int): LocalDate = LocalDate(year, month, day)

    internal const val ISO_DATE_LENGTH = 10
}

/**
 * What the platforms need to save the bytes of
 * [LocalGroupRepository.exportMembers]: they write them to a temporary file
 * named [fileName] and share it (ShareLink on iOS, ACTION_SEND with a
 * FileProvider on Android) with [MIME_TYPE]. On iOS `localGroupExportData(bytes)`
 * (appleMain) turns the bytes into `Data`; on Android they go straight to a file.
 */
object LocalGroupExportFile {
    const val MIME_TYPE: String = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    const val EXTENSION: String = "xlsx"

    /** "soci-nuovi-lombardia-2026-10-01_2026-11-08.xlsx"; the active members' file has today's date only. */
    fun fileName(office: LocalOfficeModel, kind: ExportKind, from: LocalDate?, to: LocalDate?): String {
        val what = when (kind) {
            ExportKind.NEW -> "nuovi"
            ExportKind.LEFT -> "usciti"
            ExportKind.ACTIVE -> "iscritti"
        }
        val slug = slugOf(office.slug.ifBlank { office.region.ifBlank { office.name } })
        val period = when {
            kind == ExportKind.ACTIVE || from == null || to == null -> (to ?: LocalGroupDates.today()).toString()
            else -> "${from}_$to"
        }
        return listOf("soci", what, slug, period).filter { it.isNotEmpty() }.joinToString("-") + ".$EXTENSION"
    }

    private fun slugOf(raw: String): String =
        raw.lowercase().map { if (it in 'a'..'z' || it in '0'..'9') it else '-' }
            .joinToString("").split('-').filter { it.isNotEmpty() }.joinToString("-")
}
