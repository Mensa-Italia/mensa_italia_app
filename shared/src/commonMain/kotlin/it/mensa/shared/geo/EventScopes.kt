package it.mensa.shared.geo

import it.mensa.shared.model.EventModel

/**
 * Ambito di un evento, cioe' come va etichettato in lista, sul dettaglio, sulla
 * mappa e nel filtro "Tipo evento".
 *
 * Fino a prima esistevano tre casi e uno era sbagliato: `is_national` da solo
 * decideva fra "Nazionale" e "Locale", quindi un raduno Mensa fuori dall'Italia
 * usciva identico a un evento nazionale italiano — stessa scritta, stesso
 * colore, stessa icona. La differenza c'e' e va vista.
 */
enum class EventScope {
    /** Nessuna posizione: l'evento si fa online. */
    ONLINE,

    /** In Italia, organizzato da una sede locale. */
    LOCAL,

    /** In Italia, organizzato dall'associazione nazionale (`is_national`). */
    NATIONAL,

    /** Fuori dall'Italia: la posizione non ricade in nessuna regione italiana. */
    INTERNATIONAL,
}

/**
 * Classifica un evento. Nessuna deduzione fantasiosa: la regione la calcola il
 * server e la scrive in `positions.state` (vedi [ItalianRegions]).
 *
 *  1. **Nessuna posizione** -> [EventScope.ONLINE]. E' l'unico modo che ha il
 *     modello per dire "online": non esiste un campo `is_online`.
 *  2. **Posizione senza regione italiana** -> [EventScope.INTERNATIONAL].
 *     Fuori dall'Italia `GET /api/position/state` risponde `NaN` e il record
 *     resta cosi': la posizione di Peisey-Nancroix (Francia) e' esattamente
 *     questo caso. Vale anche per `state` vuoto — se la regione non c'e',
 *     l'evento non e' in Italia.
 *  3. Altrimenti e' in Italia: [EventScope.NATIONAL] se `is_national`,
 *     [EventScope.LOCAL] se no.
 *
 * Nessun controllo sulle coordinate: un rettangolo attorno all'Italia non
 * separerebbe niente (Peisey-Nancroix sta a 6.80 E, 45.52 N, cioe' dentro), e
 * `state` da solo e' gia' la risposta giusta.
 *
 * I pochi record italiani rimasti indietro con `NaN` li ripara
 * [it.mensa.shared.repository.EventsRepository] al primo refresh richiedendo la
 * regione al server.
 */
object EventScopes {

    fun of(event: EventModel): EventScope {
        val position = event.position ?: return EventScope.ONLINE

        if (ItalianRegions.canonical(position.state) == null) return EventScope.INTERNATIONAL

        return if (event.isNational) EventScope.NATIONAL else EventScope.LOCAL
    }

    /** Scorciatoia per le UI che devono solo sapere se mostrare il globo. */
    fun isInternational(event: EventModel): Boolean = of(event) == EventScope.INTERNATIONAL
}
