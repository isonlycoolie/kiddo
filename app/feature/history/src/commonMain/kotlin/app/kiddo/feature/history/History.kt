package app.kiddo.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import app.kiddo.core.database.ChatStore
import app.kiddo.core.database.WorkspaceStore
import app.kiddo.core.designsystem.Kiddo
import app.kiddo.core.designsystem.component.Empty
import app.kiddo.core.designsystem.component.KiddoIcon
import app.kiddo.core.designsystem.component.KiddoScreen
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.NavBar
import app.kiddo.core.designsystem.component.SheetGroup
import app.kiddo.core.designsystem.component.SheetRow
import app.kiddo.core.designsystem.component.pressable
import app.kiddo.core.model.Chat
import app.kiddo.core.model.Clock
import app.kiddo.core.navigation.Route
import app.kiddo.core.platform.localDay
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.action_more
import app.kiddo.core.resources.generated.KiddoGlyph
import app.kiddo.core.resources.history_delete
import app.kiddo.core.resources.history_earlier
import app.kiddo.core.resources.history_empty
import app.kiddo.core.resources.history_empty_body
import app.kiddo.core.resources.history_search
import app.kiddo.core.resources.history_title
import app.kiddo.core.resources.history_today
import app.kiddo.core.resources.history_yesterday
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(workspaces: WorkspaceStore, private val chats: ChatStore, val clock: Clock) : ViewModel() {
    val chatList: StateFlow<List<Chat>?> = workspaces.workspace.filterNotNull()
        .flatMapLatest { chats.chats(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER), null)

    fun delete(id: String) {
        viewModelScope.launch { chats.delete(id) }
    }
}

val historyModule = module { viewModelOf(::HistoryViewModel) }

fun NavGraphBuilder.historyScreens(nav: NavController) {
    composable<Route.History> {
        val viewModel: HistoryViewModel = koinViewModel()
        val list by viewModel.chatList.collectAsStateWithLifecycle()
        val overlay = LocalOverlay.current
        HistoryScreen(
            list,
            today = localDay(viewModel.clock.now()),
            onBack = { nav.popBackStack() },
            // Opening a chat replaces the chat underneath, so Back from it leaves to wherever the user came from.
            onOpen = { id -> nav.navigate(Route.Home(chatId = id)) { popUpTo<Route.Home> { inclusive = true } } },
            onMore = { id -> overlay.sheet { SheetGroup { DeleteRow { viewModel.delete(id).also { overlay.close() } } } } },
        )
    }
}

@Composable
private fun DeleteRow(onDelete: () -> Unit) {
    SheetRow(stringResource(Res.string.history_delete), onDelete, glyph = KiddoGlyph.Trash, danger = true)
}

@Composable
fun HistoryScreen(
    chats: List<Chat>?,
    today: Long,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onMore: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    KiddoScreen(modifier, header = { NavBar(title = stringResource(Res.string.history_title), onBack = onBack) }) {
        if (chats == null) return@KiddoScreen
        if (chats.isEmpty()) {
            Empty(KiddoGlyph.Clock, stringResource(Res.string.history_empty), body = stringResource(Res.string.history_empty_body))
            return@KiddoScreen
        }
        Search(query, { query = it })
        val shown = chats.filter { query.isBlank() || it.title.contains(query.trim(), ignoreCase = true) }
        shown.groupBy { group(localDay(it.updatedAt), today) }.toSortedMap(compareBy { it.ordinal }).forEach { (day, items) ->
            BasicText(
                stringResource(day.label),
                style = Kiddo.type.label.copy(color = Kiddo.colors.muted),
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp),
            )
            items.forEach { chat -> ChatLine(chat, { onOpen(chat.id) }, { onMore(chat.id) }) }
        }
    }
}

@Composable
private fun Search(query: String, onQuery: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 42.dp).background(Kiddo.colors.well, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KiddoIcon(KiddoGlyph.Search, null, size = 18.dp, tint = Kiddo.colors.muted)
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) BasicText(stringResource(Res.string.history_search), style = Kiddo.type.body.copy(color = Kiddo.colors.muted))
            BasicTextField(query, onQuery, singleLine = true, textStyle = Kiddo.type.body.copy(color = Kiddo.colors.fg), cursorBrush = SolidColor(Kiddo.colors.fg), modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ChatLine(chat: Chat, onOpen: () -> Unit, onMore: () -> Unit) {
    Row(Modifier.fillMaxWidth().pressable(onOpen, pressedAlpha = 0.6f).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            chat.title,
            style = Kiddo.type.body.copy(color = Kiddo.colors.fg),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
        )
        Box(Modifier.pressable(onMore, pressedAlpha = 0.5f, label = stringResource(Res.string.action_more)).padding(12.dp)) {
            KiddoIcon(KiddoGlyph.More, null, size = 20.dp, tint = Kiddo.colors.muted)
        }
    }
}

private enum class Day(val label: StringResource) {
    Today(Res.string.history_today),
    Yesterday(Res.string.history_yesterday),
    Earlier(Res.string.history_earlier),
}

private fun group(day: Long, today: Long) = when (day) {
    today -> Day.Today
    today - 1 -> Day.Yesterday
    else -> Day.Earlier
}

private const val STOP_AFTER = 5_000L
