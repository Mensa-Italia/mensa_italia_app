package it.mensa.shared.members

import it.mensa.shared.model.RegSociModel

/**
 * Un registro soci inventato, per vedere il registro e il suo filtro per
 * regione senza login (`MENSA_REGSOCI_FIXTURE=1` con
 * `MENSA_LAUNCH_SCREEN=members` su iOS, `RegistryPreviewActivity` nella build
 * debug Android). Solo gli harness di debug lo accendono; spento,
 * [it.mensa.shared.repository.RegSociRepository] legge il server. Ogni nome e'
 * inventato, quindi le schermate non espongono nessuno.
 */
object RegSociFixtures {

    /** True solo quando lo accende un harness di debug. */
    var enabled: Boolean = false

    // Nome, citta' (= provincia), regione come la scrive il server.
    private val rows = listOf(
        Triple("Chiara Valenti", "Torino", "Piemonte"),
        Triple("Marco Ferretti", "Torino", "Piemonte"),
        Triple("Lorenzo Gatti", "Novara", "Piemonte"),
        Triple("Sofia Marchetti", "Cuneo", "Piemonte"),
        Triple("Tommaso Ferri", "Milano", "Lombardia"),
        Triple("Giulia Rinaldi", "Bergamo", "Lombardia"),
        Triple("Matteo Bellini", "Brescia", "Lombardia"),
        Triple("Aurora Costa", "Monza e della Brianza", "Lombardia"),
        Triple("Edoardo Fontana", "Venezia", "Veneto"),
        Triple("Beatrice Leone", "Verona", "Veneto"),
        Triple("Davide Moretti", "Bolzano", "Trentino-Alto Adige/Südtirol"),
        Triple("Martina Galli", "Trento", "Trentino-Alto Adige/Südtirol"),
        Triple("Riccardo Conti", "Trieste", "Friuli-Venezia Giulia"),
        Triple("Elena Barbieri", "Genova", "Liguria"),
        Triple("Francesco Testa", "Bologna", "Emilia-Romagna"),
        Triple("Alice Serra", "Parma", "Emilia-Romagna"),
        Triple("Pietro Longo", "Firenze", "Toscana"),
        Triple("Giorgia Bianchi", "Pisa", "Toscana"),
        Triple("Nicolò Ferrara", "Perugia", "Umbria"),
        Triple("Camilla Rizzi", "Ancona", "Marche"),
        Triple("Andrea Pellegrini", "Roma", "Lazio"),
        Triple("Ginevra Caruso", "Roma", "Lazio"),
        Triple("Leonardo Mancini", "Latina", "Lazio"),
        Triple("Vittoria Greco", "Pescara", "Abruzzo"),
        Triple("Gabriele Marino", "Napoli", "Campania"),
        Triple("Anna Lombardi", "Salerno", "Campania"),
        Triple("Simone Villa", "Bari", "Puglia"),
        Triple("Ludovica Sala", "Lecce", "Puglia"),
        Triple("Alessandro Farina", "Cosenza", "Calabria"),
        Triple("Rebecca Ricci", "Palermo", "Sicilia"),
        Triple("Federico Neri", "Catania", "Sicilia"),
        Triple("Irene Colombo", "Cagliari", "Sardegna"),
        Triple("Paolo Medas", "Sud Sardegna", "Sardegna"),
        Triple("Samuele De Luca", "Aosta", "Valle d'Aosta"),
        Triple("Noemi Fabbri", "Lugano", "NaN"),
    )

    val members: List<RegSociModel> get() = rows.mapIndexed { index, (name, city, area) ->
        RegSociModel(
            id = (10_001 + index * 37).toString(),
            name = name,
            city = city,
            state = if (area == "NaN") "" else city,
            area = area,
        )
    }
}
