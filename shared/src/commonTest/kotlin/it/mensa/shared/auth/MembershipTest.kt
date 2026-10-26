package it.mensa.shared.auth

import it.mensa.shared.model.UserModel
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Il gate del rinnovo chiude l'app fuori dalla pagina di rinnovo, quindi
 * sbagliare qui vuol dire chiudere fuori un socio in regola. I casi limite
 * sono fissati.
 */
class MembershipTest {

    private val now = Instant.parse("2026-08-22T12:00:00Z")

    private fun user(
        expiresAt: Instant? = null,
        active: Boolean = false,
    ) = UserModel(
        id = "u1",
        expireMembership = expiresAt ?: Instant.fromEpochMilliseconds(0),
        isMembershipActive = active,
    )

    @Test
    fun anExpiredCardIsExpired() {
        val lastYear = Instant.parse("2025-12-31T23:59:59Z")
        assertTrue(Membership.isExpired(user(expiresAt = lastYear), now))
    }

    @Test
    fun aValidCardIsNotExpired() {
        val nextYear = Instant.parse("2027-01-31T23:59:59Z")
        assertFalse(Membership.isExpired(user(expiresAt = nextYear), now))
    }

    @Test
    fun onlyTheDateCounts() {
        // `is_membership_active` non entra nella decisione: nessuna schermata
        // dell'app lo guarda, e come veto silenzioso avrebbe potuto tenere il
        // gate spento senza che si veda da nessuna parte.
        val lastYear = Instant.parse("2025-12-31T23:59:59Z")
        assertTrue(Membership.isExpired(user(expiresAt = lastYear, active = true), now))

        val nextYear = Instant.parse("2027-01-31T23:59:59Z")
        assertFalse(Membership.isExpired(user(expiresAt = nextYear, active = false), now))
    }

    @Test
    fun noDateMeansNoLockout() {
        // `expire_membership` assente o a zero: il server non lo sa, e non si
        // chiude fuori nessuno per un campo mancante.
        assertFalse(Membership.isExpired(user(), now))
        assertFalse(Membership.isExpired(null, now))
    }

    @Test
    fun daysUntilExpiryCountsInBothDirections() {
        assertEquals(9, Membership.daysUntilExpiry(user(expiresAt = Instant.parse("2026-08-31T12:00:00Z")), now))
        assertEquals(-10, Membership.daysUntilExpiry(user(expiresAt = Instant.parse("2026-08-12T12:00:00Z")), now))
        assertNull(Membership.daysUntilExpiry(user(), now))
        assertNull(Membership.daysUntilExpiry(null, now))
    }

    // ── Gate con la cache (avvio senza aspettare il server) ──────────────────

    private val expiredLastWeek = Instant.parse("2026-08-15T00:00:00Z")

    @Test
    fun activeOpensWhateverTheFreshness() {
        val ok = user(expiresAt = Instant.parse("2027-01-01T00:00:00Z"))
        assertEquals(MembershipGate.OPEN, Membership.gate(ok, verified = false, checking = true, syncedAt = null, now = now))
    }

    @Test
    fun expiredForTheServerClosesAtOnce() {
        val u = user(expiresAt = expiredLastWeek)
        // Confermata adesso.
        assertEquals(MembershipGate.EXPIRED, Membership.gate(u, verified = true, checking = false, syncedAt = null, now = now))
        // Gia' scaduta all'ultimo controllo: anche offline, anche durante la verifica.
        val syncedAfter = Instant.parse("2026-08-20T00:00:00Z")
        assertEquals(MembershipGate.EXPIRED, Membership.gate(u, verified = false, checking = true, syncedAt = syncedAfter, now = now))
        assertEquals(MembershipGate.EXPIRED, Membership.gate(u, verified = false, checking = false, syncedAt = syncedAfter, now = now))
    }

    /** Scaduta solo dopo l'ultimo controllo: lo splash aspetta il server, nessuna Home chiusa in faccia. */
    @Test
    fun lapsedSinceTheLastCheckWaitsForTheServer() {
        val u = user(expiresAt = expiredLastWeek)
        val syncedBefore = Instant.parse("2026-08-01T00:00:00Z")
        assertEquals(MembershipGate.CHECKING, Membership.gate(u, verified = false, checking = true, syncedAt = syncedBefore, now = now))
        assertEquals(MembershipGate.CHECKING, Membership.gate(u, verified = false, checking = true, syncedAt = null, now = now))
    }

    /** Offline (il controllo e' finito senza risposta): non si chiude fuori nessuno su una data non verificabile. */
    @Test
    fun lapsedAndUnverifiableOpens() {
        val u = user(expiresAt = expiredLastWeek)
        val syncedBefore = Instant.parse("2026-08-01T00:00:00Z")
        assertEquals(MembershipGate.OPEN, Membership.gate(u, verified = false, checking = false, syncedAt = syncedBefore, now = now))
    }
}
