package it.mensa.shared.auth

import it.mensa.shared.auth.oidc.OidcSession
import kotlinx.datetime.Clock
import kotlin.concurrent.Volatile

/**
 * In-memory snapshot of the current OIDC session, kept in sync by
 * [AuthRepository] on init / login / logout / refresh. Read by the HTTP
 * client's auth plugin on every outgoing request without going through the
 * suspend [ITokenStore.read] (which deadlocks `runBlocking` on Darwin's
 * main dispatcher — observed concretely: defaultRequest never emitted an
 * Authorization header even though the token was on disk).
 *
 * Persistent storage (EncryptedSharedPreferences / NSUserDefaults /
 * localStorage) remains the source of truth across launches; this is just
 * the hot path.
 */
object AuthHolder {
    @Volatile
    var session: OidcSession? = null

    /** Synchronous access for the HTTP client's request pipeline. */
    val token: String? get() = session?.accessToken

    /** True if the access token is within [skewSeconds] of its expiry. */
    fun isExpiringSoon(skewSeconds: Long = 60): Boolean {
        val s = session ?: return false
        return s.expiresAtEpochSeconds - Clock.System.now().epochSeconds <= skewSeconds
    }
}
