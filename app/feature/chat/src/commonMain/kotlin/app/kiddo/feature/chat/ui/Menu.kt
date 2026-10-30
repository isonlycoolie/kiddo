package app.kiddo.feature.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Avatar
import app.kiddo.core.designsystem.component.IconButton
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.menu_applications
import app.kiddo.core.resources.menu_cvs
import app.kiddo.core.resources.menu_updates
import app.kiddo.core.resources.menu_history
import app.kiddo.core.resources.menu_new_chat
import app.kiddo.core.resources.menu_recent
import app.kiddo.core.resources.menu_settings
import app.kiddo.feature.chat.ChatState
import org.jetbrains.compose.resources.stringResource

class MenuActions(
    val onNewChat: () -> Unit,
    val onApplications: () -> Unit,
    val onCvs: () -> Unit,
    val onUpdates: () -> Unit,
    val onHistory: () -> Unit,
    val onChat: (String) -> Unit,
    val onProfile: () -> Unit,
    val onSettings: () -> Unit,
)

@Composable
fun Menu(state: ChatState, actions: MenuActions, onClose: () -> Unit, modifier: Modifier = Modifier) {
    // Every choice closes the menu first, then goes.
    val go: (() -> Unit) -> () -> Unit = { action -> { onClose(); action() } }
    Column(modifier.fillMaxHeight().background(Kiddo.colors.bg).statusBarsPadding().navigationBarsPadding()) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
            MenuRow(KiddoGlyph.NewChat, stringResource(Res.string.menu_new_chat), go(actions.onNewChat))
            MenuRow(KiddoGlyph.Briefcase, stringResource(Res.string.menu_applications), go(actions.onApplications))
            MenuRow(KiddoGlyph.File, stringResource(Res.string.menu_cvs), go(actions.onCvs))
            MenuRow(KiddoGlyph.Bell, stringResource(Res.string.menu_updates), go(actions.onUpdates), count = state.unread)
            MenuRow(KiddoGlyph.Clock, stringResource(Res.string.menu_history), go(actions.onHistory))
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 8.dp)) {
            if (state.recent.isNotEmpty()) {
                BasicText(
                    stringResource(Res.string.menu_recent),
                    style = Kiddo.type.label.copy(color = Kiddo.colors.muted),
                    modifier = Modifier.padding(start = 12.dp, top = 14.dp, bottom = 4.dp),
                )
            }
            state.recent.forEach { chat ->
                BasicText(
                    chat.title,
                    style = Kiddo.type.body.copy(color = Kiddo.colors.fg),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .pressable(go { actions.onChat(chat.id) }, pressedAlpha = 0.6f)
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 10.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier.weight(1f).pressable(go(actions.onProfile), pressedAlpha = 0.6f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Avatar(size = 34.dp)
                BasicText(state.name, style = Kiddo.type.bodyStrong.copy(color = Kiddo.colors.fg), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(KiddoGlyph.Settings, stringResource(Res.string.menu_settings), go(actions.onSettings))
        }
    }
}

@Composable
private fun MenuRow(glyph: KiddoGlyph, label: String, onClick: () -> Unit, count: Int = 0) {
    Row(
        Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).pressable(onClick, pressedAlpha = 0.6f).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        KiddoIcon(glyph, null, size = 22.dp)
        BasicText(label, style = Kiddo.type.body.copy(color = Kiddo.colors.fg), modifier = Modifier.weight(1f))
        if (count > 0) {
            Box(
                Modifier.heightIn(min = 22.dp).widthIn(min = 22.dp).background(Kiddo.colors.danger, CircleShape).padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) { BasicText(count.toString(), style = Kiddo.type.small.copy(color = Color.White, fontWeight = FontWeight.SemiBold)) }
        }
    }
}
