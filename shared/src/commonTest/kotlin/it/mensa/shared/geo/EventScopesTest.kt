package it.mensa.shared.geo

import it.mensa.shared.model.EventExpand
import it.mensa.shared.model.EventModel
import it.mensa.shared.model.LocationModel
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Prima "Nazionale" e "Internazionale" erano graficamente lo stesso evento:
 * `is_national` decideva da solo, e un raduno fuori dall'Italia usciva con la
 * stessa etichetta di un evento nazionale italiano. Qui si fissa la regola che
 * li separa: la regione di `positions.state`, e nient'altro.
 */
class EventScopesTest {

    private fun event(
        isNational: Boolean = false,
        position: LocationModel? = null,
    ) = EventModel(
        id = "e1",
        name = "Evento",
        isNational = isNational,
        positionId = position?.id,
        expand = position?.let { EventExpand(position = it) },
    )

    private fun place(
        state: String,
        lat: Double = 0.0,
        lon: Double = 0.0,
    ) = LocationModel(id = "p1", name = "Posto", lat = lat, lon = lon, state = state)

    @Test
    fun noPositionMeansOnline() {
        assertEquals(EventScope.ONLINE, EventScopes.of(event()))
        // `is_national` non cambia niente: senza posizione l'evento e' online.
        assertEquals(EventScope.ONLINE, EventScopes.of(event(isNational = true)))
    }

    @Test
    fun italianRegionSplitsLocalFromNational() {
        val milano = place("Lombardia", lat = 45.46, lon = 9.19)
        assertEquals(EventScope.LOCAL, EventScopes.of(event(position = milano)))
        assertEquals(
            EventScope.NATIONAL,
            EventScopes.of(event(isNational = true, position = milano)),
        )
    }

    @Test
    fun nanStateMeansInternational() {
        // "NaN" e' cio' che /api/position/state risponde fuori dall'Italia:
        // e' il caso dell'evento a Perth citato in EventsRepository.
        val perth = place("NaN", lat = -31.95, lon = 115.86)
        assertEquals(EventScope.INTERNATIONAL, EventScopes.of(event(position = perth)))
        assertEquals(
            EventScope.INTERNATIONAL,
            EventScopes.of(event(isNational = true, position = perth)),
        )
    }

    @Test
    fun aForeignPlaceNextDoorIsStillInternational() {
        // positions/ks88ths3nytoe56, dai dati veri: Peisey-Nancroix, Francia.
        // Le coordinate cadono dentro qualunque rettangolo disegnato attorno
        // all'Italia — 45.52 N, 6.80 E — quindi contano solo perche' `state`
        // dice "NaN". E' il motivo per cui non c'e' nessun test geometrico.
        val peisey = place("NaN", lat = 45.518208, lon = 6.8047911)
        assertEquals(EventScope.INTERNATIONAL, EventScopes.of(event(position = peisey)))
    }

    @Test
    fun anEmptyStateIsInternationalToo() {
        // Nessuna regione = non e' in Italia.
        assertEquals(EventScope.INTERNATIONAL, EventScopes.of(event(position = place(""))))
    }

    @Test
    fun isInternationalMatchesOf() {
        val perth = place("NaN", lat = -31.95, lon = 115.86)
        val milano = place("Lombardia", lat = 45.46, lon = 9.19)
        assertEquals(true, EventScopes.isInternational(event(position = perth)))
        assertEquals(false, EventScopes.isInternational(event(position = milano)))
        assertEquals(false, EventScopes.isInternational(event()))
    }
}
