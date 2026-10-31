package it.mensa.shared.medals

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Made-up region medals for capturing the cabinet's region page without
 * signing in (`MENSA_REGION_MEDALS_FIXTURE=1` on iOS, extra
 * `mensa_region_medals_fixture` on Android). Only debug harnesses switch it
 * on; off, the repository reads the server. The seat's name is invented, and
 * the fixture stands in for the `region_medals_enabled` flag and for the
 * member's region too.
 */
object RegionMedalFixtures {

    /** True only when a debug harness turns it on. */
    var enabled: Boolean = false

    internal const val KEY: String = "fixture-region"

    private const val SEAT = "Sede delle Tre Valli"
    private val witnesses = listOf(MedalFixtures.ME_ID, "fixture-a", "fixture-b", "fixture-c")

    /** This season's medals and last season's, in the kinds' order. */
    internal fun rows(season: Int): List<RegionMedalModel> = current(season) + previous(season - 1)

    private fun current(season: Int) = listOf(
        row(season, "present", 11.2, 16, listOf(16, 21), Triple(5, 10, 15), 3, "silver", we = true),
        row(season, "host", 3.0, 14, listOf(14), Triple(2, 4, 6), 2, "bronze", we = true),
        row(season, "away", 6.0, 12, listOf(12), Triple(2, 4, 6), 0, "gold", we = true),
        row(season, "months", 8.0, 11, listOf(11), Triple(6, 9, 12), 0, "silver", we = false),
        row(season, "welcome", 18.0, 4, listOf(4, 22), Triple(25, 40, 60), 3, "none", eligible = true),
        row(season, "fullhouse", 9.2, 27, listOf(27), Triple(8, 15, 25), 5, "bronze", we = false),
        row(season, "core", 16.0, 21, listOf(48, 21), Triple(8, 15, 25), 5, "silver", we = true),
        row(season, "national", 0.0, 0, emptyList(), Triple(1, 2, 3), 0, "none"),
        row(season, "relay", 2.0, 9, listOf(9), Triple(2, 3, 4), 0, "bronze", we = true),
        row(season, "podium", 4.0, 0, listOf(31), Triple(3, 2, 1), 0, "none"),
    )

    private fun previous(season: Int) = listOf(
        row(season, "present", 6.4, 9, listOf(9, 25), Triple(5, 10, 15), 3, "bronze", we = true),
        row(season, "host", 5.0, 18, listOf(18), Triple(2, 4, 6), 2, "silver", we = false),
        row(season, "months", 12.0, 14, listOf(14), Triple(6, 9, 12), 0, "gold", we = true),
        row(season, "fullhouse", 7.0, 21, listOf(21), Triple(8, 15, 25), 5, "none"),
        row(season, "podium", 2.0, 0, listOf(44), Triple(3, 2, 1), 0, "silver", we = true),
    )

    private fun row(
        season: Int,
        medal: String,
        value: Double,
        count: Int,
        extra: List<Int>,
        levels: Triple<Int, Int, Int>,
        minimum: Int,
        tier: String,
        eligible: Boolean = true,
        we: Boolean = false,
    ): RegionMedalModel {
        val reached = listOf("bronze", "silver", "gold").take(listOf("none", "bronze", "silver", "gold").indexOf(tier))
        return RegionMedalModel(
            id = "fixture-$season-$medal",
            localOffice = "fixture-office",
            region = SEAT,
            regionKey = KEY,
            season = season.toDouble(),
            medal = medal,
            value = value,
            count = count.toDouble(),
            extra = JsonArray(extra.map { JsonPrimitive(it) }),
            levels = buildJsonObject {
                put("bronze", levels.first)
                put("silver", levels.second)
                put("gold", levels.third)
            },
            minimum = minimum.toDouble(),
            eligible = eligible,
            currentTier = tier,
            tier = tier,
            unlocked = buildJsonObject { reached.forEach { put(it, "${season + 1}-06-15 09:00:00.000Z") } },
            witnesses = witnessMap(reached, we),
        )
    }

    private fun witnessMap(reached: List<String>, includesMe: Boolean): JsonElement = JsonObject(
        reached.associateWith {
            JsonArray((if (includesMe) witnesses else witnesses.drop(1)).map { id -> JsonPrimitive(id) })
        },
    )
}
