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

@Composable
private fun Block(block: MdBlock, tail: Boolean) {
    val t = Kiddo.type
    val c = Kiddo.colors
    val body = t.chat.copy(color = c.fg)
    when (block) {
        is MdBlock.Paragraph -> Inline(block.text, body, tail)
        is MdBlock.Heading -> Inline(block.text, (if (block.level <= 2) t.heading else t.bodyStrong).copy(color = c.fg), tail, Modifier.padding(top = 6.dp))
        is MdBlock.Item -> Row(Modifier.padding(start = INDENT * block.depth)) {
            BasicText(block.marker, style = body, modifier = Modifier.widthIn(min = MARKER))
            Inline(block.text, body, tail, Modifier.weight(1f))
        }
        is MdBlock.Quote -> Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(3.dp).fillMaxHeight().background(c.line, RoundedCornerShape(2.dp)))
            Inline(block.text, body.copy(color = c.muted), tail, Modifier.padding(start = 12.dp).weight(1f))
        }
        is MdBlock.Code -> Box(Modifier.fillMaxWidth().background(c.well, RoundedCornerShape(12.dp)).horizontalScroll(rememberScrollState()).padding(14.dp)) {
            Inline(block.text, body.copy(fontFamily = FontFamily.Monospace, fontSize = 13.5.sp, lineHeight = 20.sp), tail, plain = true)
        }
        MdBlock.Rule -> Hairline()
    }
}

@Composable
private fun Inline(text: String, style: TextStyle, tail: Boolean, modifier: Modifier = Modifier, plain: Boolean = false) {
    val c = Kiddo.colors
    val fade = tail && !Kiddo.reducedMotion
    val built = remember(text, style.color, c, tail, fade, plain) { annotate(text, style, c, tail, fade, plain) }
    BasicText(built, modifier, style = style, inlineContent = if (tail) Caret else emptyMap())
}

private fun annotate(text: String, style: TextStyle, c: KiddoColors, tail: Boolean, fade: Boolean, plain: Boolean): AnnotatedString = buildAnnotatedString {
    val runs = if (plain) listOf(Run(text, emptySet())) else parseInline(text)
    runs.forEach { run ->
        val start = length
        append(run.text)
        if (InlineMark.Bold in run.marks) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, length)
        if (InlineMark.Italic in run.marks) addStyle(SpanStyle(fontStyle = FontStyle.Italic), start, length)
        if (InlineMark.Code in run.marks) addStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 0.9.em, background = c.well), start, length)
    }
    if (fade) {
        val end = length
        val from = maxOf(0, end - FADE)
        val step = maxOf(1, (end - from + FADE_STEPS - 1) / FADE_STEPS)
        var at = from
        var k = 0
        while (at < end) {
            val alpha = 1f - (k + 1f) / (FADE_STEPS + 1f)
            addStyle(SpanStyle(color = style.color.copy(alpha = style.color.alpha * alpha)), at, minOf(end, at + step))
            at += step
            k++
        }
    }
    if (tail) {
        append(' ')
        appendInlineContent(CARET, " ")
    }
}

private const val CARET = "caret"

private val Caret = mapOf(
    CARET to InlineTextContent(Placeholder(12.sp, 12.sp, PlaceholderVerticalAlign.TextCenter)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { ThinkingDot() }
    },
)

private const val NANOS = 1_000_000_000f
private const val MIN_SPEED = 70f
private const val CATCH_UP = 5f
private const val FADE = 28
private const val FADE_STEPS = 4
private val INDENT = 18.dp
private val MARKER = 22.dp
