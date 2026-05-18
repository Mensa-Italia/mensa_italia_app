package it.mensa.app.ui.theme

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.unit.Dp

// ─── MensaMotion — design-system motion object ────────────────────────────────

/**
 * Centralised motion spec for Mensa Android.
 *
 * Hierarchy:
 *   Hero     — FAB press, card expand, primary CTA: bouncy, overshooting spring
 *   Standard — Lists, secondary actions: smooth damped spring
 *   Gentle   — Background fades, skeleton: critically damped
 *   Shape    — Corner-radius morphing: dedicated Dp spring
 *   Page     — Screen enter/exit: tween with M3 Emphasized easing
 */
object MensaMotion {
    // ── Hero springs — bouncy, emphasised ─────────────────────────────────────
    /** FAB press, card expand, membership card reveal — overshooting bounce */
    val springHeroOvershoot: SpringSpec<Float> = spring(
        dampingRatio = 0.55f,
        stiffness = 380f,
    )

    /** Primary interactive elements — slight bounce, fast settle */
    val springHero: SpringSpec<Float> = spring(
        dampingRatio = 0.72f,
        stiffness = 380f,
    )

    // ── Standard springs — smooth, no bounce ──────────────────────────────────
    /** Lists, tertiary actions, navigation bar transitions */
    val springStandard: SpringSpec<Float> = spring(
        dampingRatio = 0.88f,
        stiffness = 800f,
    )

    /** Background fades, placeholder shimmer, gentle reveals */
    val springGentle: SpringSpec<Float> = spring(
        dampingRatio = 1f,
        stiffness = 600f,
    )

    // ── Shape morph spring — Dp typed for corner-radius animation ─────────────
    /** Corner-radius morph on press (28dp → 24dp, etc.) */
    val springShape: SpringSpec<Dp> = spring(
        dampingRatio = 0.75f,
        stiffness = 500f,
    )

    // ── Page transition tweens ─────────────────────────────────────────────────
    /** Screen enter: 350ms, M3 Emphasized decelerate easing */
    val tweenEnter: TweenSpec<Float> = tween(
        durationMillis = 350,
        easing = EasingEmphasizedDecelerate,
    )

    /** Screen exit: 250ms, M3 Emphasized accelerate easing */
    val tweenExit: TweenSpec<Float> = tween(
        durationMillis = 250,
        easing = EasingEmphasizedAccelerate,
    )

    // ── Pre-built ContentTransform ─────────────────────────────────────────────
    /**
     * Hero content swap: fade+slide-up enter, fade exit, spring-based size change.
     * Use for AnimatedContent in hero zones (card flip, score reveal, etc.).
     */
    fun heroTransform(): ContentTransform = ContentTransform(
        targetContentEnter = fadeIn(tweenEnter) + slideInVertically(
            animationSpec = tween(durationMillis = 350, easing = EasingEmphasizedDecelerate),
        ) { it / 8 },
        initialContentExit = fadeOut(tweenExit),
        sizeTransform = SizeTransform { _, _ ->
            spring(dampingRatio = 0.88f, stiffness = 800f)
        },
    )
}

// ─── Backwards-compat top-level functions ────────────────────────────────────
// These existed before MensaMotion was introduced — kept for existing callsites.

/**
 * M3 Expressive spring — low bouncy, medium-low stiffness.
 * Use for primary interactive elements.
 */
fun <T> motionExpressiveBouncy(): SpringSpec<T> = spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * M3 Expressive spring — medium bouncy.
 * Use for icons, FABs, expressive micro-interactions.
 */
fun <T> motionExpressiveMedium(): SpringSpec<T> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

/**
 * M3 Standard spring — no bounce, medium stiffness.
 * Use for navigation, content transitions, scaffold animations.
 */
fun <T> motionStandard(): SpringSpec<T> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMedium,
)

/**
 * M3 Standard spring — gentle. Use for background fades, skeleton shimmer.
 */
fun <T> motionStandardGentle(): SpringSpec<T> = spring(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow,
)

// ─── Duration constants ────────────────────────────────────────────────────────

/** Short duration for micro-interactions */
const val DURATION_SHORT = 200

/** Standard transition duration */
const val DURATION_MEDIUM = 350

/** Long transition (full-screen enter/exit) */
const val DURATION_LONG = 500

// ─── M3 Easing curves ─────────────────────────────────────────────────────────

/** M3 Emphasized easing — for incoming content */
val EasingEmphasized: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

/** M3 Emphasized decelerate — element arriving on screen */
val EasingEmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

/** M3 Emphasized accelerate — element leaving screen */
val EasingEmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

/** M3 Standard easing */
val EasingStandard: Easing = FastOutSlowInEasing

/** M3 Standard decelerate */
val EasingStandardDecelerate: Easing = LinearOutSlowInEasing

/** M3 Standard accelerate */
val EasingStandardAccelerate: Easing = FastOutLinearInEasing

// ─── Pre-built tween helpers ──────────────────────────────────────────────────

fun <T> tweenEmphasized(durationMs: Int = DURATION_MEDIUM): TweenSpec<T> =
    tween(durationMillis = durationMs, easing = EasingEmphasized)

fun <T> tweenStandard(durationMs: Int = DURATION_MEDIUM): TweenSpec<T> =
    tween(durationMillis = durationMs, easing = EasingStandard)

fun <T> tweenEnter(durationMs: Int = DURATION_MEDIUM): TweenSpec<T> =
    tween(durationMillis = durationMs, easing = EasingEmphasizedDecelerate)

fun <T> tweenExit(durationMs: Int = DURATION_SHORT): TweenSpec<T> =
    tween(durationMillis = durationMs, easing = EasingEmphasizedAccelerate)
