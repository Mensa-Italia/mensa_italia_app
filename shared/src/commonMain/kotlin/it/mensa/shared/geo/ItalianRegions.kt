package it.mensa.shared.geo

/**
 * Le 20 regioni italiane come le scrive il backend, e il confronto per il
 * filtro "Regione" della lista eventi.
 *
 * Qui NON si deduce niente. La regione di una posizione la calcola il server e
 * la scrive in `positions.state`: `GET /api/position/state?lat=&lon=` risponde
 * con esattamente questi nomi e con `NaN` quando il punto non e' in Italia
 * (verificato sui dati di produzione: 236 eventi con una delle 20 regioni, 31
 * con `NaN`, nessuno vuoto). Prima il filtro cercava invece il nome della
 * regione dentro l'indirizzo, che in Italia non la contiene quasi mai
 * ("11100 Aosta AO") e che spesso contiene vie intitolate a regioni: filtrando
 * per Sicilia usciva l'evento di "Via Sicilia" a Chieri, che e' in Piemonte.
 *
 * L'elenco resta scritto qui perche' serve alle chip del filtro e il backend
 * non espone un endpoint che le elenchi (`/api/regions`, `/api/position/states`
 * -> 404).
 */
object ItalianRegions {

    val all: List<String> = listOf(
        "Abruzzo",
        "Basilicata",
        "Calabria",
        "Campania",
        "Emilia-Romagna",
        "Friuli-Venezia Giulia",
        "Lazio",
        "Liguria",
        "Lombardia",
        "Marche",
        "Molise",
        "Piemonte",
        "Puglia",
        "Sardegna",
        "Sicilia",
        "Toscana",
        "Trentino-Alto Adige",
        "Umbria",
        "Valle d'Aosta",
        "Veneto",
    )

    /** Nome normalizzato -> nome canonico, cosi' "Emilia Romagna" trova "Emilia-Romagna". */
    private val byNormalizedName: Map<String, String> =
        all.associateBy { TextNormalizer.normalize(it) }

    /**
     * Nome canonico di [raw], null se non e' una regione italiana. Il `NaN` del
     * backend e la stringa vuota finiscono qui, quindi non matchano nessuna chip.
     */
    fun canonical(raw: String?): String? {
        val key = TextNormalizer.normalize(raw ?: return null)
        if (key.isEmpty()) return null
        return byNormalizedName[key]
    }

    /**
     * True se [state] (il campo `positions.state`) e' fra le [selected].
     * Insieme vuoto = nessun filtro, quindi passa tutto.
     */
    fun matches(state: String?, selected: Set<String>): Boolean {
        if (selected.isEmpty()) return true
        val region = canonical(state) ?: return false
        return selected.any { it == region || canonical(it) == region }
    }
}
