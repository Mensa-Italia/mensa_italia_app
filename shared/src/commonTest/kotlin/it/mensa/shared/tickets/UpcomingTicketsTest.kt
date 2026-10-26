package it.mensa.shared.tickets

import it.mensa.shared.model.TicketModel
import it.mensa.shared.repository.TicketsRepository
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpcomingTicketsTest {
    private val now = Instant.parse("2026-10-26T20:00:00Z")
    private fun ticket(deadline: String?) =
        TicketModel(id = deadline ?: "none", deadline = deadline?.let(Instant::parse))

    @Test
    fun noTicketsMeansNoButton() {
        assertFalse(TicketsRepository.hasUpcoming(emptyList(), now))
    }

    @Test
    fun onlyPastOrUndatedTicketsMeansNoButton() {
        val tickets = listOf(ticket("2026-10-25T18:00:00Z"), ticket(null))
        assertFalse(TicketsRepository.hasUpcoming(tickets, now))
    }

    @Test
    fun oneFutureTicketIsEnough() {
        val tickets = listOf(ticket("2026-10-25T18:00:00Z"), ticket("2026-10-29T18:00:00Z"))
        assertTrue(TicketsRepository.hasUpcoming(tickets, now))
    }
}
