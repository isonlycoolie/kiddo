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

