package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.ReceiptModel
import kotlinx.serialization.Serializable

@Serializable
data class ReceiptUrlResponse(
    val url: String = "",
)

/**
 * Payments / receipts endpoints. Receipts live in the PocketBase `payments`
 * collection; PDF receipt downloads are signed by /api/payment/receipt-url.
 */
class ReceiptsApi(
    private val pb: PocketBaseClient,
    private val client: HttpClient,
) {
    suspend fun list(
        filter: String? = null,
        sort: String = "-created",
    ): List<ReceiptModel> =
        pb.fullList("payments", filter = filter, sort = sort)

    suspend fun get(id: String): ReceiptModel =
        pb.getOne("payments", id)

    /**
     * GET /api/payment/receipt-url?id=<receiptId> returns a signed PDF URL.
     */
    suspend fun getReceiptUrl(id: String): String {
        val resp: ReceiptUrlResponse = client.get("/api/payment/receipt-url") {
            parameter("id", id)
        }.body()
        return resp.url
    }
}
