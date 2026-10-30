package app.kiddo.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.KiddoColors
import app.kiddo.core.designsystem.component.Hairline

// Kiddo's answer as it is written: words reveal at an even pace whatever size the provider's chunks are, the newest
// ones fade in, and the glow dot rides at the end of the line until the answer is complete.
@Composable
internal fun AnswerText(text: String, streaming: Boolean, modifier: Modifier = Modifier) {
    val shown = rememberReveal(text, animate = streaming && !Kiddo.reducedMotion)
    val visible = if (shown >= text.length) text else text.take(shown)
    val typing = streaming || shown < text.length
    val blocks = remember(visible) { parseMarkdown(visible) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEachIndexed { i, block -> Block(block, tail = typing && i == blocks.lastIndex) }
    }
}

// How many characters are on screen. An answer opened from history shows whole; a live one catches up with what has
// arrived, faster the further behind it is, so big chunks never land all at once.
@Composable
private fun rememberReveal(text: String, animate: Boolean): Int {
    var shown by remember { mutableFloatStateOf(if (animate) 0f else text.length.toFloat()) }
    val goal by rememberUpdatedState(text.length)
    val smooth by rememberUpdatedState(animate)
    LaunchedEffect(Unit) {
        snapshotFlow { goal }.collect { target ->
            if (target < shown || !smooth) shown = target.toFloat()
            var last = 0L
            while (shown < goal) {
                withFrameNanos { now ->
                    val seconds = if (last == 0L) 0f else (now - last) / NANOS
                    last = now
                    val behind = goal - shown
                    shown = minOf(goal.toFloat(), shown + maxOf(MIN_SPEED, behind * CATCH_UP) * seconds)
                }
            }
        }
    }
    return shown.toInt()
}

