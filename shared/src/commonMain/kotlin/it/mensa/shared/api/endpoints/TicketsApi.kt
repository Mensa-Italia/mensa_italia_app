package it.mensa.shared.api.endpoints

import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.TicketModel

class TicketsApi(private val pb: PocketBaseClient) {

    @Throws(Exception::class)
    suspend fun list(
        filter: String? = null,
        sort: String = "-created",
    ): List<TicketModel> =
        pb.fullList("tickets", filter = filter, sort = sort)

    @Throws(Exception::class)
    suspend fun get(id: String): TicketModel =
        pb.getOne("tickets", id)
}
