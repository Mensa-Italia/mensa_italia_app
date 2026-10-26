package it.mensa.shared.net

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectivityGateTest {
    private val now = Instant.parse("2026-10-26T12:00:00Z")
    private val hourAgo = Instant.parse("2026-10-26T11:00:00Z")
    private val twoDaysAgo = Instant.parse("2026-10-24T12:00:00Z")

    @Test
    fun onlineAlwaysOpens() {
        assertEquals(OnlineGate.OPEN, Connectivity.gate(true, twoDaysAgo, now))
    }

    /** Offline da poco: si entra, con l'avviso. */
    @Test
    fun recentlyOfflineOpens() {
        assertEquals(OnlineGate.OPEN, Connectivity.gate(false, hourAgo, now))
        assertEquals(OnlineGate.OPEN, Connectivity.gate(null, hourAgo, now))
    }

    @Test
    fun offlineForMoreThanADayBlocks() {
        assertEquals(OnlineGate.BLOCKED, Connectivity.gate(false, twoDaysAgo, now))
    }

    /** All'avvio la rete non ha ancora risposto: si aspetta invece di aprire e poi chiudere. */
    @Test
    fun unknownAfterADayWaits() {
        assertEquals(OnlineGate.CHECKING, Connectivity.gate(null, twoDaysAgo, now))
    }

    /** Mai un contatto registrato (app appena aggiornata): non si blocca. */
    @Test
    fun noRecordedContactOpens() {
        assertEquals(OnlineGate.OPEN, Connectivity.gate(false, null, now))
    }
}
