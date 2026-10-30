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

