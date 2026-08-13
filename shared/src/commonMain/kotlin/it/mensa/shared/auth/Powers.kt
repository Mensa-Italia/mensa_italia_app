package it.mensa.shared.auth

/**
 * I permessi che il backend mette in `users.powers`, e la regola per leggerli.
 *
 * Sorgente unica per Android e iOS. Prima ogni schermata si scriveva la
 * propria lista, e le liste erano gia' divergenti fra loro e dal server:
 *  - iOS lasciava creare eventi solo a `super`, perche' cercava un
 *    `canAddEvent` che il backend non ha mai emesso;
 *  - le convenzioni su iOS erano riservate a `super`/`admin`/`deals_admin`,
 *    cioe' a nessuno tranne i super, perche' `admin` e `deals_admin` non
 *    esistono lato server e il vero `deals` non era nell'elenco;
 *  - su Android lo stesso controllo elencava quattro nomi, due dei quali
 *    inesistenti.
 *
 * [all] e' l'elenco vero, quello che il server assegna. Niente `admin`,
 * niente `*_admin`, niente `canAddEvent`.
 */
object Powers {

    const val SUPER = "super"
    const val SIGS = "sigs"
    const val EVENTS = "events"
    const val ADDONS = "addons"
    const val DEALS = "deals"
    const val TESTMAKERS = "testmakers"

    /**
     * Variante di [EVENTS] che il server NON tocca. `events` e' assegnato in
     * automatico e puo' essere revocato altrettanto in automatico: chi deve
     * mantenere il permesso a prescindere ha `events_helper`.
     */
    const val EVENTS_HELPER = "events_helper"

    val all: List<String> = listOf(SUPER, SIGS, EVENTS, EVENTS_HELPER, ADDONS, DEALS, TESTMAKERS)

    /**
     * True se [powers] concede [power]. `super` passa sempre, e vale anche il
     * gemello `<power>_helper` — oggi esiste solo per gli eventi, ma la regola
     * regge se il server ne aggiungera' altri.
     */
    fun has(powers: List<String>?, power: String): Boolean {
        val granted = powers ?: return false
        return granted.contains(SUPER) ||
            granted.contains(power) ||
            granted.contains(power + HELPER_SUFFIX)
    }

    /** Creare e modificare eventi. */
    fun canManageEvents(powers: List<String>?): Boolean = has(powers, EVENTS)

    /** Creare e modificare convenzioni. */
    fun canManageDeals(powers: List<String>?): Boolean = has(powers, DEALS)

    /** Creare e gestire i SIG. */
    fun canManageSigs(powers: List<String>?): Boolean = has(powers, SIGS)

    /** Gestire gli addon. */
    fun canManageAddons(powers: List<String>?): Boolean = has(powers, ADDONS)

    /** Accedere agli strumenti degli assistenti al test. */
    fun canManageTests(powers: List<String>?): Boolean = has(powers, TESTMAKERS)

    private const val HELPER_SUFFIX = "_helper"
}
