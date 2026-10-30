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

