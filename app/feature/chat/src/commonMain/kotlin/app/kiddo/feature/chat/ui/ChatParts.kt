package app.kiddo.feature.chat.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.KiddoMotion
import app.kiddo.core.designsystem.component.Avatar
import app.kiddo.core.designsystem.component.IconButton
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.Pill
import app.kiddo.core.designsystem.component.Tick
import app.kiddo.core.designsystem.component.TodoCircle
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_menu
import app.kiddo.core.resources.chat_failed
import app.kiddo.core.resources.chat_models
import app.kiddo.core.resources.chat_profile
import app.kiddo.core.resources.chat_retry
import app.kiddo.core.resources.composer_send
import app.kiddo.core.resources.composer_stop
import app.kiddo.core.resources.generated.KiddoGlyph
import org.jetbrains.compose.resources.stringResource

// Menu left, the model in the middle under a small chevron, the person right.
@Composable
fun ChatBar(title: String, onMenu: () -> Unit, onModel: (() -> Unit)?, onProfile: () -> Unit, modifier: Modifier = Modifier, dot: Boolean = false) {
    Box(modifier.fillMaxWidth().padding(horizontal = 6.dp).heightIn(min = 52.dp)) {
        IconButton(KiddoGlyph.Menu, stringResource(Res.string.action_menu), onMenu, Modifier.align(Alignment.CenterStart))
        Row(
            Modifier.align(Alignment.Center).widthIn(max = 230.dp).pressable(onModel, pressedAlpha = 0.6f, label = stringResource(Res.string.chat_models))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            BasicText(title, style = Kiddo.type.nav.copy(color = Kiddo.colors.fg), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            if (onModel != null) KiddoIcon(KiddoGlyph.Down, null, size = 16.dp, tint = Kiddo.colors.muted)
        }
        Box(
            Modifier.align(Alignment.CenterEnd).size(44.dp).pressable(onProfile, pressedAlpha = 0.6f, label = stringResource(Res.string.chat_profile)),
            contentAlignment = Alignment.Center,
        ) { Avatar(size = 30.dp, dot = dot) }
    }
}

// The rounded well at the foot of the chat. The round button sends, and becomes Stop while Kiddo is working.
@Composable
fun Composer(
    text: String,
    onText: (String) -> Unit,
    hint: String,
    running: Boolean,
    enabled: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = Kiddo.colors
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).background(colors.well, RoundedCornerShape(26.dp)).alpha(if (enabled) 1f else DISABLED)
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        Box(Modifier.weight(1f).padding(vertical = 9.dp)) {
            if (text.isEmpty()) BasicText(hint, style = Kiddo.type.body.copy(color = colors.muted), maxLines = 1)
            BasicTextField(
                text,
                onText,
                enabled = enabled,
                textStyle = Kiddo.type.body.copy(color = colors.fg),
                cursorBrush = SolidColor(colors.fg),
                maxLines = MAX_LINES,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        val canSend = enabled && text.isNotBlank()
        val label = stringResource(if (running) Res.string.composer_stop else Res.string.composer_send)
        Box(
            Modifier.padding(start = 6.dp).size(40.dp)
                .background(if (running || canSend) colors.fg else colors.raised, CircleShape)
                .pressable(if (running) onStop else if (canSend) onSend else null, pressedScale = 0.9f, label = label),
            contentAlignment = Alignment.Center,
        ) {
            if (running) {
                Box(Modifier.size(12.dp).background(colors.bg, RoundedCornerShape(3.dp)))
            } else {
                KiddoIcon(KiddoGlyph.Send, label, size = 20.dp, stroke = 2f, tint = if (canSend) colors.bg else colors.muted)
            }
        }
    }
}

@Composable
fun YouBubble(text: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(start = 56.dp), contentAlignment = Alignment.CenterEnd) {
        BasicText(
            text,
            style = Kiddo.type.body.copy(color = Kiddo.colors.fg),
            modifier = Modifier.background(Kiddo.colors.well, RoundedCornerShape(20.dp)).padding(horizontal = 15.dp, vertical = 9.dp),
        )
    }
}

// Kiddo's answer: text across the page, written in as it streams; the thinking dot alone until the first words arrive.
@Composable
fun Answer(text: String, streaming: Boolean, failed: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (text.isNotEmpty()) AnswerText(text, streaming) else if (streaming) ThinkingDot()
        if (failed && !streaming) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                KiddoIcon(KiddoGlyph.Alert, null, size = 17.dp, tint = Kiddo.colors.danger)
                BasicText(stringResource(Res.string.chat_failed), style = Kiddo.type.sub.copy(color = Kiddo.colors.danger))
                Pill(stringResource(Res.string.chat_retry), onRetry, outline = true)
            }
        }
    }
}

// The glow dot breathing while Kiddo thinks.
@Composable
fun ThinkingDot(modifier: Modifier = Modifier) {
    val c = Kiddo.colors
    val pulse by rememberInfiniteTransition(label = "think").animateFloat(
        1f,
        0.6f,
        infiniteRepeatable(KiddoMotion.tween(KiddoMotion.THINK / 2, KiddoMotion.Ease), RepeatMode.Reverse),
        label = "pulse",
    )
    val scale = if (Kiddo.reducedMotion) 1f else pulse
    Box(
        modifier.size(12.dp).graphicsLayer {
            scaleX = scale
            scaleY = scale
        }.background(Brush.linearGradient(listOf(c.glowA, c.glowB, c.glowC)), CircleShape),
    )
}

