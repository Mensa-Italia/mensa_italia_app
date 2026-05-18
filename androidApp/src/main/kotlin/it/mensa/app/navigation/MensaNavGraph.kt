package it.mensa.app.navigation

import androidx.compose.runtime.Composable
import it.mensa.app.ui.root.RootScreen

/**
 * MensaNavGraph — root entry point for the Mensa app.
 *
 * RootScreen is the single top-level composable.
 * It manages all high-level navigation via AnimatedContent + sealed RootPhase state machine.
 * No top-level NavHost is needed — RootScreen handles:
 *   Loading → Splash
 *   Anonymous → AnonymousLanding (with inline Login transition)
 *   Onboarding → OnboardingScreen
 *   Main → MainAppShell (which owns the 5-tab NavHost internally)
 *   Public → PublicAreaScreen (with sub-route AnimatedContent)
 *
 * For deep links (event detail, deal detail, etc.) see PushDeepLinkRouter.kt.
 * Deep links into MainAppShell tab routing are wired later.
 *
 * ─── How to add routes ────────────────────────────────────────────────────────
 * - Tab features (Today/Discover/Search/Card/Profile): replace placeholder in
 *   features/<tab>/<Tab>Screen.kt directly — no change needed here.
 * - New global destinations: add NavHost here if needed.
 */
@Composable
fun MensaNavGraph() {
    RootScreen()
}
