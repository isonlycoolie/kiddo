package app.kiddo.core.designsystem

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TweenSpec
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.animation.core.tween as composeTween

val LocalKiddoColors = staticCompositionLocalOf { LightKiddoColors }
val LocalKiddoType = staticCompositionLocalOf<KiddoType> { error("KiddoTheme is missing") }
val LocalReducedMotion = staticCompositionLocalOf { false }

@Composable
fun KiddoTheme(dark: Boolean, reducedMotion: Boolean = false, content: @Composable () -> Unit) {
    val colors = if (dark) DarkKiddoColors else LightKiddoColors
    val family = interFamily()
    val type = remember(family) { kiddoType(family) }
    val selection = remember(colors) { TextSelectionColors(colors.fg, colors.fg.copy(alpha = 0.2f)) }
    CompositionLocalProvider(
        LocalKiddoColors provides colors,
        LocalKiddoType provides type,
        LocalReducedMotion provides reducedMotion,
        LocalTextSelectionColors provides selection,
        content = content,
    )
}

object Kiddo {
    val colors: KiddoColors
        @Composable @ReadOnlyComposable
        get() = LocalKiddoColors.current

    val type: KiddoType
        @Composable @ReadOnlyComposable
        get() = LocalKiddoType.current

    val reducedMotion: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalReducedMotion.current
}

// Every motion value in the app. Features never write a duration or a curve.
object KiddoMotion {
    val Standard: Easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
    val Emphasized: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
    val Sheet: Easing = CubicBezierEasing(0.2f, 0.9f, 0.1f, 1f)
    val Accelerate: Easing = CubicBezierEasing(0.4f, 0f, 1f, 1f)
    val Decelerate: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    val Pop: Easing = CubicBezierEasing(0.3f, 1.5f, 0.5f, 1f)
    val Ease: Easing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
    val InOut: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    const val INSTANT = 150
    const val QUICK = 240
    const val PUSH = 300
    const val RISE = 280
    const val DRAWER = 360
    const val SHEET = 420
    const val TOAST = 2600
    const val REDUCED = 150
    const val THINK = 1100
    const val SPIN = 900

    // The mark: pop in, smile draws, then blinks and glances. The glow drifts slowly.
    const val MARK_POP = 620
    const val SMILE_DELAY = 380
    const val SMILE = 520
    const val BLINK_EVERY = 4600
    const val LOOK_EVERY = 7000
    const val GLOW_DRIFT = 10_000
    const val SPLASH_HOLD = 2200

    const val PUSH_DISTANCE = 0.24f
    const val BACK_DISTANCE = 0.2f

    fun <T> tween(durationMillis: Int, easing: Easing, delayMillis: Int = 0): TweenSpec<T> =
        composeTween(durationMillis, delayMillis, easing)

    fun <T> reduced(): TweenSpec<T> = tween(REDUCED, Ease)
}
