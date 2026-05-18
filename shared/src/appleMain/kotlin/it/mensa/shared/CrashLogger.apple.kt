package it.mensa.shared

import kotlin.experimental.ExperimentalNativeApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import platform.posix.fflush
import platform.posix.fprintf
import platform.posix.stderr

/**
 * Installs a Kotlin/Native unhandled-exception hook that logs the failure to
 * stderr before the runtime terminates the process. Visible in Xcode's console
 * and `idevicesyslog` (filter on `MENSA_CRASH`).
 *
 * Why we do NOT use NSLog or println here:
 *
 *   * `NSLog(msg)` interprets `msg` as a printf format string. Stack traces
 *     routinely contain `%` (URL escapes, hex digits in addresses), and any
 *     stray `%` triggers `__CFSTRING_IS_CALLING_OUT_TO_AN_OBJECT_FORMAT_…`
 *     → `objc_opt_respondsToSelector` on garbage → SIGTRAP. We literally crashed
 *     on offline launch because of this: an unhandled Ktor `IOException` (the
 *     network is gone) reached this hook, the hook called `NSLog` on the
 *     stack trace, and the format-string crash terminated the app *before*
 *     the runtime could finish its own termination handshake.
 *
 *   * `NSLog("%@", msgString)` is the "safe" form, but K/N's variadic
 *     ObjC bridge has historically crashed converting a Kotlin String into a
 *     CVarArg-compatible NSObject. Avoidable footgun.
 *
 *   * `println(msg)` on K/N iOS funnels through NSLog under the hood (recent
 *     toolchains), so it has the same `%` problem.
 *
 * Solution: write the bytes directly to stderr via `fprintf(stderr, "%s\n", …)`.
 * The format string is OUR literal `"%s\n"`, not the message — so no matter
 * what the message contains, the format dispatcher cannot misinterpret it.
 * `stderr` is captured by the device console subsystem just like NSLog output.
 */
@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
fun installCrashLogger() {
    setUnhandledExceptionHook { t ->
        val msg = buildString {
            append("MENSA_CRASH: ")
            append(t::class.simpleName ?: "Throwable")
            append(": ")
            append(t.message ?: "(no message)")
            append('\n')
            append(t.stackTraceToString())
            t.cause?.let { c ->
                append("\n  caused by: ")
                append(c::class.simpleName ?: "Throwable")
                append(": ")
                append(c.message ?: "(no message)")
                append('\n')
                append(c.stackTraceToString())
            }
        }
        // Write to stderr with a literal "%s" format so the message body —
        // which may contain `%` — is treated purely as data.
        memScoped {
            fprintf(stderr, "%s\n", msg.cstr.ptr)
            fflush(stderr)
        }
    }
}
