package app.kiddo.core.designsystem.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.KiddoMotion

// Mark: a little round face on a 32 grid, the kiddo in your pocket. It pops in, draws its smile, then blinks, glances
// right and left, and grins now and then. `still` for tiny or repeated uses; reduced motion keeps it still too.
@Composable
fun Mark(modifier: Modifier = Modifier, size: Dp = 32.dp, still: Boolean = false, soft: Boolean = false) {
    val colors = Kiddo.colors
    val live = !still && !Kiddo.reducedMotion
    val pop = remember { Animatable(if (live) 0f else 1f) }
    val smile = remember { Animatable(if (live) 0f else 1f) }
    LaunchedEffect(live) {
        if (!live) return@LaunchedEffect
        pop.animateTo(1f, KiddoMotion.tween(KiddoMotion.MARK_POP, KiddoMotion.Pop))
    }
    LaunchedEffect(live) {
        if (!live) return@LaunchedEffect
        smile.animateTo(1f, KiddoMotion.tween(KiddoMotion.SMILE, KiddoMotion.InOut, KiddoMotion.SMILE_DELAY))
    }
    val loop = rememberInfiniteTransition(label = "mark")
    val blink by loop.animateFloat(1f, 1f, infiniteRepeatable(blinkFrames(), RepeatMode.Restart), label = "blink")
    val look by loop.animateFloat(0f, 0f, infiniteRepeatable(lookFrames(), RepeatMode.Restart), label = "look")
    val face = if (soft) colors.fg.copy(alpha = SOFT) else colors.fg
    val cut = colors.bg
    Canvas(modifier.size(size).blur(size * BLUR, BlurredEdgeTreatment.Unbounded)) {
        val k = this.size.width / GRID
        val p = pop.value
        withTransform({ scale(p, p, Offset(this.size.width / 2, this.size.height / 2)) }) {
            drawCircle(face, radius = 15f * k, center = Offset(16f * k, 16f * k), alpha = p.coerceIn(0f, 1f))
            val dx = if (live) look * k else 0f
            val eyeY = if (live) blink else 1f
            listOf(11.6f, 20.4f).forEach { x ->
                val h = 4.4f * k * eyeY
                drawOval(cut, Offset((x - 2f) * k + dx, 13.4f * k - h / 2), Size(4f * k, h))
            }
            drawSmile(smile.value, k, cut)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSmile(progress: Float, k: Float, color: Color) {
    if (progress <= 0f) return
    val full = Path().apply {
        moveTo(10.6f * k, 19.2f * k)
        cubicTo(12.1f * k, 21.5f * k, 13.9f * k, 22.6f * k, 16f * k, 22.6f * k)
        cubicTo(18.1f * k, 22.6f * k, 19.9f * k, 21.5f * k, 21.4f * k, 19.2f * k)
    }
    val measure = PathMeasure().apply { setPath(full, false) }
    val part = Path()
    measure.getSegment(0f, measure.length * progress, part, true)
    drawPath(part, color, style = Stroke(2.2f * k, cap = StrokeCap.Round))
}

// A blink every few seconds: the eyes close to a line and open.
private fun blinkFrames() = keyframes {
    durationMillis = KiddoMotion.BLINK_EVERY
    1f at 0
    1f at (KiddoMotion.BLINK_EVERY * 0.9f).toInt()
    0.1f at (KiddoMotion.BLINK_EVERY * 0.94f).toInt()
    1f at KiddoMotion.BLINK_EVERY
}

// A glance right, then left, then back.
private fun lookFrames() = keyframes {
    durationMillis = KiddoMotion.LOOK_EVERY
    0f at 0
    0f at (KiddoMotion.LOOK_EVERY * 0.18f).toInt()
    1.6f at (KiddoMotion.LOOK_EVERY * 0.24f).toInt()
    1.6f at (KiddoMotion.LOOK_EVERY * 0.42f).toInt()
    0f at (KiddoMotion.LOOK_EVERY * 0.5f).toInt()
    -1.6f at (KiddoMotion.LOOK_EVERY * 0.58f).toInt()
    -1.6f at (KiddoMotion.LOOK_EVERY * 0.72f).toInt()
    0f at (KiddoMotion.LOOK_EVERY * 0.8f).toInt()
}

@Composable
fun Wordmark(modifier: Modifier = Modifier, style: TextStyle = Kiddo.type.wordmark) {
    BasicText("kiddo", modifier, style = style.copy(color = Kiddo.colors.fg))
}

// Glow: three soft blobs of the Kiddo glow drifting slowly behind one of Kiddo's own moments. Each blob fades out
// gradually and the whole layer is blurred (Android 12 and up), so it reads as light, never as shapes.
@Composable
fun Glow(modifier: Modifier = Modifier, size: Dp = 260.dp) {
    val c = Kiddo.colors
    val drift by rememberInfiniteTransition(label = "glow").animateFloat(
        0f,
        1f,
        infiniteRepeatable(KiddoMotion.tween(KiddoMotion.GLOW_DRIFT, LinearEasing), RepeatMode.Reverse),
        label = "drift",
    )
    val t = if (Kiddo.reducedMotion) 0f else drift
    Canvas(modifier.size(size)) {
        val w = this.size.width
        val blobs = listOf(
            Triple(c.glowA, Offset(w * (0.32f + 0.06f * t), w * (0.42f - 0.05f * t)), w * 0.36f),
            Triple(c.glowB, Offset(w * (0.7f - 0.05f * t), w * (0.3f + 0.04f * t)), w * 0.32f),
            Triple(c.glowC, Offset(w * (0.52f + 0.04f * t), w * (0.72f - 0.04f * t)), w * 0.32f),
        )
        blobs.forEach { (color, center, radius) ->
            val a = c.glowAlpha
            drawCircle(
                Brush.radialGradient(
                    0f to color.copy(alpha = a),
                    0.45f to color.copy(alpha = a * 0.55f),
                    1f to Color.Transparent,
                    center = center,
                    radius = radius,
                ),
                radius,
                center,
            )
        }
    }
}

private const val GRID = 32f
private const val SOFT = 0.72f
private const val BLUR = 0.12f
