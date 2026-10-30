package app.kiddo.feature.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.KiddoMotion
import app.kiddo.core.designsystem.component.Glow
import app.kiddo.core.designsystem.component.Hairline
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.ScrollToTopOnRequest
import app.kiddo.core.designsystem.component.rememberKeyboardAwayOnDrag
import app.kiddo.core.platform.platformFeel
import androidx.compose.ui.input.nestedscroll.nestedScroll
import app.kiddo.core.designsystem.component.Mark
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.model.ChatRole
import app.kiddo.core.model.MessageKind
import app.kiddo.feature.chat.agent.BlockedCard
import app.kiddo.feature.chat.agent.CvBuiltCard
import app.kiddo.feature.chat.agent.DraftCard
import app.kiddo.feature.chat.agent.JobCard
import app.kiddo.feature.chat.agent.StepCard
import app.kiddo.feature.chat.agent.parseModelJson
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.chat_greeting
import app.kiddo.core.resources.chat_ready_note
import app.kiddo.core.resources.chat_ready_title
import app.kiddo.core.resources.chat_title
import app.kiddo.core.resources.composer_hint
import app.kiddo.core.resources.composer_needs_model
import app.kiddo.core.resources.composer_reply
import app.kiddo.core.resources.menu_close
import app.kiddo.core.resources.setup_ai
import app.kiddo.core.resources.setup_ai_body
import app.kiddo.core.resources.setup_cv
import app.kiddo.core.resources.setup_cv_body
import app.kiddo.core.resources.setup_mail
import app.kiddo.core.resources.setup_mail_body
import app.kiddo.core.resources.starter_cv
import app.kiddo.core.resources.starter_cv_sub
import app.kiddo.core.resources.starter_email
import app.kiddo.core.resources.starter_email_sub
import app.kiddo.core.resources.starter_email_text
import app.kiddo.core.resources.starter_post
import app.kiddo.core.resources.starter_post_sub
import app.kiddo.core.resources.starter_post_text
import app.kiddo.feature.chat.ChatState
import org.jetbrains.compose.resources.stringResource

class HomeActions(
    val onMenu: (Boolean) -> Unit,
    val onModel: () -> Unit,
    val onProfile: () -> Unit,
    val onText: (String) -> Unit,
    val onSend: () -> Unit,
    val onStop: () -> Unit,
    val onRetry: () -> Unit,
    val onSetupAi: () -> Unit,
    val onSetupMail: () -> Unit,
    val onSetupCv: () -> Unit,
    val onImproveCv: () -> Unit,
    val menu: MenuActions,
    val cards: CardActions,
)

