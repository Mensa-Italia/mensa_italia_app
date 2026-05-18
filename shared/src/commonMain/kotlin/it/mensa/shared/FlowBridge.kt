package it.mensa.shared

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow

class Closeable internal constructor(private val onClose: () -> Unit) {
    fun close() = onClose()
}

/**
 * Subscribe to a Kotlin Flow that emits NON-NULL values. The generic bound
 * `T : Any` is enforced at the call site: emissions of `null` are filtered out
 * defensively to protect Swift's `swift_getObjectType` thunk (Swift cannot
 * receive nil through a non-Optional Any callback parameter — it segfaults).
 *
 * Use [subscribeNullable] for `Flow<T?>` (e.g. `StateFlow<UserModel?>`).
 */
fun <T : Any> subscribe(
    flow: Flow<T>,
    onEach: (T) -> Unit,
    onError: (Throwable) -> Unit = {}
): Closeable {
    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val job = scope.launch {
        try {
            flow.collect { value ->
                @Suppress("SENSELESS_COMPARISON")
                if (value != null) onEach(value)
            }
        } catch (e: CancellationException) {
            // Consumer-initiated cancellation (Closeable.close on view dismiss).
            // Not an error — must NOT propagate to onError, otherwise Swift
            // sees a phantom failure every time the view disappears (cf. the
            // "wifi.exclamationmark" flash on returning to SearchView).
            throw e
        } catch (e: Throwable) { onError(e) }
    }
    return Closeable { job.cancel() }
}

/**
 * Subscribe to a Kotlin Flow that may emit `null` (e.g. `StateFlow<UserModel?>`).
 * Swift sees the callback as `(T?) -> Unit` and handles nil safely.
 */
fun <T : Any> subscribeNullable(
    flow: Flow<T?>,
    onEach: (T?) -> Unit,
    onError: (Throwable) -> Unit = {}
): Closeable {
    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    val job = scope.launch {
        try { flow.collect { onEach(it) } }
        catch (e: CancellationException) { throw e }   // see note in subscribe()
        catch (e: Throwable) { onError(e) }
    }
    return Closeable { job.cancel() }
}
