@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.model.NotificationModel
import it.mensa.shared.repository.NotificationsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.promise
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

@JsExport
class MensaWebNotifications internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: NotificationsRepository get() = KoinPlatform.getKoin().get()

    fun subscribeAll(callback: (notifications: Array<MensaWebNotification>) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.observeAll().collect { list ->
                callback(list.map { it.toJs() }.toTypedArray())
            }
        }
        return { job.cancel() }
    }

    /** Convenience: re-emits the count of notifications with `seenMs == 0`. */
    fun subscribeUnreadCount(callback: (count: Int) -> Unit): () -> Unit {
        val job: Job = scope.launch {
            sdk.awaitReady()
            repo.observeAll()
                .map { list -> list.count { it.seen == null } }
                .collect { callback(it) }
        }
        return { job.cancel() }
    }

    fun refresh(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.refresh()
    }

    fun markSeen(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.markSeen(id)
    }

    fun markAllSeen(): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.markAllSeen()
    }

    fun delete(id: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.removeOne(id)
    }
}

/**
 * `seenMs` is the epoch-millis timestamp of when the user marked the
 * notification as seen, or `0.0` if still unread (we collapse `null` to `0.0`
 * because `@JsExport` cannot represent `Double?` cleanly).
 *
 * `targetType` / `targetId` are pulled from the freeform `data` JSON. PB stores
 * the deep-link payload there with conventional keys like
 * `{"type": "event", "id": "..."}`. We pluck those two fields and ignore the
 * rest — JS callers should not parse PocketBase's raw JSON.
 */
@JsExport
data class MensaWebNotification(
    val id: String,
    val titleKey: String,
    val bodyKey: String,
    val params: Array<String>,
    val targetType: String,
    val targetId: String,
    val createdMs: Double,
    val seenMs: Double,
)

internal fun NotificationModel.toJs(): MensaWebNotification {
    val (tType, tId) = data.targetTuple()
    // Flatten the named-params map into a flat key/value array
    // ("k1", "v1", "k2", "v2", ...) so JS can reconstruct the dict without
    // KtMap. The order matches `trNamedParams.entries`.
    val params = mutableListOf<String>()
    trNamedParams.forEach { (k, v) ->
        params.add(k)
        params.add(v)
    }
    // Mirror iOS: PocketBase stores a base i18n key (`tr`, e.g.
        // `push_notification.update_deal`). The real Tolgee keys are
        // `<tr>.title` and `<tr>.body`. iOS does this in
        // NotificationsListView.swift (`notificationTitle` / `notificationBody`);
        // we do it here so all JS consumers get the correct keys to translate.
        val titleK = if (tr.isNotEmpty()) "$tr.title" else "notifications.fallback.title"
    val bodyK = if (tr.isNotEmpty()) "$tr.body" else ""
    return MensaWebNotification(
        id = id,
        titleKey = titleK,
        bodyKey = bodyK,
        params = params.toTypedArray(),
        targetType = tType,
        targetId = tId,
        createdMs = created.toEpochMilliseconds().toDouble(),
        seenMs = seen?.toEpochMilliseconds()?.toDouble() ?: 0.0,
    )
}

private fun JsonObject?.targetTuple(): Pair<String, String> {
    if (this == null) return "" to ""
    val type = this["type"]?.jsonPrimitiveOrNull()?.content ?: ""
    val id = this["id"]?.jsonPrimitiveOrNull()?.content ?: ""
    return type to id
}

private fun kotlinx.serialization.json.JsonElement.jsonPrimitiveOrNull(): JsonPrimitive? =
    runCatching { jsonPrimitive }.getOrNull()
