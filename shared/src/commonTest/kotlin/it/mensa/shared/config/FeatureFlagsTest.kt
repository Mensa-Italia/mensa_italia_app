package it.mensa.shared.config

import it.mensa.shared.model.search.SearchType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * I flag spegnevano solo le voci di menu: con `org_chart_enabled = false`
 * l'organigramma spariva dal profilo ma i suoi risultati continuavano a
 * uscire nella ricerca globale.
 */
class FeatureFlagsTest {

    @Test
    fun organigrammaSpentoNonEsceNellaRicerca() {
        val spento = FeatureFlags.Flags(orgChart = false)
        assertFalse(spento.allowsSearchType(SearchType.ORG))

        val acceso = FeatureFlags.Flags(orgChart = true)
        assertTrue(acceso.allowsSearchType(SearchType.ORG))
    }

    @Test
    fun gruppiLocaliSpentiNonEsconoNellaRicerca() {
        assertFalse(FeatureFlags.Flags(localGroups = false).allowsSearchType(SearchType.LINKTREE_LINK))
        assertTrue(FeatureFlags.Flags(localGroups = true).allowsSearchType(SearchType.LINKTREE_LINK))
    }

    @Test
    fun gliAltriTipiNonDipendonoDaiFlag() {
        val tuttoSpento = FeatureFlags.Flags()
        listOf(
            SearchType.EVENT,
            SearchType.DEAL,
            SearchType.USER,
            SearchType.DOCUMENT,
            SearchType.SIG,
            SearchType.ADDON,
        ).forEach { tipo ->
            assertTrue(tuttoSpento.allowsSearchType(tipo), "$tipo non dipende da un flag")
        }
    }

    @Test
    fun unTipoSconosciutoPassa() {
        // Il server puo' aggiungere tipi nuovi: non devono sparire per errore.
        assertTrue(FeatureFlags.Flags().allowsSearchType("qualcosa_di_nuovo"))
    }

    @Test
    fun iFlagSonoIndipendenti() {
        val soloOrg = FeatureFlags.Flags(orgChart = true, localGroups = false)
        assertTrue(soloOrg.allowsSearchType(SearchType.ORG))
        assertFalse(soloOrg.allowsSearchType(SearchType.LINKTREE_LINK))
    }
}
