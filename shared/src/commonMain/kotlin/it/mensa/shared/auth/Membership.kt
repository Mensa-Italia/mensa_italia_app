package it.mensa.shared.auth

import it.mensa.shared.model.UserModel
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Stato della tessera associativa.
 *
 * Serve a decidere una cosa sola, ma pesante: se l'app si apre normalmente o
 * se si apre sulla sola pagina di rinnovo. Prima quella decisione non esisteva
 * — con la tessera scaduta si entrava e si vedeva tutto — e la scadenza usciva
 * solo come scritta rossa dentro Profilo → Tessera.
 *
 * La regola vive qui e non nelle due UI perche' Android e iOS devono chiudere
 * la porta nello stesso identico momento.
 */
object Membership {

    /**
     * True quando la tessera risulta scaduta e l'app va messa in sola lettura
     * sulla pagina di rinnovo.
     *
     * Conta solo `expire_membership`, ed e' voluto: e' il campo su cui gia'
     * si basano la scritta "Tessera scaduta" della tessera e della pagina di
     * rinnovo. Se quelle dicono scaduta, il gate deve chiudere — due regole
     * diverse sullo stesso fatto sarebbero un modo per farlo sembrare rotto.
     * `is_membership_active` non lo guarda nessuna schermata dell'app e non
     * viene guardato nemmeno qui: come veto silenzioso avrebbe potuto
     * disattivare il gate senza che si veda da nessuna parte.
     *
     * Una data assente (epoch 0) vale "il server non lo sa" e non blocca
     * nessuno: il costo di sbagliare qui e' un socio in regola chiuso fuori
     * dalla sua app.
     */
    fun isExpired(user: UserModel?, now: Instant): Boolean {
        val u = user ?: return false
        val expiresAtMs = u.expireMembership.toEpochMilliseconds()
        if (expiresAtMs <= 0L) return false
        return expiresAtMs < now.toEpochMilliseconds()
    }

    /**
     * Come [isExpired], adesso.
     *
     * Overload esplicito invece di un parametro con valore di default: i
     * default di Kotlin non arrivano in Swift, e senza questa firma iOS
     * dovrebbe costruirsi un `Clock.System.now()` a mano a ogni chiamata.
     */
    fun isExpired(user: UserModel?): Boolean = isExpired(user, Clock.System.now())

    /** Giorni che mancano alla scadenza; negativo se e' gia' passata. */
    fun daysUntilExpiry(user: UserModel?, now: Instant): Long? {
        val u = user ?: return null
        val expiresAtMs = u.expireMembership.toEpochMilliseconds()
        if (expiresAtMs <= 0L) return null
        return (expiresAtMs - now.toEpochMilliseconds()) / MILLIS_PER_DAY
    }

    /** Come [daysUntilExpiry], adesso. Stesso motivo dell'overload sopra. */
    fun daysUntilExpiry(user: UserModel?): Long? = daysUntilExpiry(user, Clock.System.now())

    /**
     * Portale esterno dove si rinnova (cloud32.it).
     *
     * `val` e non `const val` — e per questo detekt va zittito: un `const`
     * dentro un `object` non arriva a Swift come proprieta' di
     * `Membership.shared`, e questo indirizzo lo legge anche la schermata iOS
     * del muro del rinnovo.
     */
    @Suppress("MayBeConst")
    val RENEWAL_URL: String = "https://www.cloud32.it/Associazioni/utenti/richirinnovo"

    private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000
}
