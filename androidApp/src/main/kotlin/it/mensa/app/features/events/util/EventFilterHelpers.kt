package it.mensa.app.features.events.util

import android.location.Location
import it.mensa.shared.model.EventModel

/**
 * EventFilterHelpers — Android equivalent of iOS EventFilterHelpers.swift.
 * Pure, deterministic filter predicates with no side effects.
 */

enum class EventType(val label: String, val icon: String) {
    NATIONAL("Nazionale", "globe"),
    LOCAL("Locale", "mappin"),
    ONLINE("Online", "wifi"),
}

object ItalianRegions {
    val all = listOf(
        "Abruzzo", "Basilicata", "Calabria", "Campania", "Emilia-Romagna",
        "Friuli-Venezia Giulia", "Lazio", "Liguria", "Lombardia", "Marche",
        "Molise", "Piemonte", "Puglia", "Sardegna", "Sicilia", "Toscana",
        "Trentino-Alto Adige", "Umbria", "Valle d'Aosta", "Veneto",
    )
}

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
    fun typeOf(event: EventModel): EventType = when {
        event.position == null -> EventType.ONLINE
        event.isNational -> EventType.NATIONAL
        else -> EventType.LOCAL
    }

    fun matchesType(event: EventModel, types: Set<EventType>): Boolean {
        if (types.isEmpty()) return true
        return types.contains(typeOf(event))
    }

    fun matchesRegion(event: EventModel, regions: Set<String>): Boolean {
        if (regions.isEmpty()) return true
        val address = event.position?.address ?: return false
        if (address.isBlank()) return false
        val lower = address.lowercase()
        return regions.any { lower.contains(it.lowercase()) }
    }

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
