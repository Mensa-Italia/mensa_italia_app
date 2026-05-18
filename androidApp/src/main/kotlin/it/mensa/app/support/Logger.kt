package it.mensa.app.support

import android.util.Log

/**
 * Logger — structured log wrapper.
 * Format: `[Mensa] feature/EventName: message`
 */
object Logger {

    private const val GLOBAL_TAG = "Mensa"

    /** Verbose — internal flow / trace */
    fun v(feature: String, event: String, msg: String = "") =
        Log.v(GLOBAL_TAG, "[$feature/$event] $msg")

    /** Debug — dev-only diagnostic */
    fun d(feature: String, event: String, msg: String = "") =
        Log.d(GLOBAL_TAG, "[$feature/$event] $msg")

    /** Info — notable state transitions */
    fun i(feature: String, event: String, msg: String = "") =
        Log.i(GLOBAL_TAG, "[$feature/$event] $msg")

    /** Warning — recoverable issue */
    fun w(feature: String, event: String, msg: String = "", cause: Throwable? = null) {
        if (cause != null) {
            Log.w(GLOBAL_TAG, "[$feature/$event] $msg", cause)
        } else {
            Log.w(GLOBAL_TAG, "[$feature/$event] $msg")
        }
    }

    /** Error — unexpected failure */
    fun e(feature: String, event: String, msg: String = "", cause: Throwable? = null) {
        if (cause != null) {
            Log.e(GLOBAL_TAG, "[$feature/$event] $msg", cause)
        } else {
            Log.e(GLOBAL_TAG, "[$feature/$event] $msg")
        }
    }
}
