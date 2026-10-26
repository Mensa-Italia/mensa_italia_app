package it.mensa.shared.api.endpoints

import io.ktor.client.HttpClient
import it.mensa.shared.api.PocketBaseClient
import it.mensa.shared.model.NotificationModel
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

@Serializable
internal data class SeenBody(val seen: String)

class NotificationsApi(
    private val pb: PocketBaseClient,
    @Suppress("unused") private val client: HttpClient,
) {
    @Throws(Exception::class)
    suspend fun list(): List<NotificationModel> =
        pb.fullList("user_notifications", sort = "-created")

    /// Tutte le notifiche non lette (seen == null), nessun limite.
    @Throws(Exception::class)
    suspend fun listUnread(): List<NotificationModel> =
        pb.fullList("user_notifications", filter = "seen = null", sort = "-created")

    /// Le ultime `limit` notifiche gia' lette (seen != null), ordinate dalla
    /// piu' recente. Default 20 — vedi `NotificationsRepository.refresh()`.
    @Throws(Exception::class)
    suspend fun listRecentSeen(limit: Int = 20): List<NotificationModel> =
        pb.list<NotificationModel>(
            collection = "user_notifications",
            page = 1,
            perPage = limit,
            filter = "seen != null",
            sort = "-created"
        ).items

    @Throws(Exception::class)
    suspend fun see(id: String): NotificationModel =
        pb.update("user_notifications", id, SeenBody(Clock.System.now().toString()))

    @Throws(Exception::class)
    suspend fun delete(id: String) =
        pb.delete("user_notifications", id)
}
