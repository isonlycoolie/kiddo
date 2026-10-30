package app.kiddo.core.designsystem.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.KiddoMotion
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_close
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

// Any screen can raise a sheet or a toast; the host draws them over everything. Sheets pick or confirm and close.
@Stable
class Overlay {
    internal var content by mutableStateOf<(@Composable ColumnScope.() -> Unit)?>(null)
    internal var title by mutableStateOf<String?>(null)
    internal var open by mutableStateOf(false)
    internal var toast by mutableStateOf<String?>(null)
    internal var toastKey by mutableIntStateOf(0)

    fun sheet(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
        this.title = title
        this.content = content
        open = true
    }

    fun close() {
        open = false
    }

    fun toast(text: String) {
        close()
        toast = text
        toastKey++
    }
}

val LocalOverlay = staticCompositionLocalOf<Overlay> { error("OverlayHost is missing") }

@Composable
fun OverlayHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val overlay = remember { Overlay() }
    CompositionLocalProvider(LocalOverlay provides overlay) {
        Box(modifier.fillMaxSize()) {
            content()
            BackPress(enabled = overlay.open) { overlay.close() }
            overlay.toast?.let { text ->
                Toast(
                    text,
                    overlay.toastKey,
                    onDone = { overlay.toast = null },
                    modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 96.dp),
                )
            }
            Sheet(overlay.open, overlay::close, title = overlay.title) { overlay.content?.invoke(this) }
        }
    }
}

// Sheet: rises over a dim, leaves faster than it came, and can be dragged down to close. Our own, so it matches the
// design exactly.
@Composable
fun Sheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = Kiddo.colors
    val reduced = Kiddo.reducedMotion
    val drag = rememberSheetDrag(onDismiss)
    LaunchedEffect(visible) { if (visible) drag.reset() }
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible,
            enter = fadeIn(if (reduced) KiddoMotion.reduced() else KiddoMotion.tween(KiddoMotion.QUICK, KiddoMotion.Ease)),
            exit = fadeOut(KiddoMotion.tween(KiddoMotion.QUICK, KiddoMotion.Ease)),
        ) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = DIM))
                    .pressable(onDismiss, label = stringResource(Res.string.action_close)),
            )
        }
        AnimatedVisibility(
            visible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (reduced) {
                fadeIn(KiddoMotion.reduced())
            } else {
                slideInVertically(KiddoMotion.tween(KiddoMotion.SHEET, KiddoMotion.Sheet)) { it }
            },
            exit = if (reduced) {
                fadeOut(KiddoMotion.reduced())
            } else {
                slideOutVertically(KiddoMotion.tween(KiddoMotion.QUICK, KiddoMotion.Accelerate)) { it }
            },
        ) {
            Column(
                Modifier.fillMaxWidth()
                    .offset { IntOffset(0, drag.offset.roundToInt()) }
                    .onSizeChanged { drag.height = it.height.toFloat() }
                    .nestedScroll(drag)
                    .background(colors.sheet, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .navigationBarsPadding()
                    .imePadding()
                    .heightIn(max = 620.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 10.dp, bottom = 24.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).background(colors.faint, CircleShape))
                if (title != null) {
                    BasicText(
                        title,
                        style = Kiddo.type.nav.copy(color = colors.fg, textAlign = TextAlign.Center),
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                    )
                }
                Column(
                    Modifier.padding(horizontal = 16.dp).padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun SheetGroup(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Kiddo.colors.sheetCard), content = content)
}

// Toast: floats up, holds, fades. A new key replays it.
@Composable
fun Toast(text: String, key: Int, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(key) {
        t.snapTo(0f)
        t.animateTo(1f, KiddoMotion.tween(KiddoMotion.TOAST, LinearEasing))
        onDone()
    }
    Box(
        modifier
            .graphicsLayer {
                val p = t.value
                alpha = when {
                    p < 0.12f -> p / 0.12f
                    p > 0.85f -> (1 - p) / 0.15f
                    else -> 1f
                }
                translationY = if (p < 0.12f) 12.dp.toPx() * (1 - p / 0.12f) else 0f
            }
            .background(Kiddo.colors.fg, CircleShape)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        BasicText(text, style = Kiddo.type.button.copy(color = Kiddo.colors.bg))
    }
}

// System Back while something is open (a sheet, the menu) closes it first.
@Composable
expect fun BackPress(enabled: Boolean, onBack: () -> Unit)

private const val DIM = 0.4f
