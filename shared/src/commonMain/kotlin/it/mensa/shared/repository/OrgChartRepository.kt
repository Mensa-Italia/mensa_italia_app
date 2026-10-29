package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.OrgChartApi
import it.mensa.shared.config.FeatureFlags
import it.mensa.shared.model.OrgChartModel

/**
 * Organigramma — repository sottilissimo: nessuna cache su disco perché il
 * dato cambia raramente, è poco voluminoso, e quando si passerà all'endpoint
 * server vero la roundtrip sarà comunque rapida. Se serve, in futuro
 * aggiungere un cache in-memory con TTL.
 *
 * Con `org_chart_enabled` spento non si chiede niente al server e
 * l'organigramma e' vuoto. Il flag decideva solo la voce del profilo: la
 * ricerca, che lo precarica a ogni apertura, e la rubrica lo leggevano
 * comunque, e con la lettura chiusa sul server ogni socio collezionava 403.
 * Qui vale per tutti i chiamanti, su entrambe le piattaforme.
 */
class OrgChartRepository(
    private val api: OrgChartApi,
    private val flags: FeatureFlags,
) {
    @Throws(Exception::class)
    suspend fun fetch(): OrgChartModel = if (flags.orgChartEnabled) api.get() else OrgChartModel()
}
