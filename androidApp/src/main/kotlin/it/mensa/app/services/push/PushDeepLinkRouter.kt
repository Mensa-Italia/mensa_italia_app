package it.mensa.app.services.push

/**
 * PushDeepLinkRouter — maps FCM notification payloads to typed navigation targets.
 *
 * Add new target variants here as new push notification types are introduced.
 * The router parses the FCM `data` payload map into a [NotificationTarget].
 */
object PushDeepLinkRouter {

    // ─── Notification targets ─────────────────────────────────────────────────

    sealed class NotificationTarget {
        /** Mensa event detail */
        data class Event(val eventId: String) : NotificationTarget()

        /** Deal / offer detail */
        data class Deal(val dealId: String) : NotificationTarget()

        /** Document viewer */
        data class Document(val documentId: String) : NotificationTarget()

        /** Ticket detail */
        data class Ticket(val ticketId: String) : NotificationTarget()

        /** Quid balance update */
        data class Quid(val transactionId: String? = null) : NotificationTarget()

        /** Local office detail */
        data class LocalOffice(val officeId: String) : NotificationTarget()

        /** Generic URL (fallback — open in Chrome Custom Tab) */
        data class ExternalUrl(val url: String) : NotificationTarget()

        /** Unknown or unparsable payload */
        object Unknown : NotificationTarget()
    }

    // ─── FCM payload keys ─────────────────────────────────────────────────────

    private const val KEY_TYPE = "type"
    private const val KEY_ID = "id"
    private const val KEY_URL = "url"

    // Payload type constants (must match backend push notification templates)
    private const val TYPE_EVENT = "event"
    private const val TYPE_DEAL = "deal"
    private const val TYPE_DOCUMENT = "document"
    private const val TYPE_TICKET = "ticket"
    private const val TYPE_QUID = "quid"
    private const val TYPE_LOCAL_OFFICE = "local_office"
    private const val TYPE_URL = "url"

    /**
     * Parse a FCM data payload into a [NotificationTarget].
     *
     * @param data FCM `RemoteMessage.data` map
     */
    fun parse(data: Map<String, String>): NotificationTarget {
        val type = data[KEY_TYPE] ?: return NotificationTarget.Unknown
        val id = data[KEY_ID]
        return when (type) {
            TYPE_EVENT -> if (id != null) NotificationTarget.Event(id) else NotificationTarget.Unknown
            TYPE_DEAL -> if (id != null) NotificationTarget.Deal(id) else NotificationTarget.Unknown
            TYPE_DOCUMENT -> if (id != null) NotificationTarget.Document(id) else NotificationTarget.Unknown
            TYPE_TICKET -> if (id != null) NotificationTarget.Ticket(id) else NotificationTarget.Unknown
            TYPE_QUID -> NotificationTarget.Quid(id)
            TYPE_LOCAL_OFFICE -> if (id != null) NotificationTarget.LocalOffice(id) else NotificationTarget.Unknown
            TYPE_URL -> {
                val url = data[KEY_URL] ?: return NotificationTarget.Unknown
                NotificationTarget.ExternalUrl(url)
            }
            else -> NotificationTarget.Unknown
        }
    }
}
