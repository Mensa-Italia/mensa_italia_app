package it.mensa.shared.localgroup

import it.mensa.shared.model.EventExpand
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocalOfficeAdminModel
import it.mensa.shared.model.LocalOfficeModel
import it.mensa.shared.model.LocationModel
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalGroupTest {
    private val repo = LocalGroupRepository

    @Test
    fun laChiaveDelServerVinceSulNome() {
        assertEquals("Puglia", repo.officeRegionKey(LocalOfficeModel(region = "Qualcosa", regionKey = "Puglia")))
    }

    @Test
    fun senzaChiaveSiUsaLaMappaDelServer() {
        val cases = mapOf(
            "Basilicata e Puglia" to "Puglia",
            "Piemonte e Val d'Aosta" to "Piemonte",
            "Emilia Romagna" to "Emilia-Romagna",
            "Friuli Venezia Giulia" to "Friuli-Venezia Giulia",
            "Trentino Alto Adige" to "Trentino-Alto Adige",
            "Lombardia" to "Lombardia",
        )
        cases.forEach { (region, key) ->
            assertEquals(key, repo.officeRegionKey(LocalOfficeModel(region = region, regionKey = "")), region)
        }
    }

    @Test
    fun unEventoInValleDAostaEDelPiemonte() {
        val event = EventModel(expand = EventExpand(position = LocationModel(state = "Valle d'Aosta")))
        assertTrue(repo.belongsTo(event, "piemonte", "Piemonte"))
        assertFalse(repo.belongsTo(event, "lombardia", "Lombardia"))
        assertTrue(repo.belongsTo(EventModel(localOfficeId = "lombardia"), "lombardia", "Lombardia"))
    }

    @Test
    fun ilFiltroComprendeLeRegioniAccorpate() {
        assertEquals(
            "(local_office = \"x\" || position.state = \"Basilicata\" || position.state = \"Puglia\")",
            repo.regionFilter("x", "Puglia"),
        )
    }

    @Test
    fun ilSegretarioPrimaPoiIcoSegretariPerNome() {
        val sorted = repo.sortAdmins(
            listOf(
                LocalOfficeAdminModel(name = "Zeno"),
                LocalOfficeAdminModel(name = "Mario", isTheOfficer = true),
                LocalOfficeAdminModel(name = "anna"),
            ),
        )
        assertEquals(listOf("Mario", "anna", "Zeno"), sorted.map { it.name })
    }

    @Test
    fun ilPeriodoDellaStagioneParteDalPrimoOttobre() {
        val today = LocalDate(2026, 11, 8)
        assertEquals(LocalDate(2026, 10, 1), LocalGroupPeriod.SEASON.range(today).from)
        assertEquals(LocalDate(2026, 11, 1), LocalGroupPeriod.DAYS_7.range(today).from)
        assertEquals(LocalDate(2025, 10, 1), LocalGroupPeriod.SEASON.range(LocalDate(2026, 3, 2)).from)
    }

    @Test
    fun leFixtureHannoUnMesePerMeseDellaStagione() {
        val rows = LocalGroupFixtures.monthly(LocalDate(2026, 12, 3))
        assertEquals(listOf("2026-10", "2026-11", "2026-12"), rows.map { it.month })
        val changes = LocalGroupFixtures.changes(LocalDate(2026, 10, 1), LocalDate(2026, 11, 8))
        assertEquals(12, changes.size)
        assertEquals("2026-11-08", changes.first().dayIso)
        assertEquals(3, LocalGroupFixtures.snapshot().nextEvents.size)
        assertEquals(8, LocalGroupFixtures.officeEvents().size)
    }

    @Test
    fun ilNomeDelFile() {
        val office = LocalOfficeModel(name = "Mensa Lombardia", slug = "lombardia")
        assertEquals(
            "soci-nuovi-lombardia-2026-10-01_2026-11-08.xlsx",
            LocalGroupExportFile.fileName(office, ExportKind.NEW, LocalDate(2026, 10, 1), LocalDate(2026, 11, 8)),
        )
    }
}
