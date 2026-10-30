package app.kiddo.feature.chat

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.kiddo.core.database.ChatStore
import app.kiddo.core.designsystem.component.BackPress
import app.kiddo.core.designsystem.component.LocalOverlay
import app.kiddo.core.designsystem.component.Overlay
import app.kiddo.core.designsystem.component.accountSheet
import app.kiddo.core.designsystem.component.message
import app.kiddo.core.model.MailAccount
import app.kiddo.core.navigation.Route
import app.kiddo.core.platform.rememberNotificationAsk
import app.kiddo.core.resources.Res
import app.kiddo.core.resources.agent_no_mail
import app.kiddo.core.resources.blocked_paste_text
import app.kiddo.core.resources.chat_models
import app.kiddo.core.resources.cvcard_saved
import app.kiddo.core.resources.send_title
import app.kiddo.core.resources.starter_cv
import app.kiddo.core.web.PageReader
import app.kiddo.feature.chat.agent.CvBuilder
import app.kiddo.feature.chat.agent.DraftCard
import app.kiddo.feature.chat.agent.JobAgent
import app.kiddo.feature.chat.agent.Outbox
import app.kiddo.feature.chat.agent.parseModelJson
import app.kiddo.feature.chat.ui.BrowserScreen
import app.kiddo.feature.chat.ui.CardActions
import app.kiddo.feature.chat.ui.DraftScreen
import app.kiddo.feature.chat.ui.HomeActions
import app.kiddo.feature.chat.ui.HomeScreen
import app.kiddo.feature.chat.ui.MenuActions
import app.kiddo.feature.chat.ui.SendSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatModule = module {
    factoryOf(::PromptBuilder)
    factoryOf(::JobAgent)
    factoryOf(::CvBuilder)
    factoryOf(::Outbox)
    single { PageReader(get()) }
    viewModelOf(::ChatViewModel)
}

private const val BROWSER_URL = "browser_url"
private const val BROWSER_TEXT = "browser_text"

fun NavGraphBuilder.chatScreens(nav: NavController) {
    composable<Route.Home> { entry -> HomeRoute(nav, entry) }
    composable<Route.Draft> { entry ->
        val id = entry.toRoute<Route.Draft>().messageId
        val chats: ChatStore = koinInject()
        val outbox: Outbox = koinInject()
        val scope = rememberCoroutineScope()
        val draft by produceState<DraftCard?>(null, id) { value = chats.message(id)?.payload?.let { parseModelJson<DraftCard>(it) } }
        draft?.let { d ->
            DraftScreen(d, { nav.popBackStack() }, { to, subject, body ->
                scope.launch {
                    outbox.saveDraft(id, to, subject, body)
                    nav.popBackStack()
                }
            })
        }
    }
    composable<Route.Browser> { entry ->
        val url = entry.toRoute<Route.Browser>().url
        BrowserScreen(url, { nav.popBackStack() }, { text ->
            nav.previousBackStackEntry?.savedStateHandle?.apply {
                set(BROWSER_URL, url)
                set(BROWSER_TEXT, text)
            }
            nav.popBackStack()
        })
    }
}

