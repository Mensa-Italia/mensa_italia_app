package it.mensa.shared.medals

import it.mensa.shared.geo.ItalianRegions
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * One row of `leaderboard_region_medals`: a medal of a local office's region
 * for one year, as the server computes it. The medals belong to the region,
 * not to the member: the app only shows the ones of the region the member is
 * in now. Numbers come as `12` or `12.0`, so they're read as Double; the json
 * columns are kept as they come and read leniently by [RegionMedalStatus].
 */
@Serializable
data class RegionMedalModel(
    val id: String = "",
    @SerialName("local_office") val localOffice: String = "",
    /** The seat's own name for its region. */
    val region: String = "",
    /** Normalized region name, the key the app filters on: see [RegionKey]. */
    @SerialName("region_key") val regionKey: String = "",
    val year: Double = 0.0,
    /** One of [RegionMedalKind]'s keys. */
    val medal: String = "",
    /** The figure compared with the thresholds (a podium's is the live position). */
    val value: Double = 0.0,
    val count: Double = 0.0,
    /** Secondary figures, in the order [RegionMedalKind] documents. */
    val extra: JsonElement = JsonNull,
    /** `{"bronze":n,"silver":n,"gold":n}`. */
    val levels: JsonElement = JsonNull,
    val minimum: Double = 0.0,
    val eligible: Boolean = false,
    @SerialName("current_tier") val currentTier: String = "",
    /** The highest metal reached in the year. */
    val tier: String = "",
    /** `{"bronze":"<datetime>",…}`. */
    val unlocked: JsonElement = JsonNull,
    /** `{"bronze":["userId",…],…}`: who was in the region when each level was reached. */
    val witnesses: JsonElement = JsonNull,
    val updated: String = "",
    val created: String = "",
)

/** How a region medal's figure is written. */
enum class RegionFigureUnit { PERCENT, COUNT, POSITION }

/**
 * The ten medals of a region, in the order the page lists them when nothing
 * else sorts them. [higherIsBetter] is false for the podium, whose figure is
 * a place.
 */
enum class RegionMedalKind(val key: String, val unit: RegionFigureUnit) {
    /** Members of the region seen at two or more different events, as a share. */
    PRESENT("present", RegionFigureUnit.PERCENT),

    /** Other regions the members seen twice at the region's events come from. Extras: visitors. */
    HOST("host", RegionFigureUnit.COUNT),

    /** Other regions where the region's members seen twice have stamped. Extras: members. */
    AWAY("away", RegionFigureUnit.COUNT),

    /** Months of the year with a stamped event in the region. Extras: regional events. */
    MONTHS("months", RegionFigureUnit.COUNT),

    /** Newcomers of the year stamped within two months, as a share. Extras: stamped, total. */
    WELCOME("welcome", RegionFigureUnit.PERCENT),

    /** The year's best-attended event, as a share. Extras: stampers. */
    FULLHOUSE("fullhouse", RegionFigureUnit.PERCENT),

    /** Effective regulars at the region's events, as a share. Extras: persons, effective. */
    CORE("core", RegionFigureUnit.PERCENT),

    /** National events hosted in the region. */
    NATIONAL("national", RegionFigureUnit.COUNT),

    /** Different people who organized events in the region. Extras: regional events. */
    RELAY("relay", RegionFigureUnit.COUNT),

    /** The region's place in the regions' ranking at the year's end. Extras: score. */
    PODIUM("podium", RegionFigureUnit.POSITION);

    /** False for the podium: a lower place is better. */
    val higherIsBetter: Boolean get() = unit != RegionFigureUnit.POSITION

    companion object {
        fun fromKey(key: String?): RegionMedalKind? = entries.firstOrNull { it.key == key }
    }
}

/** The metals of a region medal; [NONE] while not reached. */
enum class RegionMedalTier(val key: String) {
    NONE("none"), BRONZE("bronze"), SILVER("silver"), GOLD("gold");

    companion object {
        /** Anything that isn't a metal is [NONE]. */
        fun fromKey(key: String?): RegionMedalTier = entries.firstOrNull { it.key == key } ?: NONE
    }
}

/** One metal of a medal and the figure that earns it (for the podium, the place). */
data class RegionMedalLevel(val tier: RegionMedalTier, val threshold: Double)

/**
 * Where the region stands with one medal of one year: the row from the
 * server, read into what the screens show.
 */
class RegionMedalStatus(
    val kind: RegionMedalKind,
    val year: Int,
    /** The highest metal reached in the year; [RegionMedalTier.NONE] while none. */
    val tier: RegionMedalTier,
    /** The metal the figure is at right now, which can be lower than [tier]. */
    val currentTier: RegionMedalTier,
    val value: Double,
    val count: Double,
    /** The secondary figures, in the kind's order; may be shorter than expected. */
    val extra: List<Double>,
    /** Ascending: bronze to gold, with the figure that earns each (the podium's places go 3, 2, 1). */
    val levels: List<RegionMedalLevel>,
    /** The least people the figure needs behind it to count. */
    val minimum: Double,
    /** False while the figure has too few people behind it to count. */
    val eligible: Boolean,
    private val witnesses: Map<RegionMedalTier, Set<String>> = emptyMap(),
) {
    val isEarned: Boolean get() = tier != RegionMedalTier.NONE

    /** The next metal to reach; null once gold is. */
    val nextTier: RegionMedalTier? get() = nextLevel?.tier

    /** The figure that earns [nextTier]. */
    val nextThreshold: Double? get() = nextLevel?.threshold

    private val nextLevel: RegionMedalLevel? get() = levels.firstOrNull { it.tier.ordinal > tier.ordinal }

    /**
     * 0…1 toward [nextThreshold]; 1 once gold is reached. Counts and shares
     * climb from zero; the podium's place climbs from last of the field, so
     * it's progress toward the next place.
     */
    val progress: Double
        get() {
            val next = nextLevel ?: return 1.0
            return if (kind.higherIsBetter) {
                if (next.threshold <= 0.0) 1.0 else (value / next.threshold).coerceIn(0.0, 1.0)
            } else {
                // Unranked (no place yet) is as far as it gets.
                if (value <= 0.0) 0.0
                else ((PODIUM_FIELD - value) / (PODIUM_FIELD - next.threshold)).coerceIn(0.0, 1.0)
            }
        }

    /** The figure in the kind's unit: "12,3%", "5", "3°". */
    val valueLabel: String get() = label(value)

    /** The threshold of [tier] in the kind's unit, e.g. "8,5%". */
    fun thresholdLabel(level: RegionMedalLevel): String = label(level.threshold)

    /** Whether [userId] was in the region when [tier] was reached. */
    fun witnessed(userId: String?): Boolean =
        userId != null && isEarned && witnesses[tier]?.contains(userId) == true

    private fun label(figure: Double): String = when (kind.unit) {
        RegionFigureUnit.PERCENT -> formatFigure(figure) + "%"
        RegionFigureUnit.COUNT -> formatFigure(figure)
        RegionFigureUnit.POSITION -> formatFigure(figure) + "°"
    }

    companion object {
        /** The seats ranked in the regions' ranking: the field a place is measured against. */
        const val PODIUM_FIELD: Double = 18.0

        /** Reads a server row; null for a medal this app doesn't know. */
        fun from(model: RegionMedalModel, json: Json = lenient): RegionMedalStatus? {
            val kind = RegionMedalKind.fromKey(model.medal) ?: return null
            val levels = model.levels.element(json).tierMap()
                .mapNotNull { (tier, value) -> value?.let { RegionMedalLevel(tier, it) } }
                .filter { it.tier != RegionMedalTier.NONE }
                .sortedBy { it.tier.ordinal }
            val current = RegionMedalTier.fromKey(model.currentTier)
            // `tier` is the best of the year; an older row without it falls back to the live one.
            val best = RegionMedalTier.fromKey(model.tier).let { if (it == RegionMedalTier.NONE) current else it }
            val witnesses = (model.witnesses.element(json) as? JsonObject).orEmpty()
                .mapNotNull { (key, ids) ->
                    val tier = RegionMedalTier.fromKey(key).takeIf { it != RegionMedalTier.NONE } ?: return@mapNotNull null
                    tier to ((ids as? JsonArray).orEmpty().mapNotNull { (it as? JsonPrimitive)?.contentOrNull }.toSet())
                }
                .toMap()
            return RegionMedalStatus(
                kind = kind,
                year = model.year.roundToLong().toInt(),
                tier = best,
                currentTier = current,
                value = model.value,
                count = model.count,
                extra = model.extra.element(json).numbers(),
                levels = levels,
                minimum = model.minimum,
                eligible = model.eligible,
                witnesses = witnesses,
            )
        }

        private val lenient = Json { ignoreUnknownKeys = true; isLenient = true }

        /** A json column, which PocketBase may also hand over as text holding json. */
        private fun JsonElement.element(json: Json): JsonElement {
            val text = (this as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull ?: return this
            return runCatching { json.parseToJsonElement(text) }.getOrDefault(JsonNull)
        }

        private fun JsonElement.tierMap(): Map<RegionMedalTier, Double?> =
            (this as? JsonObject).orEmpty().entries.associate { (key, value) ->
                RegionMedalTier.fromKey(key) to (value as? JsonPrimitive)?.doubleOrNull
            }

        /** `[16, 21]`, `{"a":16,"b":21}` or a single number, as the numbers in order. */
        private fun JsonElement.numbers(): List<Double> = when (this) {
            is JsonArray -> mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
            is JsonObject -> values.mapNotNull { (it as? JsonPrimitive)?.doubleOrNull }
            is JsonPrimitive -> listOfNotNull(doubleOrNull)
        }

        private fun JsonObject?.orEmpty(): JsonObject = this ?: JsonObject(emptyMap())
        private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
    }
}

/**
 * A figure the way the app writes it: no decimals when whole, one with a
 * comma otherwise ("15", "8,5").
 */
fun formatFigure(figure: Double): String {
    val tenths = (figure * 10).roundToLong()
    val whole = tenths / 10
    val fraction = abs(tenths % 10)
    return if (fraction == 0L) "$whole" else "$whole,$fraction"
}

/** The region's medals as the screens show them. */
class RegionMedalsState(
    /** The region as the seat names itself; empty while no row says so. */
    val regionName: String,
    /** This year's medals, best first (earned by metal, then the closest to the next). */
    val currentYear: List<RegionMedalStatus>,
    /** Earlier years, each in the kinds' order. */
    val pastYears: Map<Int, List<RegionMedalStatus>>,
    val year: Int,
    /** The member the page is for, to tell which medals they were there for. */
    val userId: String? = null,
) {
    /** The earlier years, latest first, as a list Swift can walk. */
    val pastYearList: List<Int> get() = pastYears.keys.sortedDescending()

    fun past(year: Int): List<RegionMedalStatus> = pastYears[year].orEmpty()

    val earnedCount: Int get() = currentYear.count { it.isEarned }

    /** Whether the member was in the region when [status]'s metal was reached. */
    fun witnessed(status: RegionMedalStatus): Boolean = status.witnessed(userId)

    companion object {
        /** Builds the page from the region's rows; [year] is the current year. */
        fun build(
            rows: List<RegionMedalModel>,
            year: Int,
            json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
            userId: String? = null,
        ): RegionMedalsState {
            val statuses = rows.mapNotNull { RegionMedalStatus.from(it, json) }
            val byYear = statuses.groupBy { it.year }
            return RegionMedalsState(
                regionName = rows.firstOrNull { it.region.isNotBlank() }?.region.orEmpty(),
                currentYear = bestFirst(byYear[year].orEmpty()),
                pastYears = byYear.filterKeys { it < year }.toList()
                    .sortedByDescending { it.first }
                    .associate { (y, list) -> y to list.sortedBy { it.kind.ordinal } },
                year = year,
                userId = userId,
            )
        }

        /**
         * Earned by metal, gold first; then the ones on their way, closest
         * first. Ties keep the kinds' order.
         */
        internal fun bestFirst(list: List<RegionMedalStatus>): List<RegionMedalStatus> =
            list.sortedWith(
                compareBy<RegionMedalStatus> { if (it.isEarned) 0 else 1 }
                    .thenByDescending { if (it.isEarned) it.tier.ordinal else 0 }
                    .thenByDescending { if (it.isEarned) 0.0 else it.progress }
                    .thenBy { it.kind.ordinal },
            )
    }
}

/**
 * The key `leaderboard_region_medals.region_key` is filtered on: the region
 * as the server normalizes it. Seats covering two regions take the name of
 * the seat's main one, so two regions merge into one cabinet.
 */
object RegionKey {
    private val merges = mapOf(
        "Valle d'Aosta" to "Piemonte",
        "Basilicata" to "Puglia",
    )

    /**
     * The key of the region a member lives in, from `members_registry.area`
     * (the canonical name, the official bilingual one, or `NaN`/empty for
     * who lives abroad: null).
     */
    fun of(area: String?): String? {
        val region = ItalianRegions.canonical(area) ?: return null
        return merges[region] ?: region
    }
}
