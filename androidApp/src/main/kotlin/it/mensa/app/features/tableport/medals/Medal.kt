package it.mensa.app.features.tableport.medals

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import it.mensa.app.support.tr
import it.mensa.shared.koinAccess
import it.mensa.shared.medals.MedalHistory
import it.mensa.shared.medals.MedalRules
import it.mensa.shared.medals.MedalSeasonResult
import it.mensa.shared.medals.MedalStatus
import it.mensa.shared.time.Season
import it.mensa.shared.medals.MedalTier as SharedTier

/**
 * A medal of the member's collection, as shown: the rule's state from the
 * shared module (`MedalRules`, read from `view_medals`) with its name,
 * description and icon. Mirrors iOS `Medal`.
 */
data class Medal(
    val id: String,
    val title: String,
    val detail: String,
    /** Struck on the face. */
    val icon: ImageVector,
    /** The metal shown: the one earned, or the first to aim for. */
    val tier: MedalTier,
    val isEarned: Boolean,
    /** A secret medal not earned yet: shown as a mystery. */
    val isHidden: Boolean,
    /** The counter's current value. */
    val value: Int,
    /** The next metal and the value that earns it; null once at the top. */
    val nextTier: MedalTier?,
    val nextThreshold: Int?,
    /** 0…1 toward [nextThreshold]. */
    val progress: Float,
    /** Every metal of the medal, ascending, with the value that earns it. */
    val levels: List<Pair<MedalTier, Int>>,
    /** Counts stamps, so only the ones `view_medals` lets through (see [MedalCatalog.stampBased]). */
    val countsStamps: Boolean,
    /** Set for a medal of the member's region, which has more to say than a personal one. */
    val region: RegionMedalDetail? = null,
    /** Won again every season (see `MedalFamily`); false for the lifetime ones and the region's. */
    val seasonal: Boolean = false,
    /** Set for the result of a closed season (an "annata"): the season it is, by the year it starts in. */
    val pastSeason: Int? = null,
)

/** One closed season's result for a seasonal medal, as the detail's strip shows it. */
data class PastResult(val season: Int, val tier: MedalTier?, val value: Int)

/** A closed season in the "Annate": its seasonal medals with the metal each ended in. */
data class PastSeasonUi(val season: Int, val medals: List<Medal>) {
    val earnedCount: Int get() = medals.count { it.isEarned }
}

/**
 * What a region medal adds to [Medal]: its season, how it's earned, the figure
 * in its unit and the ladder with thresholds written in that unit (a
 * threshold can be 8,5%, so [Medal.levels]' whole numbers won't do).
 * Mirrors iOS `Medal.Region`.
 */
data class RegionMedalDetail(
    /** The season, by the year it starts in: 2026 is "2026/27". */
    val season: Int,
    /** "Come si ottiene". */
    val rule: String,
    /** The figure in its unit: "11,2%", "6", "3°". */
    val valueLabel: String,
    /** An earlier season: the figure is final, with no next goal. */
    val isPast: Boolean,
    val isPodium: Boolean,
    /** Every metal with the figure that earns it, and whether the season reached it. */
    val ladder: List<RegionLadderStep>,
    /** The next metal's threshold, in the unit. */
    val nextLabel: String?,
    /** The secondary figures, labelled, in words the member reads. */
    val figures: List<Pair<String, String>>,
    /** Set while too few people stand behind the figure for it to count. */
    val minimumNote: String?,
    /** The member was in the region when the metal shown was reached. */
    val witnessed: Boolean,
)

data class RegionLadderStep(val tier: MedalTier, val label: String, val reached: Boolean)

/** The metal's ramp: where light hits, the body, the shade. */
data class MetalRamp(val hi: Color, val mid: Color, val lo: Color)

enum class MedalTier(val metal: MetalRamp) {
    Bronze(MetalRamp(Color(0xFFF7C794), Color(0xFFC77D42), Color(0xFF6B3A19))),
    Silver(MetalRamp(Color(0xFFFCFCFF), Color(0xFFBDC2CC), Color(0xFF666B78))),
    Gold(MetalRamp(Color(0xFFFFF0A3), Color(0xFFDBA838), Color(0xFF805712))),
    Platinum(MetalRamp(Color(0xFFF2FAFF), Color(0xFFB3C9E0), Color(0xFF546985)));

    val label: String
        @Composable get() = when (this) {
            Bronze -> tr("medals.tier.bronze", "Bronzo")
            Silver -> tr("medals.tier.silver", "Argento")
            Gold -> tr("medals.tier.gold", "Oro")
            Platinum -> tr("medals.tier.platinum", "Platino")
        }

    companion object {
        fun of(tier: SharedTier): MedalTier = when (tier) {
            SharedTier.BRONZE -> Bronze
            SharedTier.SILVER -> Silver
            SharedTier.GOLD -> Gold
            SharedTier.PLATINUM -> Platinum
        }
    }
}

/** Not earned: dull, unlit grey whatever the metal. */
internal val LockedMetal = MetalRamp(Color(0xFF5A5A5E), Color(0xFF3A3A3E), Color(0xFF1E1E21))

data class MedalInfo(val title: String, val detail: String, val icon: ImageVector)

/**
 * Each medal's name, description and icon, by rule id. The rules
 * themselves (counters, thresholds) are the shared module's `MedalRules`;
 * iOS keeps the same table in `Medal.swift`.
 */
object MedalCatalog {
    /**
     * The medals whose counter is made of stamps: `view_medals` keeps, for
     * all of them, only the stamps worth at least a point in the ranking
     * (enough stampers for the region's size), not the member's own events'
     * and none of their region's while on its staff. Their showcase says so.
     */
    val stampBased: Set<String> = setOf(
        "collector", "steady", "unstoppable", "marathon", "four_seasons", "pioneer",
        "globetrotter", "traveller", "national_rally",
        "night_owl", "early_bird", "halloween", "new_year", "valentine", "pi_day", "birthday", "front_row", "better_late",
    )

    /** Key suffix of the descriptions whose text changed meaning, so an older translation doesn't win. */
    val detailSuffix: Map<String, String> = mapOf("four_seasons" to "_v2")

    val info: Map<String, MedalInfo> = mapOf(
        "collector" to MedalInfo("Collezionista", "Francobolli raccolti nel passaporto, tra quelli che in classifica valgono almeno un punto. Quelli dei tuoi eventi contano nella medaglia Organizzatore.", Icons.Filled.Verified),
        "steady" to MedalInfo("Costante", "Mesi diversi con almeno un francobollo.", Icons.Filled.CalendarMonth),
        "unstoppable" to MedalInfo("Inarrestabile", "Mesi consecutivi con almeno un francobollo.", Icons.Filled.LocalFireDepartment),
        "marathon" to MedalInfo("Maratoneta", "Francobolli raccolti in un solo giorno.", Icons.Filled.DirectionsRun),
        "four_seasons" to MedalInfo("Quattro stagioni", "Un francobollo in ciascuna delle quattro stagioni dell'anno: primavera, estate, autunno e inverno.", Icons.Filled.Eco),
        "pioneer" to MedalInfo("Pioniere", "Tra i primi dieci a raccogliere un francobollo.", Icons.Filled.AutoAwesome),
        "globetrotter" to MedalInfo("Giramondo", "Regioni diverse in cui hai raccolto francobolli.", Icons.Filled.Public),
        "traveller" to MedalInfo("Trasfertista", "Francobolli raccolti fuori dalla tua regione.", Icons.Filled.DirectionsCar),
        "national_rally" to MedalInfo("Raduno nazionale", "Francobolli di eventi nazionali.", Icons.Filled.Flag),
        "night_owl" to MedalInfo("Nottambulo", "Un francobollo dopo mezzanotte.", Icons.Filled.NightsStay),
        "early_bird" to MedalInfo("Mattiniero", "Un francobollo tra le 5 e le 9 del mattino.", Icons.Filled.WbTwilight),
        "halloween" to MedalInfo("Dolcetto o scherzetto", "Un francobollo la notte di Halloween.", Icons.Filled.TheaterComedy),
        "new_year" to MedalInfo("Brindisi di mezzanotte", "Un francobollo a Capodanno.", Icons.Filled.Celebration),
        "valentine" to MedalInfo("Cuore razionale", "Un francobollo a San Valentino.", Icons.Filled.Favorite),
        "pi_day" to MedalInfo("3,14159…", "Un francobollo il 14 marzo, il giorno del pi greco.", Icons.Filled.Functions),
        "birthday" to MedalInfo("Festeggiato", "Un francobollo il giorno del tuo compleanno.", Icons.Filled.Cake),
        "front_row" to MedalInfo("Prima fila", "Un francobollo raccolto il giorno stesso in cui è uscito.", Icons.Filled.Bolt),
        "better_late" to MedalInfo("Meglio tardi", "Un francobollo raccolto sei mesi dopo la sua uscita.", Icons.Filled.HourglassBottom),
        "always_there" to MedalInfo("Sempre presente", "Iscrizioni agli appuntamenti degli eventi.", Icons.Filled.Groups),
        "organizer" to MedalInfo("Organizzatore", "Eventi che hai organizzato: i francobolli dei tuoi eventi contano qui, non nelle altre medaglie.", Icons.Filled.Campaign),
        "national_director" to MedalInfo("Regista nazionale", "Hai organizzato un evento nazionale.", Icons.Filled.Stars),
        "active_member" to MedalInfo("Socio attivo", "Iscrizione in corso di validità.", Icons.Filled.VerifiedUser),
        "early_adopter" to MedalInfo("Della prima ora", "Anni passati con l'app.", Icons.Filled.PhoneIphone),
        "up_to_date" to MedalInfo("Sempre aggiornato", "Calendario degli eventi collegato.", Icons.Filled.EventAvailable),
        "cartographer" to MedalInfo("Cartografo", "Regioni seguite nel calendario.", Icons.Filled.Map),
        "multiscreen" to MedalInfo("Multischermo", "L'app su almeno due dispositivi.", Icons.Filled.Devices),
    )
}

@Composable
private fun MedalStatus.toMedal(): Medal {
    val info = MedalCatalog.info[id] ?: MedalInfo(id, "", Icons.Filled.WorkspacePremium)
    val hidden = secret && !isEarned
    return Medal(
        id = id,
        title = if (hidden) tr("medals.secret.title", "Medaglia segreta") else tr("medals.$id.title", info.title),
        detail = if (hidden) tr("medals.secret.detail", "Si svela quando la ottieni.") else tr("medals.$id.detail${MedalCatalog.detailSuffix[id].orEmpty()}", info.detail),
        icon = if (hidden) Icons.Filled.QuestionMark else info.icon,
        tier = MedalTier.of(displayTier),
        isEarned = isEarned,
        isHidden = hidden,
        value = value,
        nextTier = nextTier?.let { MedalTier.of(it) },
        nextThreshold = nextThreshold,
        progress = progress.toFloat(),
        levels = levels.map { MedalTier.of(it.tier) to it.threshold },
        countsStamps = id in MedalCatalog.stampBased,
        seasonal = isSeasonal,
    )
}

/**
 * A seasonal medal as it ended in a closed season: the metal won (or the
 * first one, dark, when it ended without), with no next goal.
 */
@Composable
private fun MedalSeasonResult.toMedal(): Medal? {
    val rule = MedalRules.rule(medalId) ?: return null
    val info = MedalCatalog.info[medalId] ?: MedalInfo(medalId, "", Icons.Filled.WorkspacePremium)
    val first = MedalTier.of(rule.levels.first().first)
    return Medal(
        id = medalId,
        title = tr("medals.$medalId.title", info.title),
        detail = tr("medals.$medalId.detail${MedalCatalog.detailSuffix[medalId].orEmpty()}", info.detail),
        icon = info.icon,
        tier = tier?.let { MedalTier.of(it) } ?: first,
        isEarned = tier != null,
        isHidden = false,
        value = value,
        nextTier = null,
        nextThreshold = null,
        progress = 1f,
        levels = rule.levels.map { MedalTier.of(it.first) to it.second },
        countsStamps = medalId in MedalCatalog.stampBased,
        seasonal = true,
        pastSeason = season,
    )
}

/**
 * Best to worst: earned by metal, platinum first; then the ones on their
 * way, closest first; the secret ones last. Ties keep the table's order
 * (`sortedWith` is stable). Mirrors iOS `MedalStore.bestFirst`.
 */
private fun bestFirst(medals: List<Medal>): List<Medal> =
    medals.sortedWith(
        compareBy<Medal> {
            when {
                it.isEarned -> 0
                it.isHidden -> 2
                else -> 1
            }
        }
            .thenByDescending { if (it.isEarned) it.tier.ordinal else 0 }
            .thenByDescending { if (!it.isEarned && !it.isHidden) it.progress else 0f },
    )

/**
 * The member's medals, the season the seasonal ones are counted in, and
 * whether the last read failed with nothing read before.
 */
data class MedalsState(val medals: List<Medal>, val failed: Boolean, val season: Int = Season.current()) {
    val earnedCount: Int get() = medals.count { it.isEarned }
}

/**
 * The member's medals, rereading `view_medals` when the caller appears.
 * Shared with Today's count through the repository, which keeps them per user.
 */
@Composable
fun rememberMedals(): MedalsState {
    val repository = remember { koinAccess().medals }
    val statuses by repository.observe().collectAsState(initial = emptyList())
    val season by repository.observeSeason().collectAsState(initial = Season.current())
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(repository) {
        failed = runCatching { repository.refresh() }.isFailure && statuses.isEmpty()
    }
    return MedalsState(bestFirst(statuses.map { it.toMedal() }), failed, season)
}

/** The member's closed seasons: for the "Annate" and for each seasonal medal's strip of past metals. */
data class MedalHistoryState(val seasons: List<PastSeasonUi>, private val byMedal: Map<String, List<PastResult>>) {
    /** [medalId]'s result in every closed season, latest first. */
    fun results(medalId: String): List<PastResult> = byMedal[medalId].orEmpty()
}

/** The seasonal medals' past seasons, rereading `medals_history` when the caller appears. */
@Composable
fun rememberMedalHistory(): MedalHistoryState {
    val repository = remember { koinAccess().medalsHistory }
    val history by repository.observe().collectAsState(initial = MedalHistory.EMPTY)
    LaunchedEffect(repository) { runCatching { repository.refresh() } }
    val seasons = history.seasons.map { season -> PastSeasonUi(season.season, season.results.mapNotNull { it.toMedal() }) }
    val byMedal = history.seasons.flatMap { it.results }.groupBy({ it.medalId }) {
        PastResult(it.season, it.tier?.let(MedalTier::of), it.value)
    }
    return MedalHistoryState(seasons, byMedal)
}

