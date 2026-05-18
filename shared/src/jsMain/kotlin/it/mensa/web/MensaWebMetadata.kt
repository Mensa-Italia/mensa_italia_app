@file:OptIn(ExperimentalJsExport::class)

package it.mensa.web

import it.mensa.shared.repository.MetadataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.promise
import org.koin.mp.KoinPlatform
import kotlin.js.Promise

/**
 * JS facade for per-user `users_metadata`. Mirrors `koin.metadata` on iOS /
 * Android: small key/value store used for things like notification
 * preferences (`notify_events`, `notify_messages`, `notify_general`,
 * `notify_me_events`).
 *
 * Notes:
 *  - All operations require [userId] explicitly so JS callers don't need
 *    to reach into the Auth bridge for the current user id.
 *  - `refresh` returns the full map (POJO via `dynamic`) so the React side
 *    can hydrate state in one shot.
 */
@JsExport
class MensaWebMetadata internal constructor(
    private val scope: CoroutineScope,
    private val sdk: MensaWebSdk,
) {
    private val repo: MetadataRepository get() = KoinPlatform.getKoin().get()

    /** Hydrate the in-memory cache from the backend for the given user.
     *  Returns the full map as a plain JS object. */
    fun refresh(userId: String): Promise<dynamic> = scope.promise {
        sdk.awaitReady()
        val map = repo.refresh(userId)
        val out: dynamic = js("({})")
        for ((k, v) in map) {
            out[k] = v
        }
        out
    }

    /** Reads a value from the in-memory cache populated by [refresh].
     *  Returns null if the key isn't present or if [refresh] wasn't called. */
    fun get(key: String): String? = repo.get(key)

    /** Upserts a metadata entry for the user and updates the local cache. */
    fun set(userId: String, key: String, value: String): Promise<Unit> = scope.promise {
        sdk.awaitReady()
        repo.set(userId, key, value)
    }
}
