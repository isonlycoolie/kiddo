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

