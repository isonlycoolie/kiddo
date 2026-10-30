package app.kiddo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_back
import app.kiddo.core.resources.action_close
import app.kiddo.core.resources.generated.KiddoGlyph
import org.jetbrains.compose.resources.stringResource

// One app screen: a header, a scrolling body under the status bar, and an optional footer pinned to the bottom with
// no backing layer of its own (it sits under the body, so nothing scrolls behind it).
@Composable
fun KiddoScreen(
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
    scrolls: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scroll = rememberScrollState()
    if (scrolls) ScrollToTopOnRequest { scroll.animateScrollTo(0) }
    Column(modifier.fillMaxSize().background(Kiddo.colors.bg).statusBarsPadding().imePadding()) {
        header?.invoke()
        val body = Modifier.weight(1f).fillMaxWidth()
        Column(
            modifier = if (scrolls) body.verticalScroll(scroll) else body,
            content = content,
        )
        if (footer != null) {
            val inset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
            Column(
                Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = max(inset, 16.dp)),
                content = footer,
            )
        }
    }
}

// IconButton: a 44 dp hit box around a 24 dp glyph.
@Composable
fun IconButton(glyph: KiddoGlyph, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp).pressable(onClick, pressedAlpha = 0.5f, label = label), contentAlignment = Alignment.Center) {
        KiddoIcon(glyph, label)
    }
}

// NavBar: 52 dp with Back (or Close) on the left, a centred title, actions on the right. No hairline.
@Composable
fun NavBar(
    modifier: Modifier = Modifier,
    title: String? = null,
    onBack: (() -> Unit)? = null,
    close: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Box(modifier.fillMaxWidth().height(52.dp).padding(horizontal = 6.dp)) {
        if (onBack != null) {
            val label = stringResource(if (close) Res.string.action_close else Res.string.action_back)
            IconButton(if (close) KiddoGlyph.Close else KiddoGlyph.Back, label, onBack, Modifier.align(Alignment.CenterStart))
        }
        if (title != null) {
            BasicText(
                title,
                style = Kiddo.type.nav.copy(color = Kiddo.colors.fg),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).widthIn(max = 230.dp),
            )
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = trailing)
    }
}

