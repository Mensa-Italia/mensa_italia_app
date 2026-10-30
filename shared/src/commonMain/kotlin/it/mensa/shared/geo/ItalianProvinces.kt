package it.mensa.shared.geo

/**
 * Le 107 province italiane per regione, scritte come le scrive il registro
 * soci in `members_registry.state` (il campo "provincia" di cloud32): nomi
 * ISTAT correnti, "Bolzano" e "Aosta" senza la forma bilingue, "Sud Sardegna".
 * Verificato sui soci attivi di produzione: ogni provincia che vi compare e'
 * qui. Serve al sottofiltro "Provincia" del registro, che deve esserci prima
 * che i soci finiscano di caricare.
 *
 * Come [ItalianRegions] qui non si deduce niente: si confronta il nome.
 */
object ItalianProvinces {

    /** Regione (nome di [ItalianRegions.all]) -> le sue province, in ordine alfabetico. */
    val byRegion: Map<String, List<String>> = mapOf(
        "Abruzzo" to listOf("Chieti", "L'Aquila", "Pescara", "Teramo"),
        "Basilicata" to listOf("Matera", "Potenza"),
        "Calabria" to listOf("Catanzaro", "Cosenza", "Crotone", "Reggio di Calabria", "Vibo Valentia"),
        "Campania" to listOf("Avellino", "Benevento", "Caserta", "Napoli", "Salerno"),
        "Emilia-Romagna" to listOf(
            "Bologna", "Ferrara", "Forlì-Cesena", "Modena", "Parma",
            "Piacenza", "Ravenna", "Reggio nell'Emilia", "Rimini",
        ),
        "Friuli-Venezia Giulia" to listOf("Gorizia", "Pordenone", "Trieste", "Udine"),
        "Lazio" to listOf("Frosinone", "Latina", "Rieti", "Roma", "Viterbo"),
        "Liguria" to listOf("Genova", "Imperia", "La Spezia", "Savona"),
        "Lombardia" to listOf(
            "Bergamo", "Brescia", "Como", "Cremona", "Lecco", "Lodi",
            "Mantova", "Milano", "Monza e della Brianza", "Pavia", "Sondrio", "Varese",
        ),
        "Marche" to listOf("Ancona", "Ascoli Piceno", "Fermo", "Macerata", "Pesaro e Urbino"),
        "Molise" to listOf("Campobasso", "Isernia"),
        "Piemonte" to listOf(
            "Alessandria", "Asti", "Biella", "Cuneo", "Novara",
            "Torino", "Verbano-Cusio-Ossola", "Vercelli",
        ),
        "Puglia" to listOf("Bari", "Barletta-Andria-Trani", "Brindisi", "Foggia", "Lecce", "Taranto"),
        "Sardegna" to listOf("Cagliari", "Nuoro", "Oristano", "Sassari", "Sud Sardegna"),
        "Sicilia" to listOf(
            "Agrigento", "Caltanissetta", "Catania", "Enna", "Messina",
            "Palermo", "Ragusa", "Siracusa", "Trapani",
        ),
        "Toscana" to listOf(
            "Arezzo", "Firenze", "Grosseto", "Livorno", "Lucca",
            "Massa-Carrara", "Pisa", "Pistoia", "Prato", "Siena",
        ),
        "Trentino-Alto Adige" to listOf("Bolzano", "Trento"),
        "Umbria" to listOf("Perugia", "Terni"),
        "Valle d'Aosta" to listOf("Aosta"),
        "Veneto" to listOf("Belluno", "Padova", "Rovigo", "Treviso", "Venezia", "Verona", "Vicenza"),
    )

    /** Nome normalizzato -> nome canonico, cosi' "Forli Cesena" trova "Forlì-Cesena". */
    private val byNormalizedName: Map<String, String> =
        byRegion.values.flatten().associateBy { TextNormalizer.normalize(it) }

    /** Le province di [region] (anche col nome bilingue), vuoto se non e' una regione italiana. */
    fun of(region: String?): List<String> =
        ItalianRegions.canonical(region)?.let { byRegion[it] }.orEmpty()

    /**
     * Nome canonico di [raw], null se non e' una provincia italiana: la stringa
     * vuota e "-- STATO ESTERO/FOREIGN STATE--" di chi risiede all'estero
     * finiscono qui.
     */
    fun canonical(raw: String?): String? {
        val key = TextNormalizer.normalize(raw ?: return null)
        if (key.isEmpty()) return null
        return byNormalizedName[key]
    }
}
