package it.mensa.shared.medals

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RegionMedalsTest {

    private fun row(
        medal: String,
        value: Double,
        tier: String,
        levels: Triple<Int, Int, Int>,
        season: Int = 2026,
        current: String = tier,
        witnesses: Map<String, List<String>> = emptyMap(),
        extra: kotlinx.serialization.json.JsonElement = JsonNull,
    ) = RegionMedalModel(
        medal = medal,
        season = season.toDouble(),
        value = value,
        tier = tier,
        currentTier = current,
        levels = buildJsonObject {
            put("bronze", levels.first)
            put("silver", levels.second)
            put("gold", levels.third)
        },
        extra = extra,
        witnesses = buildJsonObject {
            witnesses.forEach { (t, ids) -> put(t, JsonArray(ids.map { JsonPrimitive(it) })) }
        },
        eligible = true,
    )

    private fun status(model: RegionMedalModel) = assertNotNull(RegionMedalStatus.from(model))

    // ── Tier and progress ────────────────────────────────────────────────────

    @Test
    fun levelsAreAscendingAndNextIsTheFirstUnreached() {
        val s = status(row("present", 7.5, "bronze", Triple(5, 10, 15)))
        assertEquals(listOf(RegionMedalTier.BRONZE, RegionMedalTier.SILVER, RegionMedalTier.GOLD), s.levels.map { it.tier })
        assertEquals(listOf(5.0, 10.0, 15.0), s.levels.map { it.threshold })
        assertTrue(s.isEarned)
        assertEquals(RegionMedalTier.SILVER, s.nextTier)
        assertEquals(10.0, s.nextThreshold)
        assertEquals(0.75, s.progress, 0.0001)
    }

    @Test
    fun unearnedMedalAimsAtBronze() {
        val s = status(row("host", 1.0, "none", Triple(2, 4, 6)))
        assertFalse(s.isEarned)
        assertEquals(RegionMedalTier.BRONZE, s.nextTier)
        assertEquals(0.5, s.progress, 0.0001)
    }

    @Test
    fun goldIsTheTopAndProgressIsFull() {
        val s = status(row("months", 12.0, "gold", Triple(6, 9, 12)))
        assertNull(s.nextTier)
        assertNull(s.nextThreshold)
        assertEquals(1.0, s.progress)
    }

    @Test
    fun theBestOfTheYearIsShownEvenIfTheFigureDippedBack() {
        val s = status(row("present", 4.0, tier = "silver", levels = Triple(5, 10, 15), current = "none"))
        assertEquals(RegionMedalTier.SILVER, s.tier)
        assertEquals(RegionMedalTier.NONE, s.currentTier)
        assertEquals(RegionMedalTier.GOLD, s.nextTier)
    }

    @Test
    fun aRowWithoutTheBestTierFallsBackToTheLiveOne() {
        val s = status(row("host", 4.0, tier = "", levels = Triple(2, 4, 6), current = "silver"))
        assertEquals(RegionMedalTier.SILVER, s.tier)
    }

    @Test
    fun podiumLevelsAreInvertedAndProgressIsTowardTheNextPlace() {
        val far = status(row("podium", 18.0, "none", Triple(3, 2, 1)))
        assertEquals(listOf(3.0, 2.0, 1.0), far.levels.map { it.threshold })
        assertFalse(far.kind.higherIsBetter)
        assertEquals(RegionMedalTier.BRONZE, far.nextTier)
        assertEquals(0.0, far.progress, 0.0001)

        // 10th of 18 toward the 3rd: 8 of the 15 places climbed.
        val mid = status(row("podium", 10.0, "none", Triple(3, 2, 1)))
        assertEquals(8.0 / 15.0, mid.progress, 0.0001)

        // On the podium's step the live place already beats the next target.
        assertEquals(1.0, status(row("podium", 3.0, "none", Triple(3, 2, 1))).progress, 0.0001)

        // Awarded bronze, aiming at silver (2nd): 3rd of 18 is 15 of 16 places.
        val awarded = status(row("podium", 3.0, "bronze", Triple(3, 2, 1)))
        assertEquals(RegionMedalTier.SILVER, awarded.nextTier)
        assertEquals(15.0 / 16.0, awarded.progress, 0.0001)
    }

    @Test
    fun podiumWithoutAPlaceYetHasNoProgress() {
        assertEquals(0.0, status(row("podium", 0.0, "none", Triple(3, 2, 1))).progress)
    }

    // ── Labels ───────────────────────────────────────────────────────────────

    @Test
    fun figuresAreWrittenInItalian() {
        assertEquals("15", formatFigure(15.0))
        assertEquals("8,5", formatFigure(8.5))
        assertEquals("12,3", formatFigure(12.34))
        assertEquals("0", formatFigure(0.0))
    }

    @Test
    fun labelsFollowTheKindsUnit() {
        assertEquals("11,2%", status(row("present", 11.2, "silver", Triple(5, 10, 15))).valueLabel)
        assertEquals("6", status(row("host", 6.0, "gold", Triple(2, 4, 6))).valueLabel)
        val podium = status(row("podium", 3.0, "bronze", Triple(3, 2, 1)))
        assertEquals("3°", podium.valueLabel)
        assertEquals(listOf("3°", "2°", "1°"), podium.levels.map(podium::thresholdLabel))
        val fullhouse = status(row("fullhouse", 9.0, "bronze", Triple(8, 15, 25)))
        assertEquals("8%", fullhouse.thresholdLabel(fullhouse.levels.first()))
    }

    // ── Json columns ─────────────────────────────────────────────────────────

    @Test
    fun extraFiguresReadFromAnArrayAnObjectOrANumber() {
        val array = status(row("present", 1.0, "none", Triple(5, 10, 15), extra = JsonArray(listOf(JsonPrimitive(16), JsonPrimitive(21)))))
        assertEquals(listOf(16.0, 21.0), array.extra)
        val obj = status(row("core", 1.0, "none", Triple(8, 15, 25), extra = buildJsonObject { put("persons", 48); put("effective", 21) }))
        assertEquals(listOf(48.0, 21.0), obj.extra)
        assertEquals(listOf(14.0), status(row("host", 1.0, "none", Triple(2, 4, 6), extra = JsonPrimitive(14))).extra)
        assertEquals(emptyList(), status(row("national", 0.0, "none", Triple(1, 2, 3))).extra)
    }

    @Test
    fun jsonColumnsCameBackAsTextStillRead() {
        val model = RegionMedalModel(
            medal = "host",
            season = 2026.0,
            tier = "bronze",
            levels = JsonPrimitive("""{"bronze":2,"silver":4,"gold":6}"""),
            witnesses = JsonPrimitive("""{"bronze":["me"]}"""),
        )
        val s = status(model)
        assertEquals(listOf(2.0, 4.0, 6.0), s.levels.map { it.threshold })
        assertTrue(s.witnessed("me"))
    }

    @Test
    fun missingOrBrokenJsonDoesNotBreakTheRow() {
        val s = status(RegionMedalModel(medal = "relay", season = 2026.0, levels = JsonPrimitive("not json"), witnesses = JsonNull))
        assertTrue(s.levels.isEmpty())
        assertNull(s.nextTier)
        assertFalse(s.witnessed("me"))
    }

    @Test
    fun anUnknownMedalIsLeftOut() {
        assertNull(RegionMedalStatus.from(RegionMedalModel(medal = "from_the_future")))
    }

    // ── Witnesses ────────────────────────────────────────────────────────────

    @Test
    fun youWereThereOnlyForTheTierShown() {
        val s = status(
            row(
                "present", 11.0, "silver", Triple(5, 10, 15),
                witnesses = mapOf("bronze" to listOf("me", "other"), "silver" to listOf("other")),
            ),
        )
        assertFalse(s.witnessed("me"))
        assertTrue(s.witnessed("other"))
        assertFalse(s.witnessed(null))
    }

    @Test
    fun noMetalNoWitnesses() {
        val s = status(row("present", 1.0, "none", Triple(5, 10, 15), witnesses = mapOf("bronze" to listOf("me"))))
        assertFalse(s.witnessed("me"))
    }

    // ── The page ─────────────────────────────────────────────────────────────

    @Test
    fun theStateSplitsTheCurrentSeasonFromThePastOnes() {
        val rows = listOf(
            row("present", 1.0, "none", Triple(5, 10, 15), season = 2026),
            row("host", 5.0, "silver", Triple(2, 4, 6), season = 2026),
            row("host", 2.0, "bronze", Triple(2, 4, 6), season = 2025),
            row("months", 9.0, "silver", Triple(6, 9, 12), season = 2024),
            row("present", 6.0, "bronze", Triple(5, 10, 15), season = 2025),
        )
        val state = RegionMedalsState.build(rows, 2026)
        assertEquals(listOf("host", "present"), state.currentSeason.map { it.kind.key })
        assertEquals(listOf(2025, 2024), state.pastSeasonList)
        // Past seasons keep the kinds' order.
        assertEquals(listOf("present", "host"), state.past(2025).map { it.kind.key })
        assertEquals(1, state.earnedCount)
    }

    @Test
    fun theCurrentSeasonListsTheBestMetalFirstThenTheClosest() {
        val rows = listOf(
            row("host", 1.0, "none", Triple(2, 4, 6)),
            row("months", 7.0, "bronze", Triple(6, 9, 12)),
            row("present", 11.0, "silver", Triple(5, 10, 15)),
            row("relay", 4.0, "gold", Triple(2, 3, 4)),
            row("core", 9.0, "bronze", Triple(8, 15, 25)),
            row("away", 3.0, "none", Triple(2, 4, 6)),
        )
        val order = RegionMedalsState.build(rows, 2026).currentSeason.map { it.kind.key }
        // Gold, silver, the bronzes in the kinds' order; then the unearned, the closest to bronze first.
        assertEquals(listOf("relay", "present", "months", "core", "away", "host"), order)
    }

    @Test
    fun theRegionNameIsTheSeatsOwn() {
        val rows = listOf(RegionMedalModel(medal = "host", season = 2026.0, region = "Sede delle Tre Valli"))
        assertEquals("Sede delle Tre Valli", RegionMedalsState.build(rows, 2026).regionName)
        assertEquals("", RegionMedalsState.build(emptyList(), 2026).regionName)
    }

    @Test
    fun aRowParsesFromTheServersJson() {
        val body = """{"id":"x","local_office":"o","region":"R","region_key":"K","season":2026,"medal":"present","value":11.2,
            "count":16,"extra":[16,21],"levels":{"bronze":5,"silver":10,"gold":15},"minimum":3,"eligible":true,
            "current_tier":"silver","tier":"silver","unlocked":{"bronze":"2026-05-01 10:00:00.000Z"},
            "witnesses":{"silver":["u1"]},"updated":"","created":"","collectionName":"x"}"""
        val model = Json { ignoreUnknownKeys = true; coerceInputValues = true }.decodeFromString(RegionMedalModel.serializer(), body)
        val s = status(model)
        assertEquals(RegionMedalKind.PRESENT, s.kind)
        assertEquals(2026, s.season)
        assertEquals(RegionMedalTier.SILVER, s.tier)
        assertEquals(listOf(16.0, 21.0), s.extra)
        assertTrue(s.witnessed("u1"))
    }

    // ── The region key ───────────────────────────────────────────────────────

    @Test
    fun theKeyIsTheCanonicalRegion() {
        assertEquals("Lombardia", RegionKey.of("Lombardia"))
        assertEquals("Emilia-Romagna", RegionKey.of("Emilia Romagna"))
        assertEquals("Trentino-Alto Adige", RegionKey.of("Trentino-Alto Adige/Südtirol"))
    }

    @Test
    fun twoRegionsMergeIntoTheirSeats() {
        assertEquals("Piemonte", RegionKey.of("Valle d'Aosta"))
        assertEquals("Piemonte", RegionKey.of("Valle d'Aosta/Vallée d'Aoste"))
        assertEquals("Puglia", RegionKey.of("Basilicata"))
        assertEquals("Puglia", RegionKey.of("Puglia"))
    }

    @Test
    fun whoLivesAbroadHasNoKey() {
        assertNull(RegionKey.of("NaN"))
        assertNull(RegionKey.of(""))
        assertNull(RegionKey.of(null))
        assertNull(RegionKey.of("Atlantide"))
    }

    @Test
    fun theFixtureCoversTheTiersAndTheSeasons() {
        val state = RegionMedalsState.build(RegionMedalFixtures.rows(2026), 2026)
        assertEquals(10, state.currentSeason.size)
        assertEquals(listOf(2025), state.pastSeasonList)
        assertEquals(7, state.earnedCount)
        assertTrue(state.currentSeason.any { it.witnessed(MedalFixtures.ME_ID) })
        assertTrue(state.currentSeason.any { it.isEarned && !it.witnessed(MedalFixtures.ME_ID) })
        assertEquals(setOf(RegionMedalTier.NONE, RegionMedalTier.BRONZE, RegionMedalTier.SILVER, RegionMedalTier.GOLD), state.currentSeason.map { it.tier }.toSet())
    }
}
