package app.kiddo.core.designsystem.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.KiddoMotion
import app.kiddo.core.resources.generated.KiddoGlyph

// ListRow: a list line on a 52 dp pitch: optional glyph or lead, the label with an optional grey line, then a value or
// control. A row with no onClick is a plain container, so a Toggle inside it is never a button within a button.
@Composable
fun ListRow(
    label: String,
    modifier: Modifier = Modifier,
    glyph: KiddoGlyph? = null,
    lead: (@Composable () -> Unit)? = null,
    sub: AnnotatedString? = null,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    external: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = Kiddo.colors
    val ink = if (danger) colors.danger else colors.fg
    Row(
        modifier.fillMaxWidth().heightIn(min = 52.dp).pressable(onClick, pressedAlpha = 0.6f).padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        glyph?.let { KiddoIcon(it, null, size = 22.dp, tint = ink) }
        lead?.invoke()
        Column(Modifier.weight(1f).padding(vertical = 9.dp)) {
            BasicText(label, style = Kiddo.type.row.copy(color = ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
            sub?.let { BasicText(it, style = Kiddo.type.sub.copy(color = colors.muted), modifier = Modifier.padding(top = 1.dp)) }
        }
        value?.let { BasicText(it, style = Kiddo.type.body.copy(color = colors.muted), maxLines = 1) }
        when {
            trailing != null -> trailing()
            onClick != null && !danger -> KiddoIcon(
                if (external) KiddoGlyph.External else KiddoGlyph.Chevron,
                null,
                size = 18.dp,
                tint = colors.faint,
            )
        }
    }
}

fun sub(text: String) = AnnotatedString(text)

// Group: rows on one rounded grey card with hairlines between them.
@Composable
fun Group(modifier: Modifier = Modifier, title: String? = null, note: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 20.dp)) {
        title?.let { BasicText(it, style = Kiddo.type.label.copy(color = Kiddo.colors.muted), modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)) }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kiddo.colors.well), content = content)
        note?.let { BasicText(it, style = Kiddo.type.small.copy(color = Kiddo.colors.muted), modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp)) }
    }
}

@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().padding(horizontal = 14.dp).height(0.5.dp).background(Kiddo.colors.line))
}

// Note: one quiet line with a glyph. Lock and shield notes promise safety, so their glyph is green.
@Composable
fun Note(text: String, modifier: Modifier = Modifier, glyph: KiddoGlyph = KiddoGlyph.Lock) {
    val safe = glyph == KiddoGlyph.Lock || glyph == KiddoGlyph.Shield
    Row(modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        KiddoIcon(glyph, null, Modifier.padding(top = 1.dp), size = 17.dp, tint = if (safe) Kiddo.colors.ok else Kiddo.colors.muted)
        BasicText(text, style = Kiddo.type.sub.copy(color = Kiddo.colors.muted))
    }
}

// Info: a read-only detail, its name small above the value.
@Composable
fun Info(name: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        BasicText(name, style = Kiddo.type.small.copy(color = Kiddo.colors.muted))
        BasicText(value.ifEmpty { "-" }, style = Kiddo.type.body.copy(color = Kiddo.colors.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// Tick: the green done badge. TodoCircle: the dotted circle for something still to do.
@Composable
fun Tick(modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Box(modifier.size(size).background(Kiddo.colors.ok, CircleShape), contentAlignment = Alignment.Center) {
        KiddoIcon(KiddoGlyph.Check, null, size = size * 0.62f, stroke = 2.6f, tint = Color.White)
    }
}

@Composable
fun TodoCircle(modifier: Modifier = Modifier, size: Dp = 26.dp) {
    val faint = Kiddo.colors.faint
    Canvas(modifier.size(size)) {
        val w = 2.dp.toPx()
        drawCircle(faint, radius = (this.size.minDimension - w) / 2, style = Stroke(w, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f))))
    }
}

// Avatar: a person glyph on the well grey until the user adds a photo. A red dot when something needs them.
@Composable
fun Avatar(modifier: Modifier = Modifier, size: Dp = 32.dp, dot: Boolean = false) {
    Box(modifier.size(size)) {
        Box(Modifier.size(size).background(Kiddo.colors.well, CircleShape), contentAlignment = Alignment.Center) {
            KiddoIcon(KiddoGlyph.User, null, size = size * 0.56f)
        }
        if (dot) {
            Box(
                Modifier.align(Alignment.TopEnd).size(10.dp).background(Kiddo.colors.bg, CircleShape).padding(2.dp)
                    .background(Kiddo.colors.danger, CircleShape),
            )
        }
    }
}

// Empty: one glyph in a soft circle, a bold line, a grey line, and an optional action under them.
@Composable
fun Empty(glyph: KiddoGlyph, title: String, modifier: Modifier = Modifier, body: String? = null, action: (@Composable () -> Unit)? = null) {
    Column(modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).background(Kiddo.colors.well, CircleShape), contentAlignment = Alignment.Center) {
            KiddoIcon(glyph, null, size = 28.dp)
        }
        BasicText(title, style = Kiddo.type.heading.copy(color = Kiddo.colors.fg, textAlign = TextAlign.Center), modifier = Modifier.padding(top = 18.dp))
        body?.let { BasicText(it, style = Kiddo.type.body.copy(color = Kiddo.colors.muted, textAlign = TextAlign.Center), modifier = Modifier.padding(top = 6.dp)) }
        action?.let {
            Spacer(Modifier.height(22.dp))
            it()
        }
    }
}

// Spinner: a ring that turns while something runs.
@Composable
fun Spinner(modifier: Modifier = Modifier, size: Dp = 18.dp, color: Color = Kiddo.colors.fg) {
    val turn by rememberInfiniteTransition(label = "spin").animateFloat(
        0f,
        360f,
        infiniteRepeatable(KiddoMotion.tween(KiddoMotion.SPIN, LinearEasing), RepeatMode.Restart),
        label = "turn",
    )
    val track = Kiddo.colors.faint
    Canvas(modifier.size(size).graphicsLayer { rotationZ = turn }) {
        val w = 2.dp.toPx()
        drawCircle(track, radius = (this.size.minDimension - w) / 2, style = Stroke(w))
        drawArc(color, -90f, 90f, false, Offset(w / 2, w / 2), this.size.copy(this.size.width - w, this.size.height - w), style = Stroke(w, cap = StrokeCap.Round))
    }
}
