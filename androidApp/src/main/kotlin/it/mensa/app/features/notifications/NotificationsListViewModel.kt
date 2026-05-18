package it.mensa.app.features.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.koinAccess
import it.mensa.shared.model.NotificationModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// ─── Notification target ──────────────────────────────────────────────────────

/**
 * Typed deep-link destination parsed from NotificationModel.data.
 * Mirrors iOS NotificationTarget enum in NotificationsListView.swift.
 */
sealed class NotificationTarget {
    data class Event(val eventId: String) : NotificationTarget()
    data class Deal(val dealId: String) : NotificationTarget()
    data class SingleDocument(val documentId: String) : NotificationTarget()
    object MultipleDocuments : NotificationTarget()
    object TicketPurchase : NotificationTarget()
    object PaymentUpdateStatus : NotificationTarget()
    /** Opens a specific Quid issue (category_id). */
    data class Quid(val categoryId: String) : NotificationTarget()
    /** Opens a specific Quid article (wp_post_id). */
    data class QuidArticle(val postId: String) : NotificationTarget()
    /** Opens a Quid PDF issue (record_id). */
    data class QuidPdf(val recordId: String) : NotificationTarget()
    /** Opens a local office by slug. */
    data class LocalOffice(val slug: String) : NotificationTarget()
    /**
     * Third-party data-access approval prompt. Carries the `ex_apps` record id
     * and the callback URL the caller expects a `{"accepted": bool}` POST on.
     * Mirrors iOS `AccountConfirmationSheet`.
     */
    data class AccountConfirmation(
        val exAppId: String,
        val callbackUrl: String,
        val notificationId: String?,
    ) : NotificationTarget()
}

// ─── Grouping ─────────────────────────────────────────────────────────────────

enum class NotificationGroup(val titleKey: String, val fallback: String) {
    Today("notifications.group.today", "Oggi"),
    Yesterday("notifications.group.yesterday", "Ieri"),
    Week("notifications.group.week", "Settimana scorsa"),
    Older("notifications.group.older", "Piu vecchie"),
}

data class NotificationSection(
    val group: NotificationGroup,
    val items: List<NotificationModel>,
)

// ─── Filter ───────────────────────────────────────────────────────────────────

enum class NotificationFilter(val labelKey: String, val fallback: String) {
    All("notifications.filter.all", "Tutte"),
    Unread("notifications.filter.unread", "Non lette"),
}

// ─── UI State ─────────────────────────────────────────────────────────────────

data class NotificationsListUiState(
    val notifications: List<NotificationModel> = emptyList(),
    val filter: NotificationFilter = NotificationFilter.All,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
) {
    val filtered: List<NotificationModel>
        get() = when (filter) {
            NotificationFilter.All -> notifications
            NotificationFilter.Unread -> notifications.filter { it.seen == null }
        }

    val unreadCount: Int get() = notifications.count { it.seen == null }

    val sections: List<NotificationSection>
        get() {
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()
            val today = now.toLocalDateTime(tz).date
            val items = filtered.sortedByDescending { it.created.toEpochMilliseconds() }
            val buckets = mutableMapOf<NotificationGroup, MutableList<NotificationModel>>()
            for (n in items) {
                val date = n.created.toLocalDateTime(tz).date
                val group = when {
                    date == today -> NotificationGroup.Today
                    date.toEpochDays() == today.toEpochDays() - 1 -> NotificationGroup.Yesterday
                    today.toEpochDays() - date.toEpochDays() < 7 -> NotificationGroup.Week
                    else -> NotificationGroup.Older
                }
                buckets.getOrPut(group) { mutableListOf() }.add(n)
            }
            return NotificationGroup.values().mapNotNull { g ->
                buckets[g]?.let { NotificationSection(g, it) }
            }
        }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

/**
 * Parses NotificationModel.data JsonObject into a flat [Map<String, String>].
 *
 * The shared [NotificationModel.data] field is a `kotlinx.serialization.json.JsonObject?`
 * which is not accessible from the Android module (transitive `implementation` dep).
 * We call `.toString()` on it to get its JSON representation and parse it with
 * [org.json.JSONObject] which is always available on Android.
 *
 * Mirrors iOS notificationDataDict(_:).
 */
fun notificationDataMap(n: NotificationModel): Map<String, String>? {
    val raw = n.data?.toString() ?: return null
    return try {
        val json = org.json.JSONObject(raw)
        val result = mutableMapOf<String, String>()
        for (key in json.keys()) {
            var s = json.optString(key, "")
            // Trim surrounding quotes if present (JSON string encoding)
            if (s.startsWith("\"") && s.endsWith("\"") && s.length >= 2) {
                s = s.drop(1).dropLast(1)
            }
            result[key] = s
        }
        result
    } catch (_: Exception) {
        null
    }
}

/**
 * Parses a [NotificationModel] into a [NotificationTarget].
 * Mirrors iOS notificationTarget(from:).
 */
fun notificationTarget(n: NotificationModel): NotificationTarget? {
    val dict = notificationDataMap(n) ?: return null
    fun str(key: String): String? = dict[key]?.takeIf { it.isNotEmpty() && it != "null" }
    return when (dict["type"] ?: "") {
        "event" -> str("event_id")?.let { NotificationTarget.Event(it) }
        "deal" -> str("deal_id")?.let { NotificationTarget.Deal(it) }
        "single_document" -> str("document_id")?.let { NotificationTarget.SingleDocument(it) }
        "multiple_documents" -> NotificationTarget.MultipleDocuments
        "ticket_purchase" -> NotificationTarget.TicketPurchase
        "payment_update_status" -> NotificationTarget.PaymentUpdateStatus
        "quid" -> str("category_id")?.let { NotificationTarget.Quid(it) }
        "quid_article" -> str("post_id")?.let { NotificationTarget.QuidArticle(it) }
        "quid_pdf" -> str("record_id")?.let { NotificationTarget.QuidPdf(it) }
        "local_office" -> str("slug")?.let { NotificationTarget.LocalOffice(it) }
        "account_confirmation" -> {
            val appId = str("keyAppId")
            val url = str("url")
            if (appId != null && url != null) {
                NotificationTarget.AccountConfirmation(
                    exAppId = appId,
                    callbackUrl = url,
                    notificationId = str("internal_id"),
                )
            } else null
        }
        else -> null
    }
}

/**
 * Returns a Material icon name for the notification type.
 * Mirrors iOS notificationSystemIcon(for:).
 */
fun notificationIconName(n: NotificationModel): String {
    val dict = notificationDataMap(n) ?: return "notifications"
    return when (dict["type"]) {
        "event" -> "event"
        "single_document", "multiple_documents" -> "description"
        "account_confirmation" -> "verified_user"
        "deal" -> "local_offer"
        "ticket_purchase" -> "confirmation_number"
        "payment_update_status" -> "credit_card"
        "quid" -> "newspaper"
        "quid_article" -> "article"
        "local_office" -> "location_city"
        else -> "notifications"
    }
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class NotificationsListViewModel : ViewModel() {

    private val repo = koinAccess().notifications

    private val _uiState = MutableStateFlow(NotificationsListUiState())
    val uiState: StateFlow<NotificationsListUiState> = _uiState.asStateFlow()

    init {
        // Observe DB flow — updates on every SSE write-through or refresh
        repo.observeAll()
            .onEach { list ->
                _uiState.update { it.copy(notifications = list, loading = false) }
            }
            .catch { e ->
                _uiState.update { it.copy(loading = false, error = e.message) }
            }
            .launchIn(viewModelScope)

        // SSE realtime — automatically cancelled when ViewModel is cleared
        repo.observeRealtime(viewModelScope)

        // Initial network fetch
        refresh()
    }

    fun setFilter(filter: NotificationFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(refreshing = true, error = null) }
            runCatching { repo.refresh() }
                .onFailure { e -> _uiState.update { it.copy(error = e.message) } }
            _uiState.update { it.copy(refreshing = false, loading = false) }
        }
    }

    fun markSeen(n: NotificationModel) {
        viewModelScope.launch {
            runCatching { repo.markSeen(n.id) }
        }
    }

    fun markAllSeen() {
        viewModelScope.launch {
            runCatching { repo.markAllSeen() }
        }
    }

    fun delete(n: NotificationModel) {
        viewModelScope.launch {
            runCatching { repo.removeOne(n.id) }
        }
    }
}
