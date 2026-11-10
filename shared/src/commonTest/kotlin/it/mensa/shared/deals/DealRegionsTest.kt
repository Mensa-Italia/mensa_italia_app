package it.mensa.shared.deals

import it.mensa.shared.model.DealExpand
import it.mensa.shared.model.DealModel
import it.mensa.shared.model.LocationModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DealRegionsTest {
    private fun deal(id: String, state: String?) =
        DealModel(id = id, expand = state?.let { DealExpand(position = LocationModel(state = it)) })

    private val piemonte = deal("piemonte", "Piemonte")
    private val aosta = deal("aosta", "Valle d'Aosta")
    private val lombardia = deal("lombardia", "Lombardia")
    private val nowhere = deal("nowhere", null)
    private val nan = deal("nan", "NaN")
    private val all = listOf(lombardia, nowhere, piemonte, nan, aosta)

    @Test
    fun keyOfFoldsRegionsAndHasNoKeyOutsideItaly() {
        assertEquals("Piemonte", DealRegions.keyOf(piemonte))
        assertEquals("Piemonte", DealRegions.keyOf(aosta))
        assertEquals("Lombardia", DealRegions.keyOf(lombardia))
        assertNull(DealRegions.keyOf(nowhere))
        assertNull(DealRegions.keyOf(nan))
    }

    @Test
    fun regionsAreDistinctAndAlphabetical() {
        assertEquals(listOf("Lombardia", "Piemonte"), DealRegions.regions(all))
        assertEquals(emptyList(), DealRegions.regions(listOf(nowhere, nan)))
    }

    @Test
    fun filterPutsTheRegionFirstThenTheOnesValidEverywhere() {
        assertEquals(
            listOf("piemonte", "aosta", "nowhere", "nan"),
            DealRegions.filter(all, "Piemonte").map { it.id },
        )
        assertEquals(
            listOf("lombardia", "nowhere", "nan"),
            DealRegions.filter(all, "Lombardia").map { it.id },
        )
        assertEquals(listOf("nowhere", "nan"), DealRegions.filter(all, "Sicilia").map { it.id })
    }

    @Test
    fun filterWithoutRegionLeavesTheListUnchanged() {
        assertEquals(all, DealRegions.filter(all, null))
    }
}
