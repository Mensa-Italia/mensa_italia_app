package it.mensa.shared.notifications

/**
 * Typed destination for tapping a notification. Mirrors Flutter's
 * `handleNotificationActions` in `master_model.dart` and the previous
 * Swift `NotificationTarget` enum.
 *
 * Subclasses are declared at the top level (not nested) so they bridge
 * to Swift as `NotificationTargetEvent`, `NotificationTargetDeal`, etc.
 * — a flat namespace that's easier to pattern-match in Swift.
 */
sealed class NotificationTarget

data class NotificationTargetEvent(val id: String) : NotificationTarget()
data class NotificationTargetDeal(val id: String) : NotificationTarget()
data class NotificationTargetSingleDocument(val id: String) : NotificationTarget()
object NotificationTargetMultipleDocuments : NotificationTarget()
object NotificationTargetTicketPurchase : NotificationTarget()
object NotificationTargetPaymentUpdateStatus : NotificationTarget()

/** Opens a Quid issue. Payload carries the WP category id (e.g. 113). */
data class NotificationTargetQuid(val categoryId: String) : NotificationTarget()

/** Opens a Quid article. Payload carries the WP post id. */
data class NotificationTargetQuidArticle(val postId: String) : NotificationTarget()

/** Opens a Quid PDF issue (numbers 1..12). Payload carries the record id. */
data class NotificationTargetQuidPdf(val recordId: String) : NotificationTarget()

/** Opens the linktree-style page of a local office. Payload carries the slug. */
data class NotificationTargetLocalOffice(val slug: String) : NotificationTarget()

/** Il messaggio degli sviluppatori: non apre una pagina, rimostra la scheda. */
object NotificationTargetDeveloperMessage : NotificationTarget()

/**
 * Third-party data-access approval prompt. Payload carries the `ex_apps`
 * record id and the callback URL the host expects a `{"accepted": bool}`
 * POST on. `notificationId` is forwarded so the UI can mark the originating
 * notification seen after the user decides. Mirrors Flutter's
 * `account_confirmation` branch in `handleNotificationActions`.
 */
data class NotificationTargetAccountConfirmation(
    val exAppId: String,
    val callbackUrl: String,
    val notificationId: String?,
) : NotificationTarget()

/**
 * Che dato lascia l'app in un'esportazione verso un'app esterna. Il valore di
 * [wire] e' quello del campo `scope` del payload della push e del backend
 * (`ex_export_requests.scope`). Aggiungere un tipo di dato vuol dire aggiungere
 * un caso qui e le sue stringhe nelle due UI.
 */
enum class DataExportScope(val wire: String) {
    STAMPS("stamps"),
    MEDALS("medals");

    companion object {
        fun fromWire(value: String?): DataExportScope? = entries.firstOrNull { it.wire == value }
    }
}

/**
 * Richiesta di esportare una sola volta i dati del socio (timbri o medaglie)
 * verso un'app esterna, da confermare in app. Push `data_export_confirmation`:
 * `requestId` e' il record `ex_export_requests`, `keyAppId` l'`ex_apps` che
 * chiede, `url` il callback da avvisare dopo la decisione.
 */
data class NotificationTargetDataExportConfirmation(
    val requestId: String,
    val exAppId: String,
    val scope: DataExportScope,
    val callbackUrl: String,
    val notificationId: String?,
) : NotificationTarget()

/**
 * Stateless router. Both APNs `userInfo` and stored `NotificationModel.data`
 * (a Kotlin `JsonObject`) are normalized on the platform side to a flat
 * `Map<String, String>` before being passed in.
 */
object NotificationRouter {

    /**
     * Parse the flat payload `data` into a [NotificationTarget].
     * Field names mirror Flutter's `handleNotificationActions`
     * (`type`, `event_id`, `deal_id`, `document_id`, ...).
     *
     * Returns null when `type` is missing/unknown or when a required
     * id field is absent/empty/"null".
     */
    fun targetFromData(data: Map<String, String>): NotificationTarget? {
        val type = data["type"].orEmpty()
        fun str(key: String): String? {
            val v = data[key]
            return if (v.isNullOrEmpty() || v == "null") null else v
        }
        return when (type) {
            "event" -> str("event_id")?.let { NotificationTargetEvent(it) }
            "deal" -> str("deal_id")?.let { NotificationTargetDeal(it) }
            "single_document" -> str("document_id")?.let { NotificationTargetSingleDocument(it) }
            "multiple_documents" -> NotificationTargetMultipleDocuments
            "ticket_purchase" -> NotificationTargetTicketPurchase
            "payment_update_status" -> NotificationTargetPaymentUpdateStatus
            // `quid_id` e' quello che manda davvero il cron del backend
            // (`crons/quidnotify`); `category_id` resta per le notifiche vecchie.
            "quid" -> (str("category_id") ?: str("quid_id"))?.let { NotificationTargetQuid(it) }
            "quid_article" -> str("post_id")?.let { NotificationTargetQuidArticle(it) }
            "quid_pdf" -> str("record_id")?.let { NotificationTargetQuidPdf(it) }
            "local_office" -> str("slug")?.let { NotificationTargetLocalOffice(it) }
            "developer_message" -> NotificationTargetDeveloperMessage
            "account_confirmation" -> {
                val appId = str("keyAppId")
                val url = str("url")
                if (appId != null && url != null) {
                    NotificationTargetAccountConfirmation(
                        exAppId = appId,
                        callbackUrl = url,
                        notificationId = str("internal_id"),
                    )
                } else null
            }
            "data_export_confirmation" -> {
                val requestId = str("requestId")
                val appId = str("keyAppId")
                val scope = DataExportScope.fromWire(str("scope"))
                val url = str("url")
                if (requestId != null && appId != null && scope != null && url != null) {
                    NotificationTargetDataExportConfirmation(
                        requestId = requestId,
                        exAppId = appId,
                        scope = scope,
                        callbackUrl = url,
                        notificationId = str("internal_id"),
                    )
                } else null
            }
            else -> null
        }
    }

    /**
     * Resolve a system icon name for a given notification `type`.
     * Returns SF Symbol names (also used as a stable key Android can map to
     * Material icons later). Mirrors Flutter's `getBasedOnNotification`.
     */
    fun systemIconName(type: String): String = when (type) {
        "event" -> "star"
        "single_document", "multiple_documents" -> "doc.text"
        "account_confirmation" -> "person.badge.shield.checkmark"
        "data_export_confirmation" -> "square.and.arrow.up"
        "deal" -> "tag"
        "ticket_purchase" -> "ticket"
        "payment_update_status" -> "creditcard"
        "quid" -> "newspaper"
        "quid_article" -> "doc.richtext"
        "local_office" -> "building.2"
        "developer_message" -> "megaphone"
        else -> "bell"
    }
}
