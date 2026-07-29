package it.mensa.app.support

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo

/**
 * LaunchHarness: intent-driven launch overrides used by automation
 * (`tools/storekit`) to drive the app straight to a given screen with a
 * pre-authenticated session, so store screenshots can be captured without a
 * human tapping through login.
 *
 * It is the Android counterpart of the iOS `MENSA_LAUNCH_SCREEN` /
 * `MENSA_AUTOLOGIN_EMAIL` process-environment hooks (see
 * `iosApp/iosApp/App/iosAppApp.swift`). Android apps can't read a launcher's
 * environment, so the same knobs arrive as intent extras:
 *
 * ```
 * adb shell am start -n it.mensa.app/.MainActivity \
 *   --es mensa_screen today \
 *   --es mensa_autologin_email socio@example.org \
 *   --es mensa_autologin_pwd '••••'
 * ```
 *
 * **Only honoured on a debuggable build.** On a release APK/AAB
 * [configure] returns immediately and every field stays null, so the extras
 * are inert even if someone crafts the intent by hand.
 */
object LaunchHarness {

    const val EXTRA_SCREEN = "mensa_screen"
    const val EXTRA_EMAIL = "mensa_autologin_email"
    const val EXTRA_PASSWORD = "mensa_autologin_pwd"

    /** Requested destination alias (e.g. `today`, `events`, `card`), or null. */
    var screen: String? = null
        private set

    private var email: String? = null
    private var password: String? = null

    /** True when this process was launched by the screenshot/automation harness. */
    val isActive: Boolean
        get() = screen != null || email != null

    /** Credentials to sign in with before rendering, or null when not requested. */
    val autologin: Pair<String, String>?
        get() {
            val e = email ?: return null
            val p = password ?: return null
            return e to p
        }

    /**
     * Reads the harness extras off the launch [intent]. No-op unless the
     * running build is debuggable.
     */
    fun configure(context: Context, intent: Intent?) {
        if (!context.isDebuggable()) return
        val extras = intent?.extras ?: return
        extras.getString(EXTRA_SCREEN)?.takeIf { it.isNotBlank() }?.let { screen = it }
        extras.getString(EXTRA_EMAIL)?.takeIf { it.isNotBlank() }?.let { email = it }
        extras.getString(EXTRA_PASSWORD)?.takeIf { it.isNotBlank() }?.let { password = it }
        if (isActive) {
            Logger.i("LaunchHarness", "configure", "active: screen=$screen autologin=${email != null}")
        }
    }

    private fun Context.isDebuggable(): Boolean =
        (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
}
