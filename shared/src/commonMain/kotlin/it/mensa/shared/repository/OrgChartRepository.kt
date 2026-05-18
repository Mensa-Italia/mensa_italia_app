package it.mensa.shared.repository

import it.mensa.shared.api.endpoints.OrgChartApi
import it.mensa.shared.model.OrgChartModel

/**
 * Organigramma — repository sottilissimo: nessuna cache su disco perché il
 * dato cambia raramente, è poco voluminoso, e quando si passerà all'endpoint
 * server vero la roundtrip sarà comunque rapida. Se serve, in futuro
 * aggiungere un cache in-memory con TTL.
 */
class OrgChartRepository(private val api: OrgChartApi) {
    suspend fun fetch(): OrgChartModel = api.get()
}
