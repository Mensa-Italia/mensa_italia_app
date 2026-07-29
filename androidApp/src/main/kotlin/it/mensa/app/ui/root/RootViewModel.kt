package it.mensa.app.ui.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import it.mensa.app.support.LaunchHarness
import it.mensa.app.support.Logger
import it.mensa.app.support.koinAccess
import it.mensa.shared.auth.AuthState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * RootPhase — sealed hierarchy describing the current top-level phase of the app.
 *
 * State machine:
 *   Unknown   ─init()──► Loading
 *   Loading   ─auth.Authenticated + user + onboarding──► Onboarding | Main
 *   Loading   ─auth.Anonymous──► Anonymous
 *   Anonymous ─user taps Login──► (stays Anonymous, RootScreen shows Login)
 *   Anonymous ─user taps Explore──► Public
 *   Onboarding─user completes──► Main
 *   Public    ─user taps Login──► (stays Anonymous, RootScreen shows Login)
 */
sealed class RootPhase {
    data object Loading : RootPhase()
    data object Anonymous : RootPhase()
    data object Onboarding : RootPhase()
    data object Main : RootPhase()
    data object Public : RootPhase()
}

class RootViewModel : ViewModel() {

    private val auth = koinAccess().auth
    private val i18n = koinAccess().i18n
    private val onboarding = koinAccess().onboarding
    private val events = koinAccess().events

    private val _phase = MutableStateFlow<RootPhase>(RootPhase.Loading)
    val phase: StateFlow<RootPhase> = _phase.asStateFlow()

    /** True when we restored a session at boot (affects onboarding gate in DEBUG). */
    private var sessionRestored: Boolean = false

    init {
        viewModelScope.launch {
            bootstrap()
        }
    }

    private suspend fun bootstrap() {
        // 1. Bootstrap i18n (loads catalog; fallbacks work even if this fails)
        runCatching {
            val locale = java.util.Locale.getDefault().language.ifBlank { "it" }
            i18n.bootstrap(locale)
            Logger.i("RootVM", "i18n", "bootstrapped for locale: $locale")
        }

        // 2. Restore persisted auth token
        runCatching { auth.init() }

        // 2b. Automation sign-in (screenshot capture). Only ever populated on a
        //     debuggable build — see [LaunchHarness]. Runs before the phase
        //     subscription so the very first emission is already Authenticated.
        if (auth.authState.value !is AuthState.Authenticated) {
            LaunchHarness.autologin?.let { (email, password) ->
                Logger.i("RootVM", "harness", "auto-login for $email")
                val result = runCatching { auth.login(email, password) }
                    .getOrElse { Result.failure(it) }
                if (result.isFailure) {
                    Logger.w("RootVM", "harness", "auto-login failed", result.exceptionOrNull())
                }
            }
        }

        // Record whether this was an existing session (affects onboarding gate)
        sessionRestored = auth.authState.value is AuthState.Authenticated

        // 3. Pre-warm Today DB cache — non-blocking, fire and forget
        if (sessionRestored) {
            coroutineScope {
                launch {
                    runCatching { events.refresh() }
                }
            }
        }

        // 4. Subscribe to authState + currentUser and derive phase
        combine(auth.authState, auth.currentUser) { state, user -> Pair(state, user) }
            .collect { (state, user) ->
                _phase.value = derivePhase(state, user)
            }
    }

    private suspend fun derivePhase(
        state: AuthState,
        user: it.mensa.shared.model.UserModel?,
    ): RootPhase = when (state) {
        is AuthState.Authenticated -> {
            if (user == null) {
                // User record not yet loaded — stay on Loading
                RootPhase.Loading
            } else if (LaunchHarness.isActive) {
                // Screenshot harness always wants the authenticated shell —
                // a fresh automation login would otherwise land on onboarding.
                RootPhase.Main
            } else {
                val showOnboarding = onboarding.shouldShow(user)
                if (showOnboarding) RootPhase.Onboarding else RootPhase.Main
            }
        }
        is AuthState.Anonymous -> RootPhase.Anonymous
        is AuthState.Unknown -> RootPhase.Loading
    }

    /** Called when the onboarding flow completes. */
    fun onOnboardingComplete() {
        val user = auth.currentUser.value
        if (user != null) {
            runCatching { onboarding.markShown(user.id) }
        }
        _phase.value = RootPhase.Main
    }

    /** Navigate to public area from anonymous landing. */
    fun enterPublic() {
        _phase.value = RootPhase.Public
    }

    /** Return to anonymous landing from public area. */
    fun exitPublic() {
        _phase.value = RootPhase.Anonymous
    }
}
