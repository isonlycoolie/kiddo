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

@Composable
fun HomeScreen(state: ChatState, draft: String, menuOpen: Boolean, actions: HomeActions, modifier: Modifier = Modifier) {
    val ready = state.active != null
    val empty = state.messages.isEmpty()
    Box(modifier.fillMaxSize()) {
        KiddoScreen(
            scrolls = false,
            header = {
                ChatBar(
                    title = state.active?.model ?: stringResource(Res.string.chat_title),
                    onMenu = { actions.onMenu(true) },
                    onModel = actions.onModel.takeIf { state.accounts.size > 1 || ready },
                    onProfile = actions.onProfile,
                    dot = state.unread > 0,
                )
            },
            footer = {
                if (empty && ready && state.setup.done) Starters(actions.onText, actions.onImproveCv)
                val hint = when {
                    !ready -> Res.string.composer_needs_model
                    empty -> Res.string.composer_hint
                    else -> Res.string.composer_reply
                }
                Composer(draft, actions.onText, stringResource(hint), state.live.running, ready, actions.onSend, actions.onStop)
            },
        ) {
            when {
                !state.loaded -> Box(Modifier.weight(1f))
                empty && !state.setup.done -> SetupCard(state, actions, Modifier.weight(1f))
                empty -> Greeting(Modifier.weight(1f))
                else -> Conversation(state, actions.onRetry, actions.cards, Modifier.weight(1f))
            }
        }
        Drawer(menuOpen, state, actions.menu, onClose = { actions.onMenu(false) })
    }
}

@Composable
private fun Greeting(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Glow(Modifier.offset(y = (-40).dp), size = 230.dp)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Mark(size = 46.dp)
            BasicText(
                stringResource(Res.string.chat_greeting),
                style = Kiddo.type.title.copy(color = Kiddo.colors.fg, textAlign = TextAlign.Center, fontSize = Kiddo.type.heading.fontSize * 1.26f),
                modifier = Modifier.padding(top = 16.dp),
            )
        }
    }
}

@Composable
private fun SetupCard(state: ChatState, actions: HomeActions, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Glow(Modifier.align(Alignment.TopEnd).offset(x = 60.dp, y = 10.dp), size = 280.dp)
        SetupSteps(state, actions)
    }
}

@Composable
private fun SetupSteps(state: ChatState, actions: HomeActions) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.Center) {
        BasicText(
            stringResource(Res.string.chat_ready_title, state.name),
            style = Kiddo.type.title.copy(color = Kiddo.colors.fg),
            modifier = Modifier.padding(start = 4.dp),
        )
        Column(Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Kiddo.colors.well)) {
            SetupStep(stringResource(Res.string.setup_ai), stringResource(Res.string.setup_ai_body), state.setup.model, actions.onSetupAi)
            Hairline()
            SetupStep(stringResource(Res.string.setup_mail), stringResource(Res.string.setup_mail_body), state.setup.mail, actions.onSetupMail)
            Hairline()
            SetupStep(stringResource(Res.string.setup_cv), stringResource(Res.string.setup_cv_body), state.setup.cv, actions.onSetupCv)
        }
        BasicText(
            stringResource(Res.string.chat_ready_note),
            style = Kiddo.type.sub.copy(color = Kiddo.colors.muted),
            modifier = Modifier.padding(start = 4.dp, top = 14.dp),
        )
    }
}

@Composable
private fun Starters(onText: (String) -> Unit, onImproveCv: () -> Unit) {
    val post = stringResource(Res.string.starter_post_text)
    val email = stringResource(Res.string.starter_email_text)
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Starter(stringResource(Res.string.starter_post), stringResource(Res.string.starter_post_sub), { onText(post) })
        Starter(stringResource(Res.string.starter_cv), stringResource(Res.string.starter_cv_sub), onImproveCv)
        Starter(stringResource(Res.string.starter_email), stringResource(Res.string.starter_email_sub), { onText(email) })
    }
}

@Composable
private fun Conversation(state: ChatState, onRetry: () -> Unit, cards: CardActions, modifier: Modifier = Modifier) {
    val list = rememberLazyListState()
    val count = state.messages.size
    // Follows the answer as it grows while the reader is at the bottom; scrolling up to read lets go until they return.
    var follow by remember { mutableStateOf(true) }
    LaunchedEffect(count) {
        if (count == 0) return@LaunchedEffect
        follow = true
        list.animateScrollToItem(count - 1)
        list.scrollBy(TO_END)
    }
    LaunchedEffect(list) { snapshotFlow { list.isScrollInProgress }.collect { moving -> if (!moving) follow = !list.canScrollForward } }
    LaunchedEffect(list) {
        snapshotFlow { list.canScrollForward }.collect { more -> if (more && follow && !list.isScrollInProgress) list.scrollBy(TO_END) }
    }
    ScrollToTopOnRequest { list.animateScrollToItem(0) }
    LazyColumn(
        modifier.fillMaxWidth().nestedScroll(rememberKeyboardAwayOnDrag(platformFeel.dragDismissesKeyboard)),
        state = list,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(state.messages, key = { it.id }, contentType = { it.kind }) { message ->
            val payload = message.payload
            when {
                message.role == ChatRole.User -> YouBubble(message.text)
                message.kind == MessageKind.Step && payload != null -> parseModelJson<StepCard>(payload)?.let { StepLine(it) }
                message.kind == MessageKind.Job && payload != null -> parseModelJson<JobCard>(payload)?.let { JobCardView(it) }
                message.kind == MessageKind.Draft && payload != null -> parseModelJson<DraftCard>(payload)?.let { DraftCardView(message.id, it, cards) }
                message.kind == MessageKind.Blocked && payload != null -> parseModelJson<BlockedCard>(payload)?.let { BlockedView(it.url, cards) }
                message.kind == MessageKind.CvBuilt && payload != null -> parseModelJson<CvBuiltCard>(payload)?.let { CvBuiltView(message.id, it, cards) }
                else -> TextAnswer(state, message, onRetry)
            }
        }
        if (state.live.running && state.live.streamingId == null) item(key = "working") { ThinkingDot() }
    }
}

@Composable
private fun TextAnswer(state: ChatState, message: app.kiddo.core.model.ChatMessage, onRetry: () -> Unit) {
    val live = message.id == state.live.streamingId
    // When a stream ends the saved text can land a moment after the live copy clears; keep the last words meanwhile.
    val last = remember(message.id) { StringBuilder() }
    val text = when {
        live -> state.live.streamed
        message.text.isNotEmpty() -> message.text
        else -> last.toString()
    }
    if (live || message.text.isNotEmpty()) last.setLength(0).also { last.append(text) }
    Answer(
        text = text,
        streaming = live && state.live.running,
        failed = message.failed && message.id == state.messages.last().id,
        onRetry = onRetry,
    )
}

// The menu: a panel from the left over a dim, like ChatGPT's.
@Composable
private fun Drawer(open: Boolean, state: ChatState, actions: MenuActions, onClose: () -> Unit) {
    val reduced = Kiddo.reducedMotion
    AnimatedVisibility(open, enter = fadeIn(KiddoMotion.tween(KiddoMotion.QUICK, KiddoMotion.Ease)), exit = fadeOut(KiddoMotion.tween(KiddoMotion.QUICK, KiddoMotion.Ease))) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = DIM)).pressable(onClose, label = stringResource(Res.string.menu_close)))
    }
    AnimatedVisibility(
        open,
        enter = if (reduced) fadeIn(KiddoMotion.reduced()) else slideInHorizontally(KiddoMotion.tween(KiddoMotion.DRAWER, KiddoMotion.Emphasized)) { -it },
        exit = if (reduced) fadeOut(KiddoMotion.reduced()) else slideOutHorizontally(KiddoMotion.tween(KiddoMotion.QUICK, KiddoMotion.Accelerate)) { -it },
    ) {
        Menu(state, actions, onClose, Modifier.fillMaxWidth(DRAWER_WIDTH))
    }
}

private const val DIM = 0.4f
private const val DRAWER_WIDTH = 0.84f
private const val TO_END = 100_000f
