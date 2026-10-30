package app.kiddo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.resources.generated.KiddoGlyph

// Primary: the one full-width 52 dp action at the foot of a page or sheet. `busy` shows a ring and ignores taps.
// `kiddo` marks Kiddo's own work with the glow sparkle in front.
@Composable
fun Primary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    busy: Boolean = false,
    glyph: KiddoGlyph? = null,
    kiddo: Boolean = false,
    danger: Boolean = false,
) {
    val colors = Kiddo.colors
    val fill = if (danger) colors.danger else colors.accent
    val ink = if (danger) Color.White else colors.accentFg
    Row(
        modifier.fillMaxWidth().height(52.dp).alpha(if (enabled) 1f else DISABLED)
            .background(fill, CircleShape)
            .pressable(if (enabled && !busy) onClick else null, pressedScale = 0.98f),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            busy -> Spinner(color = ink)
            else -> {
                if (kiddo) GlowSpark(24.dp)
                glyph?.let { KiddoIcon(it, null, size = 20.dp, tint = ink) }
                BasicText(text, style = Kiddo.type.primary.copy(color = ink))
            }
        }
    }
}

@Composable
fun Secondary(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glyph: KiddoGlyph? = null,
    lead: (@Composable () -> Unit)? = null,
    busy: Boolean = false,
) {
    Row(
        modifier.fillMaxWidth().height(52.dp)
            .border(1.dp, Kiddo.colors.line, CircleShape)
            .pressable(if (busy) null else onClick, pressedScale = 0.98f),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (busy) {
            Spinner()
        } else {
            lead?.invoke()
            glyph?.let { KiddoIcon(it, null, size = 20.dp) }
            BasicText(text, style = Kiddo.type.primary.copy(color = Kiddo.colors.fg))
        }
    }
}

// Pill: the 36 dp inline action, solid or outline.
@Composable
fun Pill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, outline: Boolean = false, glyph: KiddoGlyph? = null) {
    val colors = Kiddo.colors
    val base = if (outline) modifier.border(1.dp, colors.line, CircleShape) else modifier.background(colors.accent, CircleShape)
    val ink = if (outline) colors.fg else colors.accentFg
    Row(
        base.height(36.dp).pressable(onClick, pressedScale = 0.97f).padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph?.let { KiddoIcon(it, null, size = 17.dp, tint = ink) }
        BasicText(text, style = Kiddo.type.button.copy(color = ink))
    }
}

