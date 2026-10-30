package app.kiddo.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import app.kiddo.core.designsystem.KiddoMotion

// Tap feedback: a quick scale or fade while pressed, no ripple, as in the design.
@Composable
fun Modifier.pressable(
    onClick: (() -> Unit)?,
    pressedScale: Float = 1f,
    pressedAlpha: Float = 1f,
    role: Role = Role.Button,
    label: String? = null,
): Modifier {
    if (onClick == null) return this
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val spec = KiddoMotion.tween<Float>(KiddoMotion.INSTANT, KiddoMotion.Ease)
    val scale by animateFloatAsState(if (pressed) pressedScale else 1f, spec, label = "pressScale")
    val alpha by animateFloatAsState(if (pressed) pressedAlpha else 1f, spec, label = "pressAlpha")
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            this.alpha = alpha
        }.clickable(source, null, role = role, onClickLabel = label, onClick = onClick)
}
