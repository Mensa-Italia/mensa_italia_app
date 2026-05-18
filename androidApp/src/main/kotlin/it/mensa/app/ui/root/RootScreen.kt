package it.mensa.app.ui.root

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.mensa.app.features.auth.LoginScreen
import it.mensa.app.features.onboarding.OnboardingScreen
import it.mensa.app.features.publicarea.PublicAreaScreen
import it.mensa.app.ui.components.LoadingDots
import it.mensa.app.ui.components.PrimaryButton
import it.mensa.app.ui.shell.MainAppShell
import it.mensa.app.ui.theme.EasingEmphasizedDecelerate
import it.mensa.app.ui.theme.LocalMensaGradients
import it.mensa.app.ui.theme.MensaBlue
import it.mensa.app.support.tr
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

/**
 * RootScreen — top-level composable that drives the app-level state machine.
 *
 * Architecture:
 * - Uses AnimatedContent with M3 Expressive transitions between RootPhases.
 * - Does NOT use a NavHost at this level.
 * - AnonymousLanding manages its own sub-state to show Login inline.
 * - MainAppShell owns its internal NavHost for the 5 tabs.
 */
@Composable
fun RootScreen(
    vm: RootViewModel = koinViewModel(),
) {
    val phase by vm.phase.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = phase,
        transitionSpec = {
            when {
                initialState is RootPhase.Loading -> {
                    (fadeIn(tween(400, easing = EasingEmphasizedDecelerate)) +
                            slideInVertically(tween(400, easing = EasingEmphasizedDecelerate)) { it / 16 })
                        .togetherWith(fadeOut(tween(200)))
                }
                targetState is RootPhase.Main -> {
                    (fadeIn(tween(350, easing = EasingEmphasizedDecelerate)) +
                            slideInVertically(tween(350, easing = EasingEmphasizedDecelerate)) { it / 12 })
                        .togetherWith(fadeOut(tween(200)))
                }
                targetState is RootPhase.Onboarding -> {
                    (fadeIn(tween(300)) +
                            slideInVertically(tween(300, easing = EasingEmphasizedDecelerate)) { it / 8 })
                        .togetherWith(fadeOut(tween(150)))
                }
                else -> fadeIn(tween(250)).togetherWith(fadeOut(tween(200)))
            }
        },
        label = "RootPhaseTransition",
    ) { targetPhase ->
        when (targetPhase) {
            is RootPhase.Loading -> SplashScreen()
            is RootPhase.Anonymous -> AnonymousLanding(
                onExplorePublic = { vm.enterPublic() },
            )
            is RootPhase.Onboarding -> OnboardingScreen(
                onComplete = { vm.onOnboardingComplete() },
            )
            is RootPhase.Main -> MainAppShell()
            is RootPhase.Public -> PublicAreaScreen(
                onLogin = { vm.exitPublic() },
            )
        }
    }
}

// ─── Splash Screen ─────────────────────────────────────────────────────────────

@Composable
private fun SplashScreen() {
    var showDots by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(250)
        showDots = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MensaBlue),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            MensaLogoMark(size = 120.dp, variant = LogoVariant.Solid)
            Spacer(Modifier.height(36.dp))
            AnimatedVisibility(
                visible = showDots,
                enter = fadeIn(tween(400)),
            ) {
                LoadingDots(color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

// ─── Anonymous Landing ────────────────────────────────────────────────────────

/**
 * Manages sub-state between landing buttons and the Login form.
 * Login is shown as a local AnimatedContent switch — no NavHost needed.
 */
@Composable
private fun AnonymousLanding(
    onExplorePublic: () -> Unit,
) {
    var showLogin by remember { mutableStateOf(false) }

    AnimatedContent(
        targetState = showLogin,
        transitionSpec = {
            if (targetState) {
                (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 10 })
                    .togetherWith(fadeOut(tween(200)))
            } else {
                fadeIn(tween(250)).togetherWith(fadeOut(tween(200)))
            }
        },
        label = "AnonymousLoginTransition",
    ) { inLogin ->
        if (inLogin) {
            LoginScreen(onBack = { showLogin = false })
        } else {
            AnonymousLandingContent(
                onLoginClick = { showLogin = true },
                onExploreClick = onExplorePublic,
            )
        }
    }
}

@Composable
private fun AnonymousLandingContent(
    onLoginClick: () -> Unit,
    onExploreClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = LocalMensaGradients.current.brandDiagonal),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            MensaLogoMark(size = 100.dp, variant = LogoVariant.Solid)
            Spacer(Modifier.height(8.dp))
            Text(
                text = tr("app.login.title", "Bentornato in Mensa"),
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Text(
                text = tr("app.login.subtitle", "Accedi all'area soci per continuare."),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = tr("views.signin.title2", "Accedi"),
                onClick = onLoginClick,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = onExploreClick,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 14.dp),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.9f),
                ),
            ) {
                Text(
                    text = tr("app.login.explore_no_account", "Esplora senza account"),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
