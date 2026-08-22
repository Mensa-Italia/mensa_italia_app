package it.mensa.app.features.events.util

import android.location.Location
import it.mensa.shared.geo.EventScope
import it.mensa.shared.geo.EventScopes
import it.mensa.shared.geo.ItalianRegions
import it.mensa.shared.model.EventModel

/**
 * EventFilterHelpers — Android equivalent of iOS EventFilterHelpers.swift.
 * Pure, deterministic filter predicates with no side effects.
 */

enum class EventType(val label: String, val scope: EventScope) {
    NATIONAL("Nazionale", EventScope.NATIONAL),
    INTERNATIONAL("Internazionale", EventScope.INTERNATIONAL),
    LOCAL("Locale", EventScope.LOCAL),
    ONLINE("Online", EventScope.ONLINE),
}

// L'elenco delle regioni vive in :shared (it.mensa.shared.geo.ItalianRegions):
// chip del filtro, resolver, iOS e web devono leggere gli stessi identici nomi,
// altrimenti il confronto non trova mai niente.

object DistanceSteps {
    val kmValues = listOf(5, 25, 50, 100, 200, 500)
    fun label(km: Int?) = if (km == null) "Illimitato" else "$km km"
}

data class EventFilterState(
    val types: Set<EventType> = emptySet(),
    val regions: Set<String> = emptySet(),
    val maxDistanceKm: Int? = null,
    val useMyLocation: Boolean = false,
    val userLatitude: Double? = null,
    val userLongitude: Double? = null,
) {
    val activeCount: Int get() {
        var n = 0
        if (types.isNotEmpty()) n++
        if (regions.isNotEmpty()) n++
        if (useMyLocation && maxDistanceKm != null) n++
        return n
    }
    val isEmpty: Boolean get() = activeCount == 0

    fun reset() = copy(
        types = emptySet(),
        regions = emptySet(),
        maxDistanceKm = null,
        useMyLocation = false,
    )
}

object EventFilterHelpers {
    /**
     * Chi decide l'ambito e' [EventScopes] in :shared, cosi' la chip della
     * lista, il badge del dettaglio, il pin sulla mappa e iOS raccontano tutti
     * la stessa cosa.
     */
    fun typeOf(event: EventModel): EventType = when (EventScopes.of(event)) {
        EventScope.ONLINE -> EventType.ONLINE
        EventScope.LOCAL -> EventType.LOCAL
        EventScope.NATIONAL -> EventType.NATIONAL
        EventScope.INTERNATIONAL -> EventType.INTERNATIONAL
    }

    fun matchesType(event: EventModel, types: Set<EventType>): Boolean {
        if (types.isEmpty()) return true
        return types.contains(typeOf(event))
    }

    /**
     * Confronta la regione della posizione con quelle selezionate.
     *
     * Prima si cercava il nome della regione dentro l'indirizzo: gli indirizzi
     * italiani la regione non la scrivono ("11100 Aosta AO"), le vie invece
     * spesso si', e filtrando per Sicilia usciva l'evento di "Via Sicilia" a
     * Chieri. Ora si legge `position.state`, che e' il campo che il backend
     * calcola con `/api/position/state` e che EventsRepository riempie quando
     * arriva vuoto.
     */
    fun matchesRegion(event: EventModel, regions: Set<String>): Boolean =
        ItalianRegions.matches(event.position?.state, regions)

    fun matchesDistance(
        event: EventModel,
        maxDistanceKm: Int?,
        useMyLocation: Boolean,
        userLatitude: Double?,
        userLongitude: Double?,
    ): Boolean {
        if (!useMyLocation || maxDistanceKm == null || userLatitude == null || userLongitude == null) return true
        val pos = event.position ?: return false
        val results = FloatArray(1)
        Location.distanceBetween(userLatitude, userLongitude, pos.lat, pos.lon, results)
        val km = results[0] / 1000.0
        return km <= maxDistanceKm
    }

    fun matches(event: EventModel, state: EventFilterState): Boolean =
        matchesType(event, state.types)
            && matchesRegion(event, state.regions)
            && matchesDistance(
                event,
                state.maxDistanceKm,
                state.useMyLocation,
                state.userLatitude,
                state.userLongitude,
            )
}
