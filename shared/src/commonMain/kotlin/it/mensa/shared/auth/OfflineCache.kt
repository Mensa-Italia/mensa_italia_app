package it.mensa.shared.auth

import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Quanto a lungo i dati in locale (registro soci con telefoni ed email,
 * documenti, ricevute...) restano leggibili senza che il server confermi il
 * socio.
 *
 * Prima non c'era nessun limite: l'avvio dalla cache non scadeva mai, e un
 * socio sospeso o espulso poteva tenere l'app in modalita' aereo e continuare
 * a consultare il registro per sempre, perche' il server — che lo avrebbe
 * fermato — non veniva mai interpellato.
 *
 * La regola: se l'ultima volta che il server ha mandato il record del socio
 * (`auth.current_user.synced_at`, scritto a ogni login e a ogni `/me` riuscito)
 * e' piu' vecchia di [MAX_AGE], i dati locali si cancellano. Chi e' online non
 * se ne accorge: ogni avvio e ogni ritorno in primo piano rinnovano l'orario.
 */
object OfflineCache {

    /** Quanto puo' vivere la cache senza una conferma del server. */
    val MAX_AGE: Duration = 24.hours

    /**
     * Quanto l'ultima conferma puo' stare "nel futuro" prima di considerarla
     * un orologio portato indietro. Un minimo di tolleranza serve perche'
     * l'ora la scrive il telefono stesso e puo' essere corretta via rete.
     */
    val CLOCK_SKEW: Duration = 5.minutes

    /**
     * True quando la cache va buttata.
     *
     *  - nessuna conferma registrata: non si sa quanto e' vecchia, quindi si
     *    tratta come scaduta (succede anche aggiornando da una versione che
     *    non salvava l'orario: il primo avvio online la riscrive subito);
     *  - conferma "nel futuro" oltre [CLOCK_SKEW]: qualcuno ha spostato
     *    indietro la data del telefono per tenere viva la cache;
     *  - conferma piu' vecchia di [MAX_AGE].
     */
    fun isStale(syncedAt: Instant?, now: Instant): Boolean {
        val confirmed = syncedAt ?: return true
        if (confirmed > now + CLOCK_SKEW) return true
        return now - confirmed > MAX_AGE
    }
}
